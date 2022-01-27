"""Kafka Source Connector API
A API written in Python, using Flask module to create a simple API for some operations. This API is responsible for
registering and un-registering the Kafka Connector with the Registry Service. Also, it sends status updates of this
connector in Registry Service every a specified time interval. Finally, it has a route for collecting netflow files.
When a nfcapd file is received, it is converted in csv and stored in /home/kafka-source-connector/collected_files_csv/
directory, for the FileWatcher to notify the Source Task about it.

On startup, the API tries to register in Registry Service. If an error occured, it tries again until the registration
is completed. When the registration is finished, a separate thread for updating Kafka Connector status is starting.
Finally, the Flask server is starting and waiting for connections

Execution Params:
- Registry Service Params:
param --registry_service_ip -rip: IP of services registry.
param --registry_service_port -rp: Port of services registry.
- Flask Server Params:
param --port -p: Port that API for discovery & registry service will listen to, defaults to 7000 (optional).
param --name -n: Name of connector. This name must be unique among all connectors.
param --update_seconds -us: Time interval (in seconds) to update status of this connector to Registry Service, defaults to 60 (optional).
"""

import sys
import json
import time
import logging
import argparse
import requests
import threading

from os import system
from flask import Flask, request

# Initialize logger
logger = logging.getLogger()
logger.setLevel(logging.INFO)
handler = logging.StreamHandler(sys.stdout)
handler.setLevel(logging.INFO)
formatter = logging.Formatter('%(asctime)s %(levelname)s %(message)s')
handler.setFormatter(formatter)
logger.addHandler(handler)

# Initialize Flask app
app = Flask("Palantir CSV Source Connector")

def parseArguments():
    parser = argparse.ArgumentParser(description='Netflow Kafka Source Connector Params.')

    # Arguments about discovery service
    registryServiceArgsGroup = parser.add_argument_group('Registry Service Params')
    registryServiceArgsGroup.add_argument('-rip', '--registry_service_ip', type=str, required=True, 
        help='IP of registry service')
    registryServiceArgsGroup.add_argument('-rp', '--registry_service_port', type=int, required=True, 
        help='Port, where registry service listens to')

    # Arguments for Flask server
    apiArgsGroup = parser.add_argument_group('Kafka Source Connector Params')
    apiArgsGroup.add_argument('-p', '--port', type=int, required=False, default=7000, 
        help='Port that API for discovery & registry service will listen to. Default: 7000')
    apiArgsGroup.add_argument('-n', '--name', type=str, required=True, 
        help='Name of connector. This name must be unique among all connectors.')
    apiArgsGroup.add_argument('-us', '--update_seconds', type=int, required=False, default=60, 
        help='Time interval (in seconds) to update status of this connector to Registry Service. Default: 60')

    args = parser.parse_args()

    return args

# Status upodate function. It sends a message in Registry Service, with its name and its URL.
# Runs in separate thread, every X seconds (i.e. 1 minute).
def periodicUpdate(timeIntervalSeconds=60):
    """Status Update function
    Status update function. It sends a message in Registry Service periodically, with its name and its URL.
    It runs in a separate thread, every a specified time interval (Default: 60 seconds).
    """

    threading.Timer(timeIntervalSeconds, periodicUpdate, [timeIntervalSeconds], {}).start()

    serviceName = str(args.name)
    serviceUrl = 'http://' + str(args.name) + ':' + str(args.port)

    registryServiceUrl = 'http://' + str(args.registry_service_ip) + ':' + str(args.registry_service_port)

    headers = {}
    headers['Content-Type'] = 'application/json'

    serviceInfo = {}
    serviceInfo['name'] = serviceName
    serviceInfo['url'] = serviceUrl
    
    # Send an update command to Registry service
    requests.post(url=registryServiceUrl + '/update', headers=headers, json=serviceInfo)

    logger.info("------------------ Status Update ------------------")
    logger.info("Service Name: " + serviceName)
    logger.info("Service URL: " + serviceUrl)
    logger.info("--------------------------------------------------")

# Retrieve a nfcapd file and convert it
@app.route("/convert", methods=["POST"])
def convertFile():
    """(POST) Retrieve netflow file route.
    A route used for retrieving sent netflow files, in .nfcapd format. When a file is received it is converted in csv
    format using nfdump tool and saved under /home/kafka-source-connector/collected_files_csv/ directory.

    Parameters
    ----------
    param filename: Filename of .nfcapd file to be collected

    Request Body
    ------------
    File Content (in binary mode)

    Responses (response, status code)
    ---------------------------------
    {"result": "success"}, 200
    {"result": "error", "message": "A message describing the error occured"}, 400
    """

    response = {}
    statusCode = 200
    
    try:
        nfcapd_filename = '/home/kafka-source-connector/collected_files/' + str(request.args['filename'])
        csv_filename = '/home/kafka-source-connector/collected_files_csv/' + str(request.args['filename'])

        f = open(nfcapd_filename, 'wb')
        f.write(request.data)
        f.close()

        system('nfdump -o csv -r ' + nfcapd_filename + ' -q > ' + str(csv_filename) + '.csv')

        statusCode = 200
        response['result'] = 'success'
    except Exception as e:
        statusCode = 400
        response['result'] = 'error'
        response['message'] = e

    return response, statusCode

# Route to check that connector is up and running
@app.route("/ping", methods=["GET"])
def ping():
    """(GET) Ping route.
    A route used for pinging the existing connector. It can be used as health check. If the API responds, the connector
    is up and running. If this route doesn't respond, something is wrong with the connector. The management of not
    retrieving a response must happen in client side.

    Responses (response, status code)
    ---------------------------------
    {"result": "success", "ping": "pong"}, 200
    """

    response = {}
    response['result'] = 'success'
    response['ping'] = 'pong'
    statusCode = 200
    
    return response, statusCode

if __name__ == "__main__":
    # Parse command line arguments
    args = parseArguments()

    # Register service to Registry Service. If register request repeat (every 10 seconds), until registration is successfull
    registryServiceUrl = 'http://' + str(args.registry_service_ip) + ':' + str(args.registry_service_port)

    headers = {}
    headers['Content-Type'] = 'application/json'

    serviceInfo = {}
    serviceInfo['name'] = str(args.name)
    serviceInfo['url'] = 'http://' + str(args.name) + ':' + str(args.port)

    logger.info(registryServiceUrl)
    logger.info(serviceInfo)

    while True:
        # Send a register command to Registry service
        response = requests.post(url=registryServiceUrl + '/register', headers=headers, json=serviceInfo)
        content = response.json()

        if response.status_code == 200:
            try:
                if content['result']:
                    if content['result'] == 'success':
                        break
                    else:
                        logger.error('Cannot register connector (result = error).')
                        logger.error(content['message'])
                        time.sleep(10)
            except Exception as e:
                logger.error('Cannot register connector (result not in json keys).')
                logger.error(e)
                time.sleep(10)
        else:
            logger.error('Cannot establish connection with Registry Service. Status code : ' + str(response.status_code) + '.')
            try:
                if content['message']:
                    logger.error(content['message'])
            except Exception as e:
                logger.error('Unknown cause')
            time.sleep(10)

    # Start thread for periodic updates
    periodicUpdate(timeIntervalSeconds=args.update_seconds)

    app.run(
            host='0.0.0.0',
            port=args.port,
            debug=True
        )
import sys
import json
import logging
import argparse
import threading
import requests

from requests import exceptions
from flask import Flask, request

# Initialize logger
logger = logging.getLogger()
logger.setLevel(logging.INFO)
handler = logging.StreamHandler(sys.stdout)
handler.setLevel(logging.INFO)
formatter = logging.Formatter('%(asctime)s %(levelname)s %(message)s')
handler.setFormatter(formatter)
logger.addHandler(handler)

# Dictionary, where all deployed services will be stored
services = {}

# Routes for running services
SERVICE_PING_ROUTE = '/ping'
SERVICE_CONVERT_FILE_ROUTE = '/convert'

# Counter for load balancing the services round robin
nextServiceCounter = 0

# Initialize Flask app
app = Flask("Palantir Registry Service")

def parseArguments():
    parser = argparse.ArgumentParser(description='Registry Service Params.')

    # Arguments for HealthCheckService
    healthCheckArgsGroup = parser.add_argument_group('Health Check Service Params')
    healthCheckArgsGroup.add_argument('-hs', '--health_check_seconds', type=int, required=False, default=60, 
        help='Time interval (in seconds) to check status of all deployed services. Default: 60')

    # Arguments for Flask server
    apiArgsGroup = parser.add_argument_group('Discovery & Registry Service Params')
    apiArgsGroup.add_argument('-p', '--port', type=int, required=False, default=5000, 
        help='Port that API for discovery & registry service will listen to. Default: 5000')

    args = parser.parse_args()

    return args

# If a service is down or sends an exit request, remove it from list with available services
# Also prints a cause for removing service. If cause not provided, print message "Unknown cause"
# List of cause codes:
# 0 -> Unknown cause (Default): Service is removed from dictionary without providing information.
# 1 -> Unregister message: Service unregistered itself by sending an unregister message.
# 2 -> Service is down: Service is removed from dictionary, because it didn't respond to ping request. Probably service is down.
def removeService(srv, cause=0):
    if srv in services.keys():
        if cause == 1:
            logger.warning('[' + str(srv) + '] Unregister message received. Removing endpoint ' + str(services[srv]))
        elif cause == 2:
            logger.warning('[' + str(srv) + '] Service is down. Removing endpoint ' + str(services[srv]))
        else:
            logger.warning('[' + str(srv) + '] Unknown cause. Removing endpoint ' + str(services[srv]))
        services.pop(srv, None)
    else:
        logger.warning('Service ' + str(srv) + 'cannot be removed because it is not registered!')

# Update existing service, if already exists and it does not have the same name and url. Otherwise register it.
def updateService(srv, url):
    try:
        if srv in services.keys():
            if services[srv] != url:
                logger.info('Updating service ' +str(srv) + '. Setting url=' + str(url))
                services[srv] = url
        else:
            logger.info('Cannot update service ' +str(srv) + ' because it does not exists. Registering it with url=' + str(url))
            services[srv] = url
    except Exception as e:
        logger.error('Cannot update service ' + str(srv) + '(' + str(url) + '). An error occured.')
        logger.error(str(e))

# Health check function. It pings all registered services to identify
# if they are running or not. Runs in separate thread, every X seconds (i.e. 1 minute).
def healthCheck(timeIntervalSeconds=60):
    threading.Timer(timeIntervalSeconds, healthCheck, [timeIntervalSeconds], {}).start()

    # Ping all services and update services dict, if a service is down
    for srv in list(services):
        try:
            logging.info("Pinging service " + str(srv) + "("+services[srv]+SERVICE_PING_ROUTE+")")
            r = requests.get(services[srv]+SERVICE_PING_ROUTE)
            r.raise_for_status()  # Raises a HTTPError if the status is 4xx, 5xxx
        except (requests.exceptions.ConnectionError, requests.exceptions.Timeout):
            removeService(srv)
        except requests.exceptions.HTTPError:
            removeService(srv)
        else:
            content = r.json()
            if r.status_code == 200:
                if content['result'] or content['ping']:
                    if (content['result'] != 'success') or (content['ping'] != 'pong'):
                        removeService(srv)
                else:
                    removeService(srv)

    logger.info("------------------ Health Check ------------------")
    logger.info("Number of running services: " + str(len(services)))
    logger.info(services)
    logger.info("--------------------------------------------------")

# Parse command line arguments
args = parseArguments()
logger.info("Registry Service is starting with the following params")
for arg in vars(args):
     logger.info("\t" + str(arg) + ": " + str(getattr(args, arg)))
logger.info("--------------------------------------------------")

# API route for registering new services
@app.route("/register", methods=["POST"])
def registerServiceRoute():
    response = {}
    status_code = 200

    try :
        content = request.json
        logger.info('Trying to register service ' + str(content))

        services[content['name']] = content['url']

        response['result'] = 'success'
        status_code = 200
    except Exception as e:
        print(e)

        logging.error('An exception occured in /register route.')
        logging.error(str(e))

        response['result'] = 'error'
        response['message'] = str(e)
        status_code = 400
    
    return response, status_code

# API route for unregistering services
@app.route("/unregister", methods=["POST"])
def unregisterServiceRoute():
    response = {}
    status_code = 200

    try :
        content = request.json
        logger.info('Trying to unregister service ' + str(content))

        removeService(content['name'], 1)

        response['result'] = 'success'
        status_code = 200
    except Exception as e:
        logging.error('An exception occured in /unregister route.')
        logging.error(str(e))

        response['result'] = 'error'
        response['message'] = str(e)
        status_code = 400
    
    return response, status_code

# API route for registering new services
@app.route("/update", methods=["POST"])
def updateServiceRoute():
    response = {}
    status_code = 200

    try :
        content = request.json
        logger.info('Trying to update service ' + str(content))

        updateService(content['name'], content['url'])

        response['result'] = 'success'
        status_code = 200
    except Exception as e:
        logging.error('An exception occured in /update route.')
        logging.error(str(e))

        response['result'] = 'error'
        response['message'] = str(e)
        status_code = 400
    
    return response, status_code

@app.route("/target", methods=["GET"])
def getNextServiceRoute():
    global nextServiceCounter

    response = {}
    status_code = 200

    if nextServiceCounter < 0:
        nextServiceCounter = 0

    if len(services) <= 0:
        response['result'] = 'error'
        response['message'] = 'Not available service'

        status_code = 400
    else:
        if nextServiceCounter >= len(services):
            nextServiceCounter = 0

        nextServiceName = list(services)[nextServiceCounter]
        nextServiceUrl = services[nextServiceName]
        
        nextServiceCounter = nextServiceCounter + 1

        response['result'] = 'success'
        response['name'] = nextServiceName
        response['url'] = nextServiceUrl+SERVICE_CONVERT_FILE_ROUTE

        status_code = 200
            
    return response, status_code

@app.route("/services", methods=["GET"])
def getServicesRoute():
    return services, 200

# Start health check thread
healthCheck(timeIntervalSeconds=args.health_check_seconds)

# Run Flask API
app.run(
    host='0.0.0.0',
    port=args.port,
    debug=True
)
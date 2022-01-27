import sys
import time
import logging
import argparse
import requests

# Initialize logger
logger = logging.getLogger()
logger.setLevel(logging.INFO)
handler = logging.StreamHandler(sys.stdout)
handler.setLevel(logging.INFO)
formatter = logging.Formatter('%(asctime)s %(levelname)s %(message)s')
handler.setFormatter(formatter)
logger.addHandler(handler)

def parseArguments():
    parser = argparse.ArgumentParser(description='Netflow File Distributor Service Params.')

    # Arguments for Netflow File Distributor Service
    distributorArgsGroup = parser.add_argument_group('Netflow File Distributor Service Params')
    distributorArgsGroup.add_argument('-rip', '--registry_service_ip', type=str, required=True, 
        help='IP of registry service')
    distributorArgsGroup.add_argument('-rp', '--registry_service_port', type=int, required=True, 
        help='Port, where registry service listens to')
    distributorArgsGroup.add_argument('-f', '--file', type=str, required=True,
        help='Path of nfcapd file to send')
    distributorArgsGroup.add_argument('-n', '--filename', type=str, required=True,
        help='Name of nfcapd file to send, without path')

    args = parser.parse_args()

    return args

# Parse command line arguments
args = parseArguments()
logger.info("Registry Service is starting with the following params")
for arg in vars(args):
     logger.info("\t" + str(arg) + ": " + str(getattr(args, arg)))
logger.info("--------------------------------------------------")

# Acquire target from registry service (if any is available). 
# If no target is available sleep for 10 seconds and try again.
targetSearch = True
while targetSearch:
    logger.info('Searching for next available connector.')
    r = requests.get(url='http://' + str(args.registry_service_ip) + ':' + str(args.registry_service_port) + '/target')

    if r.status_code == 200:
        content = r.json()

        if content['result']:
            if content['result'] == 'success':
                logger.info('Available connector found: ' + content['name'] + '('+ content['url'] +')')
                target = content['url']

                # Set request headers
                headers = {}
                headers['Content-Type'] = 'application/json'

                # Set request params
                params = {}
                params['filename'] = args.filename

                # Load nfcapd file (binary)
                nfcapd_file = open(args.file, 'rb').read()

                # Send the POST request to available connector.
                response = requests.post(url=target, headers=headers, params=params, data=nfcapd_file)

                if response.status_code == 200:
                    r_content = response.json()
                    if r_content['result']:
                        if r_content['result'] == 'success':
                            targetSearch = False
                            break
                        else:
                            logger.error('Available connector cannot receive the sent file (result = error).')
                            logger.error(r_content['message'])
                            time.sleep(10)
                    else:
                        logger.error('Available connector cannot receive the sent file (result not in json keys).')
                        time.sleep(10)
                else:
                    logger.error('Available connector cannot receive the sent file. Status code : ' + str(response.status_code) + '.')
                    time.sleep(10)
            else:
                logger.error('Not available connector found.  Searching again in 10 seconds.')
                time.sleep(10)
        else:
            logger.error('Not available connector found. Searching again in 10 seconds.')
            time.sleep(10)
    else:
        logger.error('Not available connector found. Searching again in 10 seconds.')
        time.sleep(10)
import os
import json
import argparse
from kafka import KafkaConsumer

"""Zeeklogs Consumer
A Kafka consumer, that will read forwarded Zeek logs (using Filebeat) to kafka and appends them to zeeklog file.

Execution Params:
- Kafka Params:
param --kafka-bootstrap-server -k: Kafka bootstrap servers, comma separated, defaults to localhost:9092 (optional).
param --kafka-topic -t: Kafka topic to read logs from, defaults to zeeklogs (optional).

param --directory -d: Directory where zeeklogs will be stored to, defaults to /home/zeekflow-integration/logs/ (optional).
param --logfile -l: Name of zeeklog file, defaults to conn.log (optional).

param --benchmark -b: Enabled benchmark mode, defaults to False(optional).
"""

def parseArguments():
    parser = argparse.ArgumentParser(
        description='Zeekflow Integration API Params.')

    # Zeek logs configuration
    zeekArgsGroup = parser.add_argument_group('Zeek Logs Params')
    zeekArgsGroup.add_argument('-d', '--directory', type=str, required=False, default='/home/zeekflow-integration/logs/',
        help='Directory, where zeek logs will be stored to. Default: /home/zeekflow-integration/logs/')
    zeekArgsGroup.add_argument('-l', '--logfile', type=str, required=False, default='conn.log',
        help='File, where zeek logs are synced. Default: conn.log')

    # Kafka consumer configuration
    kafkaArgsGroup = parser.add_argument_group('Kafka Consumer Params')
    kafkaArgsGroup.add_argument('-k', '--kafka-bootstrap-server', type=str, required=False, default='localhost:9092',
        help='Kafka bootstrap servers, comma separated. Default: localhost:9092')
    kafkaArgsGroup.add_argument('-t', '--kafka-topic', type=str, required=False, default='zeeklogs',
        help='Kafka topic, where zeek logs will be sent to. Default: zeeklogs')
    
    # Other configuration
    otherArgsGroup = parser.add_argument_group('Other Params')
    otherArgsGroup.add_argument('-b', '--benchmark', type=bool, required=False, default=False,
        help='Run Zeekflow API in benchmark mode. Default: False')

    args = parser.parse_args()

    return args

if __name__ == "__main__":
    # Parse command line arguments
    args = parseArguments()

    print('Arguments: ' + str(args))

    # Check zeeklogs directory. Create if not already exists
    if os.path.exists(args.directory):
        print('Zeeklogs path ' + str(args.directory) + ' already exists!')
    else :
        print('Zeeklogs path ' + str(args.directory) + ' does not exists! Creating now...')
        os.mkdir(args.directory)
    
    # Full path of zeek log file
    zeeklog = os.path.join(args.directory, args.logfile)
    print('Zeeklog file: ' + str(zeeklog))

    # Kakfa consumer
    kafkaBootstrapServer = args.kafka_bootstrap_server
    kafkaTopic = args.kafka_topic

    consumer = KafkaConsumer(
        kafkaTopic,
        bootstrap_servers=[kafkaBootstrapServer],
        group_id='palantir-zeeklogs-group',
        auto_offset_reset='latest',
        enable_auto_commit=True
    )

    for message in consumer:
        try: 
            messageJson = json.loads(message.value)

            # For each message open zeeklogs file and append the line.
            # If file does not exists in first iteration, create it.
            with open(zeeklog, "a") as zeeklog_file:
                # Append zeeklog record
                zeeklog_file.write(messageJson['message']+'\n')
        except Exception as e:
            print("An exception occured trying to read message and append it to zeeklog file:")
            print(str(e))

    # Close Kafka consumer
    consumer.close()
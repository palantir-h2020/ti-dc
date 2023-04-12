import sys
import logging
import argparse

from kafka import KafkaConsumer
from kafka import KafkaProducer

# Initialize logger
logger = logging.getLogger()
logger.setLevel(logging.INFO)
handler = logging.StreamHandler(sys.stdout)
handler.setLevel(logging.INFO)
formatter = logging.Formatter('%(asctime)s %(levelname)s %(message)s')
handler.setFormatter(formatter)
logger.addHandler(handler)

# Constants

# Parse command line arguments function
def parseArguments():
    parser = argparse.ArgumentParser(description='SDA-DCP Converter Params.')

    # Kafka Arguments
    kafkaArgsGroup = parser.add_argument_group('Kafka Params')
    kafkaArgsGroup.add_argument('-b', '--bootstrap_server', type=str, required=False, default='localhost:9092', 
        help='Kafka Bootstrap Servers. Default: localhost:9092')
    kafkaArgsGroup.add_argument('-sda', '--sda_topic', type=str, required=False, default='netflow-raw-sda', 
        help='DCP output topic. Default: netflow-raw-sda')
    kafkaArgsGroup.add_argument('-dcp', '--dcp_topic', type=str, required=False, default='netflow-raw-dcp', 
        help='DCP output topic. Default: netflow-raw-dcp')

    # Other Arguments
    otherArgsGroup = parser.add_argument_group('Other Params')
    otherArgsGroup.add_argument('-tid', '--tenant_id', type=int, required=False, default=0, 
        help='Tenant ID. Default: 0')

    args = parser.parse_args()

    return args

def main():
    args = parseArguments()

    print(args)

    consumer = KafkaConsumer(
        args.sda_topic,
        bootstrap_servers=[args.bootstrap_server],
        auto_offset_reset='latest',
        enable_auto_commit=True,
    )

    producer = KafkaProducer(
        bootstrap_servers=args.bootstrap_server,
        key_serializer=lambda x: x.encode('utf-8'),
        value_serializer=lambda x: x.encode('utf-8')
    )

    counter = 0

    for message in consumer:
        producer.send(topic=args.dcp_topic, key=str(args.tenant_id)+'_sda-message-'+str(counter), value=((message.value).decode('utf-8')).rstrip())
        producer.flush()

        counter = counter + 1

        if (counter % 1000) == 0:
            print("Messages sent: " + str(counter))

main()
#!/bin/bash

KAFKA_HOME=/opt/kafka

# Configure Kafka Connect
sed -i "s/bootstrap.servers=localhost:9092/bootstrap.servers=${KAFKA_IP}:${KAFKA_PORT}/g" "$KAFKA_HOME/connectors/palantir-connect-standalone.properties"
# Option for benchmarking
sed -i "s/benchmark_mode=false/benchmark_mode=${BENCHMARK_MODE}/g" "$KAFKA_HOME/connectors/netflow-source-linux.properties"

# Start Csv Kafka Source Connector
$KAFKA_HOME/bin/connect-standalone.sh $KAFKA_HOME/connectors/palantir-connect-standalone.properties $KAFKA_HOME/connectors/netflow-source-linux.properties > "netflow_csv_source_connector.log" 2>&1 &

# Start Python server
python3 src/main/python/nfcapd_api.py -rip ${REGISTRY_IP} -rp ${REGISTRY_PORT} -n ${NAME} -p ${API_PORT} -us ${UPDATE_INTERVAL}


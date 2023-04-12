#!/bin/bash

KAFKA_HOME=/opt/kafka

# Configure Kafka Connect
sed -i "s/bootstrap.servers=localhost:9092/bootstrap.servers=${KAFKA_IP}:${KAFKA_PORT}/g" "$KAFKA_HOME/connectors/palantir-connect-standalone.properties"
# Configure Configuration
sed -i "s/kafka.topic.source=netflow-raw/kafka.topic.source=${KAFKA_TOPIC}/g" "$KAFKA_HOME/connectors/netflow-source-linux.properties"
sed -i "s/collector.id=0/collector.id=${COLLECTOR_ID}/g" "$KAFKA_HOME/connectors/netflow-source-linux.properties"
sed -i "s/tenant.id=0/tenant.id=${TENANT_ID}/g" "$KAFKA_HOME/connectors/netflow-source-linux.properties"
# Options for filewatcher
sed -i "s+filewatcher.dir.observe=/home/kafka-source-connector/collected_files_csv/+filewatcher.dir.observe=${FILEWATCHER_DIRECTORY}+g" "$KAFKA_HOME/connectors/netflow-source-linux.properties"
sed -i "s/filewatcher.interval.s=5/filewatcher.interval.s=${FILEWATCHER_INTERVAL_SECONDS}/g" "$KAFKA_HOME/connectors/netflow-source-linux.properties"
# Option for benchmarking
sed -i "s/benchmark_mode=false/benchmark_mode=${BENCHMARK_MODE}/g" "$KAFKA_HOME/connectors/netflow-source-linux.properties"
# Options for Zeek integration
sed -i "s/zeek_integration.enabled=false/zeek_integration.enabled=${ZEEK_INTEGRATION}/g" "$KAFKA_HOME/connectors/netflow-source-linux.properties"
sed -i "s/zeek_integration.ip=localhost/zeek_integration.ip=${ZEEK_IP}/g" "$KAFKA_HOME/connectors/netflow-source-linux.properties"
sed -i "s/zeek_integration.port=80/zeek_integration.port=${ZEEK_PORT}/g" "$KAFKA_HOME/connectors/netflow-source-linux.properties"

# Start Csv Kafka Source Connector
$KAFKA_HOME/bin/connect-standalone.sh $KAFKA_HOME/connectors/palantir-connect-standalone.properties $KAFKA_HOME/connectors/netflow-source-linux.properties > "netflow_csv_source_connector.log" 2>&1 &

# Start Python server
python3 src/main/python/nfcapd_api.py -rip ${REGISTRY_IP} -rp ${REGISTRY_PORT} -n ${NAME} -p ${API_PORT} -us ${UPDATE_INTERVAL} -z ${ZEEK_INTEGRATION} -zip ${ZEEK_IP} -zp ${ZEEK_PORT}


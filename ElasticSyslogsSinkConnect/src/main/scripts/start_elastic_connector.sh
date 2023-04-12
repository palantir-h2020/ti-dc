#!/bin/bash

KAFKA_HOME=/opt/kafka

cd "$KAFKA_HOME/connectors/"

# Configure Kafka Connect
sed -i "s/bootstrap.servers=localhost:9092/bootstrap.servers=${KAFKA_IP}:${KAFKA_PORT}/g" "$KAFKA_HOME/connectors/palantir-connect-standalone.properties"

sed -i "s/topics=syslog-raw-connector,syslog-preprocessed-connector/topics=${RAW_SINK_TOPIC},${PREPROCESSED_SINK_TOPIC}/g" "$KAFKA_HOME/connectors/elastic-sink.properties"
sed -i "s/kafka.topic.sink.syslog.raw=syslog-raw-connector/kafka.topic.sink.syslog.raw=${RAW_SINK_TOPIC}/g" "$KAFKA_HOME/connectors/elastic-sink.properties"
sed -i "s/kafka.topic.sink.syslog.preprocessed=syslog-preprocessed-connector/kafka.topic.sink.syslog.preprocessed=${PREPROCESSED_SINK_TOPIC}/g" "$KAFKA_HOME/connectors/elastic-sink.properties"
sed -i "s/elastic.host.ip=localhost/elastic.host.ip=${ELASTIC_IP}/g" "$KAFKA_HOME/connectors/elastic-sink.properties"
sed -i "s/elastic.host.port=9200/elastic.host.port=${ELASTIC_PORT}/g" "$KAFKA_HOME/connectors/elastic-sink.properties"
sed -i "s/elastic.authentication.username=admin/elastic.authentication.username=${ELASTIC_USER}/g" "$KAFKA_HOME/connectors/elastic-sink.properties"
sed -i "s/elastic.authentication.password=admin/elastic.authentication.password=${ELASTIC_PASS}/g" "$KAFKA_HOME/connectors/elastic-sink.properties"
sed -i "s/elastic.indexes.shards=1/elastic.indexes.shards=${ELASTIC_INDEX_SHARDS}/g" "$KAFKA_HOME/connectors/elastic-sink.properties"
sed -i "s/elastic.indexes.partitions=1/elastic.indexes.partitions=${ELASTIC_INDEX_PARTITIONS}/g" "$KAFKA_HOME/connectors/elastic-sink.properties"
sed -i "s/elastic.indexes.syslog=syslog-raw-index,syslog-preprocessed-index/elastic.indexes.syslog=${RAW_ELASTIC_INDEX},${PREPROCESSED_ELASTIC_INDEX}/g" "$KAFKA_HOME/connectors/elastic-sink.properties"
sed -i "s/elastic.index.syslog.raw=syslog-raw-index/elastic.index.syslog.raw=${RAW_ELASTIC_INDEX}/g" "$KAFKA_HOME/connectors/elastic-sink.properties"
sed -i "s/elastic.index.syslog.preprocessed=syslog-preprocessed-index/elastic.index.syslog.preprocessed=${PREPROCESSED_ELASTIC_INDEX}/g" "$KAFKA_HOME/connectors/elastic-sink.properties"
sed -i "s/connector.sink.syslog.raw=false/connector.sink.syslog.raw=${INSERT_RAW}/g" "$KAFKA_HOME/connectors/elastic-sink.properties"
# Option for benchmarking
sed -i "s/benchmark_mode=false/benchmark_mode=${BENCHMARK_MODE}/g" "$KAFKA_HOME/connectors/elastic-sink.properties"

# Start Csv Kafka Source Connector
$KAFKA_HOME/bin/connect-standalone.sh $KAFKA_HOME/connectors/palantir-connect-standalone.properties $KAFKA_HOME/connectors/elastic-sink.properties
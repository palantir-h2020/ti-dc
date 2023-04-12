FROM debian:bullseye-slim

# Install required packages
RUN apt-get update -y
RUN apt-get install -y curl
RUN apt-get install -y wget
RUN apt-get install -y software-properties-common 
RUN apt-get install -y gpg
RUN apt-get install -y openjdk-17-jdk-headless
RUN apt-get install -y maven

WORKDIR /tmp/

RUN wget https://archive.apache.org/dist/kafka/2.8.0/kafka_2.13-2.8.0.tgz \
    && tar -xzvf kafka_2.13-2.8.0.tgz \ 
    && rm kafka_2.13-2.8.0.tgz \ 
    && mv kafka_2.13-2.8.0 /opt/kafka

RUN mkdir -p /tmp/kafka/

# Create a folder for storing source code, and any other needed components
WORKDIR /home/kafka-sink-connector/

# Copy source code & pom.xml
COPY src ./src
COPY pom.xml ./

# Make script executable
RUN chmod 777 src/main/scripts/start_elastic_connector.sh
RUN chmod +x src/main/scripts/start_elastic_connector.sh

# Build java Kafka Connector
RUN mvn clean compile package

# Create dir for Kafka connectors
WORKDIR /opt/kafka/connectors/

# Copy built jar & config files in kafka connectors dir
RUN cp /home/kafka-sink-connector/jar/PalantirCsvElasticSinkConnect-0.1-SNAPSHOT-jar-with-dependencies.jar ./
RUN cp /home/kafka-sink-connector/src/main/resources/palantir-connect-standalone.properties ./
RUN cp /home/kafka-sink-connector/src/main/resources/elastic-sink.properties ./
RUN cp /home/kafka-sink-connector/src/main/resources/log4j.properties ./
RUN cp /home/kafka-sink-connector/src/main/resources/subnets.txt ./

# Change workdir to home
WORKDIR /home/kafka-sink-connector/

# ENV variables
# Kafka Bootstrap server
ENV KAFKA_IP=10.101.41.255
# Kafka port
ENV KAFKA_PORT=9092
# Kafka topic to fetch raw netflow data from and send them to Elastic
ENV RAW_SINK_TOPIC=netflow-raw
# Kafka topic to fetch preprocessed netflow data from and send them to Elastic
ENV PREPROCESSED_SINK_TOPIC=netflow-anonymized-preprocessed
# Ip of Elasticsearch
ENV ELASTIC_IP=odfe-service.ti-dcp
# Port of Elasticsearch
ENV ELASTIC_PORT=9200
# Elastic account username
ENV ELASTIC_USER=admin
# Elastic account password
ENV ELASTIC_PASS=admin
# Elastic index shards
ENV ELASTIC_INDEX_SHARDS=1
# Elastic index partitions
ENV ELASTIC_INDEX_PARTITIONS=1
# Elastic index for saving raw netflow data
ENV RAW_ELASTIC_INDEX=netflow-raw-index
# Elastic index for saving preprocessed netflow data
ENV PREPROCESSED_ELASTIC_INDEX=netflow-preprocessed-index
# Also, insert raw netflows. Default: false
ENV INSERT_RAW=false
# Zeek integration option
ENV ZEEK_ENABLED=false
# Option for benchmarking
ENV BENCHMARK_MODE=true

# Run script to register collector, start Python server and Kafka Connector
CMD ["/bin/bash", "src/main/scripts/start_elastic_connector.sh"]
FROM debian:buster-slim

# Install required packages
RUN apt-get update -y
RUN apt-get install -y curl
RUN apt-get install -y wget
RUN apt-get install -y python3
RUN apt-get install -y python3-pip
RUN apt-get install -y software-properties-common 
RUN apt-get install -y gpg
RUN apt-add-repository 'deb http://security.debian.org/debian-security stretch/updates main'
RUN apt-get update -y
RUN apt-get install -y openjdk-8-jdk-headless
RUN apt-get install -y maven
RUN apt-get install -y nfdump

# Install Python requirements
COPY src/main/python/requirements.txt /tmp/
RUN pip3 install -r /tmp/requirements.txt

WORKDIR /tmp/

RUN wget https://archive.apache.org/dist/kafka/2.8.0/kafka_2.13-2.8.0.tgz \
    && tar -xzvf kafka_2.13-2.8.0.tgz \ 
    && rm kafka_2.13-2.8.0.tgz \ 
    && mv kafka_2.13-2.8.0 /opt/kafka

RUN mkdir -p /tmp/kafka/

# Create a folder for storing received nfcapd files
WORKDIR /home/kafka-source-connector/collected_files
# Create a folder for storing converted csv files
WORKDIR /home/kafka-source-connector/collected_files_csv
# Create a folder for storing source code, and any other needed components
WORKDIR /home/kafka-source-connector/

# Copy source code & pom.xml
COPY src ./src
COPY pom.xml ./

# Make script executable
RUN chmod 777 src/main/scripts/start_netflow_connector.sh
RUN chmod +x src/main/scripts/start_netflow_connector.sh

# Build java Kafka Connector
RUN mvn clean compile package

# Create dir for Kafka connectors
WORKDIR /opt/kafka/connectors/

# Copy built jar & config files in kafka connectors dir
RUN cp /home/kafka-source-connector/jar/PalantirNetflowCsvSourceConnect-0.1-SNAPSHOT-jar-with-dependencies.jar ./
RUN cp /home/kafka-source-connector/src/main/resources/palantir-connect-standalone.properties ./
RUN cp /home/kafka-source-connector/src/main/resources/netflow-source-linux.properties ./
RUN cp /home/kafka-source-connector/src/main/resources/log4j.properties ./

# Change workdir to home
WORKDIR /home/kafka-source-connector/

# ENV variables
# Kafka Bootstrap server
ENV KAFKA_IP=10.101.41.255
# Kafka port
ENV KAFKA_PORT=9092
# Name of the connector. This must be unique among all connectors
ENV NAME=source-connector-0
# Port, where Connector Python API, will listens to
ENV API_PORT=7000
# IP of registry service
ENV REGISTRY_IP=registry-service.ti-dcp
# Port, where registry service listens to
ENV REGISTRY_PORT=5000
# Time interval (in seconds) that connector will 
# update its status in registry Service
ENV UPDATE_INTERVAL=60
# Option for benchmarking
ENV BENCHMARK_MODE=true

# Expose required ports
EXPOSE ${API_PORT}

# Run script to register collector, start Python server and Kafka Connector
CMD ["/bin/bash", "src/main/scripts/start_netflow_connector.sh"]
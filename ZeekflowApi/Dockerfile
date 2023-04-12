FROM python:3.8.10-slim

# Install required Linux libraries
RUN apt-get update -y
RUN apt-get install -y dos2unix

# Install required Python packages
COPY requirements.txt /tmp/
RUN pip install -r /tmp/requirements.txt

# Create required folders
RUN mkdir -p /home/zeekflow-integration/collected_files_csv/
RUN mkdir -p /home/zeekflow-integration/collected_files_zeek/
RUN mkdir -p /home/zeekflow-integration/logs/

# Change to home directory
WORKDIR /home/zeekflow-integration/

# Copy required files
COPY zeekflow_api.py /home/zeekflow-integration/zeekflow_api.py
COPY zeeklogs_consumer.py /home/zeekflow-integration/zeeklogs_consumer.py
COPY start_zeekflow_api.sh /home/zeekflow-integration/start_zeekflow_api.sh

# Give permissions
RUN chmod 777 /home/zeekflow-integration/start_zeekflow_api.sh
RUN chmod +x /home/zeekflow-integration/start_zeekflow_api.sh
RUN dos2unix /home/zeekflow-integration/start_zeekflow_api.sh

# Set ENV variables
# Port, where API will listens to for requests
ENV API_PORT=7100
# Directory where zeek logs are stored
ENV ZEEK_LOGS=/home/zeekflow-integration/logs/
# Filename of zeek folder
ENV ZEEK_LOGFILE_NAME=conn.log
# Kafka Bootstrap Server
ENV KAFKA_BOOTSTRAP_SERVER=localhost:9092
# Kafka Topic, where zeeklogs will be sent to
ENV KAFKA_TOPIC=zeeklogs
# Benchmark mode. Some info about benchmarking will be saved in a file
ENV BENCHMARK_MODE=false

# Expose required ports
EXPOSE ${API_PORT}

CMD ["/bin/bash", "/home/zeekflow-integration/start_zeekflow_api.sh"]
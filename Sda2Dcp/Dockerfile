FROM python:3.7.8-slim

# Install required Python packages
COPY requirements.txt /tmp/
RUN pip install -r /tmp/requirements.txt

# Create directory in home where nfcapd files and execution scripts will be saved
WORKDIR /home/Sda2DcpService

# Copy required files
COPY converter.py /home/Sda2DcpService/
COPY start_sda2dcp_converter.sh /home/Sda2DcpService/

RUN chmod 777 start_sda2dcp_converter.sh
RUN chmod +x start_sda2dcp_converter.sh

# Set ENV variables
# Kafka bootstrap servers
ENV KAFKA_BOOTSTRAP_SERVER=localhost:9092
# Input topic, that data from SDA are retrieved
ENV SDA_TOPIC=netflow-raw-sda
# Output topic, that data for DCP will be sent
ENV DCP_TOPIC=netflow-raw-dcp
# Tenant ID (for multi-tenancy support)
ENV TENANT_ID=0

CMD ["/bin/bash", "start_sda2dcp_converter.sh"]
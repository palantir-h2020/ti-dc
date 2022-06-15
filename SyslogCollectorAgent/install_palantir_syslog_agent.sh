#!/bin/bash

# Step counter
COUNTER=0
# Total steps
TOTAL_STEPS=14

read -p "Please enter Logstash host (in format LOGSTASH_IP:LOGSTASH_PORT): " logstash_host

# Run apt-get update
(( COUNTER++ ))
echo "[$COUNTER/$TOTAL_STEPS][INFO] Updating package lists from repositories ..."
echo "[$COUNTER/$TOTAL_STEPS][INFO] Running command: sudo apt-get update"
sudo apt-get update

# Install required packages
(( COUNTER++ ))
echo "[$COUNTER/$TOTAL_STEPS][INFO] Installing required packages ..."
echo "[$COUNTER/$TOTAL_STEPS][INFO] Running command: sudo apt-get install wget tar"
sudo apt-get install wget tar

# Download filebeat
(( COUNTER++ ))
echo "[$COUNTER/$TOTAL_STEPS][INFO] Downloading Filebeat v7.12.1 ..."
echo "[$COUNTER/$TOTAL_STEPS][INFO] Running command: wget https://artifacts.elastic.co/downloads/beats/filebeat/filebeat-7.12.1-linux-x86_64.tar.gz"
wget https://artifacts.elastic.co/downloads/beats/filebeat/filebeat-7.12.1-linux-x86_64.tar.gz

# Untar
(( COUNTER++ ))
echo "[$COUNTER/$TOTAL_STEPS][INFO] Untaring the downloaded tar ..."
echo "[$COUNTER/$TOTAL_STEPS][INFO] Running command: tar -xzvf filebeat-7.12.1-linux-x86_64.tar.gz"
tar -xzvf filebeat-7.12.1-linux-x86_64.tar.gz

# Create directory (if not exists) /opt/palantir
(( COUNTER++ ))
echo "[$COUNTER/$TOTAL_STEPS][INFO] Creating installation directory (if not exists) /opt/palantir/ ..."
echo "[$COUNTER/$TOTAL_STEPS][INFO] Running command: sudo mkdir -p /opt/palantir/"
sudo mkdir -p /opt/palantir/

# Deleting existing Palantir syslog collector (if already exists)
(( COUNTER++ ))
echo "[$COUNTER/$TOTAL_STEPS][INFO] Deleting existing Palantir syslog collector (if already exists) ..."
echo "[$COUNTER/$TOTAL_STEPS][INFO] Running command: sudo rm -r /opt/palantir/syslog-collector"
sudo rm -r /opt/palantir/syslog-collector

# Copy to /opt/palantir
(( COUNTER++ ))
echo "[$COUNTER/$TOTAL_STEPS][INFO] Moving filebeat to /opt/palantir/ ..."
echo "[$COUNTER/$TOTAL_STEPS][INFO] Running command: sudo mv filebeat-7.12.1-linux-x86_64 /opt/filebeat"
sudo mv filebeat-7.12.1-linux-x86_64 /opt/palantir/syslog-collector

# Copy configuration file
(( COUNTER++ ))
echo "[$COUNTER/$TOTAL_STEPS][INFO] Moving configuration file palantir-syslog.yml ..."
echo "[$COUNTER/$TOTAL_STEPS][INFO] Running command: sudo mv palantir-syslog.yml /opt/filebeat/palantir-syslog.yml"
sudo mv palantir-syslog.yml /opt/palantir/syslog-collector/palantir-syslog.yml

# Edit configuration file
(( COUNTER++ ))
echo "[$COUNTER/$TOTAL_STEPS][INFO] Editing configuration file palantir-syslog.yml ..."
echo "[$COUNTER/$TOTAL_STEPS][INFO] Running command: sudo sed -i 's/logstash_hosts_string/$logstash_host' /opt/palantir/syslog-collector/palantir-syslog.yml "
sudo sed -i "s/logstash_hosts_string/$logstash_host/g" /opt/palantir/syslog-collector/palantir-syslog.yml

# Copying service file
(( COUNTER++ ))
echo "[$COUNTER/$TOTAL_STEPS][INFO] Moving service file palantir-syslog.service ..."
echo "[$COUNTER/$TOTAL_STEPS][INFO] Running command: sudo mv palantir-syslog.service /etc/systemd/system/palantir-syslog.service"
sudo mv palantir-syslog.service /etc/systemd/system/palantir-syslog.service

# Reload services list
(( COUNTER++ ))
echo "[$COUNTER/$TOTAL_STEPS][INFO] Reloading system services ..."
echo "[$COUNTER/$TOTAL_STEPS][INFO] Running command: sudo systemctl daemon-reload"
sudo systemctl daemon-reload

# Enable Palantir syslog collector service
(( COUNTER++ ))
echo "[$COUNTER/$TOTAL_STEPS][INFO] Enabling Palantir syslog collector service ..."
echo "[$COUNTER/$TOTAL_STEPS][INFO] Running command: sudo systemctl enable palantir-syslog.service"
sudo systemctl enable palantir-syslog.service

# Start Palantir syslog collector service
(( COUNTER++ ))
echo "[$COUNTER/$TOTAL_STEPS][INFO] Starting Palantir syslog collector service ..."
echo "[$COUNTER/$TOTAL_STEPS][INFO] Running command: sudo systemctl restart palantir-syslog.service"
sudo systemctl restart palantir-syslog.service

# Remove filebeat tar
(( COUNTER++ ))
echo "[$COUNTER/$TOTAL_STEPS][INFO] Cleaning up ..."
echo "[$COUNTER/$TOTAL_STEPS][INFO] Running command: sudo rm filebeat-7.12.1-linux-x86_64.tar.gz"
sudo rm filebeat-7.12.1-linux-x86_64.tar.gz
# Palantir Syslog Collector Agent (Ubuntu)

## Installation

### Install using ready script (sudo privileges required).
- Make install_palantir_syslog_agent.sh executable: `sudo chmod +x install_palantir_syslog_agent.sh` .
- Give requested input, about where logstash is running when prompted from the script.
- Make sure that service is running, after installation finished. Run `sudo systemctl status palantir-syslog.service`. Status must be active(running).

### Manual installation (sudo privileges required).
- Edit file palantir-syslog.yml:
- - In output.logstash replace "logstash_hosts_string" with "LOGSTASH_IP:LOGSTASH_PORT".
- Update package lists from repositories: 
`sudo apt-get update`
- Install required packages: 
`sudo apt-get install wget tar`
- Download Filebeat v7.12.1: 
`wget https://artifacts.elastic.co/downloads/beats/filebeat/filebeat-7.12.1-linux-x86_64.tar.gz`
- Untar filebeat: 
`tar -xzvf filebeat-7.12.1-linux-x86_64.tar.gz`
- Create installation directory (if not exists) /opt/palantir/: 
`sudo mkdir -p /opt/palantir/`
- Delet existing Palantir syslog collector (if already exists): 
`sudo rm -r /opt/palantir/syslog-collector`
- Move filebeat to /opt/palantir/: 
`sudo mv filebeat-7.12.1-linux-x86_64 /opt/palantir/syslog-collector`
- Move configuration file palantir-syslog.yml: 
`sudo mv palantir-syslog.yml /opt/palantir/syslog-collector/palantir-syslog.yml`
- Move service file palantir-syslog.service: 
`sudo mv palantir-syslog.service /etc/systemd/system/palantir-syslog.service`
- Reload system services: 
`sudo systemctl daemon-reload`
- Enable Palantir syslog collector service: 
`sudo systemctl enable palantir-syslog.service`
- Start Palantir syslog collector service: 
`sudo systemctl restart palantir-syslog.service`
- Clean up: 
`sudo rm filebeat-7.12.1-linux-x86_64.tar.gz`
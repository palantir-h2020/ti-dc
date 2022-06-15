# Logstash (v7.12.1)

This instance of Logstash has been configured in order to receive input from Beats (i.e. Filebeat) and forward the data in both Kafka and Elasticsearch.

## Configuration
- *logstash.yml*:
The values of name, namespace and target port in Service kind can be changed. Also, the values name, namespace nodeSelector/alias, hostPort can be changed in Deployment kind. Also, the volumes section in Deployment must be changed in order to match, where the configuration (directory volumes), has been stored in order to be mounted.

- *volumes/config/logstash.yml*:
This file is used to setup the interface and port, that Logstash will listen to.

- *volumes/config/logstash.yml*:
This file is used to show Logstash, where to find the pipelines configuration (inside container). This must not be changed, unless the mounting point (or the default path) of the stored pipelines is different that the one described in the file and in the pod YML file.

- *volumes/pipeline/logstash.conf*:
Field "input/beats/port" sets the port, that Logstash will listen in order to retrieve data from Beats. 
For Kafka output fields "output/kafka/bootstrap_servers" and "output/kafka/topic" must be configured in order to match the destination Kafka deployment.
For Elasticsearch output fields "output/elasticsearch/hosts", "output/elasticsearch/user", "output/elasticsearch/password" and "output/elasticsearch/index" must be configured in order to match the destination Elastic deployment. Also field "output/elasticsearch/template" must be removed if the default template must be used, or modified if another .json file must be used for the template.

- *volumes/templates/syslog-raw-index-template.json*:
Field "index_patterns" is the one, that defines the index, where the ingested data will be stored to Elastic. 
**IMPORTANT:** Using this configuration, .keyword fields are disabled for all text fields. If .keyword fields are required, please remove the mounting of the volumes/templates folder from the created pod.
# Threat Intelligence / Distributed Collection

## Project modules

This project consists of five (5) modules.

- OpenDistro for ElasticSearch among Kibana
- Elastic Sink Connector (Raw & Preprocessed Netflow Data Store To Elastic From Kafka)
- Registry Service
- Collector Load Balancer
- Netflow Source Connector (Raw Netflow Data Ingestion To Kafka)

## Deployment

It is a fully distributed project thanks to [Docker](https://www.docker.com/) and [Kubernetes](https://kubernetes.io/).
Executing,

```bash
sudo bash deploy-in-k8s.sh
```

- Six (6) docker images will be built (ODFE, ODFE-Kibana, Elastic Sink Connector, Registry Service, Collector Load
  Balancer, Netflow Source Connector).
- Afterwards, these 6 images become K8s deployments with 6 K8s services respectively will be in a Kubernetes namespace.

If everything gone well, the elastic-sink-connector app logger will show this:

```
INFO yy/mm/dd hh:mm:ss,XXX XXXXXXX sink.ElasticSinkTask [task-thread-elastic-sink-connector-0] Retrieved xxx messages (xxx raw netflows, xxx prepprocessed netflows)
```

And the registry service app logger will show this:

```
yy/mm/dd hh:mm:ss,XXX INFO Number of running services: 1
yy/mm/dd hh:mm:ss,XXX INFO {'netflow-source-connector-service': 'http://netflow-source-connector-service:7000'}
```

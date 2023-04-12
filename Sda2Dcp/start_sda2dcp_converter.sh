#!/bin/bash

python converter.py -b ${KAFKA_BOOTSTRAP_SERVER} -sda ${SDA_TOPIC} -dcp ${DCP_TOPIC}
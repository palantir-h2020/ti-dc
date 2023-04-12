#!/bin/bash

python zeeklogs_consumer.py -d ${ZEEK_LOGS} -l ${ZEEK_LOGFILE_NAME} -k ${KAFKA_BOOTSTRAP_SERVER} -t ${KAFKA_TOPIC} -b ${BENCHMARK_MODE} > "logs/zeeklog_consumer.log" 2>&1 &
python zeekflow_api.py -p ${API_PORT} -d ${ZEEK_LOGS} -l ${ZEEK_LOGFILE_NAME} -b ${BENCHMARK_MODE}
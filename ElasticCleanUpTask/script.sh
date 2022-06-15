#!/bin/sh

ELASTIC_IP='10.101.41.42'
ELASTIC_PORT='9200'
USERNAME='admin'
PASSWORD='admin'
IDX_PTRN_1='metricbeat-*'
IDX_PTRN_2='syslog-raw-dev'

###############################################################################################################################
############################################# --- Cleaning up indices --- #####################################################
###############################################################################################################################

curl -k -X POST "https://$USERNAME:$PASSWORD@$ELASTIC_IP:$ELASTIC_PORT/$IDX_PTRN_1/_delete_by_query?pretty" -H 'Content-Type: application/json' -d'
{
  "query": {
    "bool": {
      "filter": [
        {
          "range": {
            "@timestamp": {
              "lt": "now-1d"
            }
          }
        }
      ]
    }
  }
}'

#curl -k -X POST "https://$USERNAME:$PASSWORD@$ELASTIC_IP:$ELASTIC_PORT/$IDX_PTRN_2/_delete_by_query?pretty" -H 'Content-Type: application/json' -d'
#{
#  "query": {
#    "bool": {
#      "filter": [
#        {
#          "range": {
#            "@timestamp": {
#              "lt": "now-3d"
#            }
#          }
#        }
#      ]
#    }
#  }
#}'

###############################################################################################################################
############################################ --- Deleting empty indices --- ###################################################
###############################################################################################################################

for idx in $(curl -X GET -k "https://$USERNAME:$PASSWORD@$ELASTIC_IP:$ELASTIC_PORT/_all/_settings?pretty" -s | jq 'keys' | sed 's/["|,]//g' | grep -E '[[:alpha:]]+'); do
    count=$(curl -X GET -k "https://$USERNAME:$PASSWORD@$ELASTIC_IP:$ELASTIC_PORT/$idx/_count" -s | jq '.count')
    if (( $count == 0 )); then
        if [[ ${idx:0:1} != "." ]]; then
            echo "Deleting index: $idx ..."
            echo
            curl -X DELETE -k "https://$USERNAME:$PASSWORD@$ELASTIC_IP:$ELASTIC_PORT/$idx"
        fi
    fi
done
#!/bin/bash

# Check if required packages are installed. If not, install them
if ! type "inotifywait" > /dev/null; then
    echo "inotifywait is not installed. It will install now."
    apt-get install -y inotify-tools
fi
if ! type "nfdump" > /dev/null; then
    echo "nfdump is not installed. It will install now."
    apt-get install -y nfdump
fi

# Start nfcapd process in background
echo "Starting nfcapd in port ${NFCAPD_PORT} with time interval equal to ${NFCAPD_INTERVAL_SECONDS} seconds"
nfcapd -D -t ${NFCAPD_INTERVAL_SECONDS} -T all -w -l ./netflows/ -p ${NFCAPD_PORT}

inotifywait -m ./netflows/ -e moved_to --exclude nfcapd.current.* --exclude targets |
    while read dir action n_file; 
    do
        file="netflows/${n_file}"

        echo "The file '$file' appeared in directory '$dir' via '$action'"
	    if [[ $file == nfcapd.current* ]]; then
	        echo "Ignoring nfcapd.current file"
	    else
            if [ "${BENCHMARK_MODE}" = true ] ; then
                echo "$file detect $(date +%s)" >> benchmark.txt    
            fi
            
            echo "[INFO] $file detected. Searching for available service and sending the file ($n_file)"
            python send_nfcapd_file.py -rip ${REGISTRY_IP} -rp ${REGISTRY_PORT} -f $file -n $n_file

            if [ "${BENCHMARK_MODE}" = true ] ; then
                echo "$file send $(date +%s)" >> benchmark.txt
            fi

	        echo "[INFO] Deleting file $file."
            rm $file
	    fi
    done
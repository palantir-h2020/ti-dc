#!/bin/sh

echo "Cron is starting. Job will be running every day at Midnight."

# start cron
/usr/sbin/crond -f -l 8

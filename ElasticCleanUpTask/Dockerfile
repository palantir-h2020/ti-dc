FROM alpine:3.15.4

RUN apk add curl

RUN apk add wget

RUN wget https://github.com/stedolan/jq/releases/download/jq-1.6/jq-linux64 && \
    mv jq-linux64 /usr/local/bin/jq && \
    chmod +x /usr/local/bin/jq

# Run Cron Job Every Day at Midnight 
ADD crontab.txt /crontab.txt

ADD script.sh /script.sh

COPY entry.sh /entry.sh

RUN chmod 755 /script.sh /entry.sh

RUN /usr/bin/crontab /crontab.txt

CMD ["/entry.sh"]

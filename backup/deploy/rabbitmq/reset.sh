#!/bin/bash
[ "`docker network ls | grep net_cetcxl_app | awk '{print $3}'`"  != "overlay" ] &&  docker network create --attachable -d overlay net_cetcxl_app
docker rm -f rabbitmq &> /dev/null
rm -rf /var/production/rabbitmq
docker-compose -f /deploy/rabbitmq/docker-compose.yml up -d 2> /dev/null
sleep 3s
docker ps | grep rabbitmq  || exit 1

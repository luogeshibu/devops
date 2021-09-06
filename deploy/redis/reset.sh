#!/bin/bash
#The script is executed on the deployment machine.
[ "`docker network ls | grep net_cetcxl_app | awk '{print $3}'`" != "overlay" ] && docker network create --attachable -d overlay net_cetcxl_app
rm -rf /var/production/redis
docker rm -f redis &> /dev/null
#docker service rm cetcxl_redis &> /dev/null
#image=`cat /deploy/redis/redis-compose.yml | grep image | awk '{print $2}'`
#image_tag=`cat /deploy/redis/redis-compose.yml | grep image | awk '{print $2}' | awk -F ":" '{print $2}'`
#docker images $image | grep $image_tag || docker pull $image
#sleep 5s
docker-compose -f /deploy/redis/docker-compose.yml up -d 2> /dev/null
#docker stack deploy -c /deploy/redis/redis-compose.yml cetcxl 2> /dev/null
sleep 10s
docker ps | grep redis

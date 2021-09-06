#!/bin/bash
#The script is executed on the deployment machine.
ES_LUXICON_URL=$1
[ -z "$ES_LUXICON_URL" ] && echo "need ES_LUXICON_URL parameter!" && exit 1
ES_LUXICON_URL_HOST=`echo $ES_LUXICON_URL | awk -F "/" '{print $3}'`
sysctl -w vm.max_map_count=262144
[ "`docker network ls | grep net_cetcxl_app | awk '{print $3}'`" != "overlay" ] && docker network create --attachable -d overlay net_cetcxl_app
docker rm -f elasticsearch1 elasticsearch2 elasticsearch3 head &> /dev/null
docker volume prune -f
docker-compose -p cetcxl -f /deploy/elasticsearch-cluster/elasticsearch-compose.yml up -d
sleep 5s
for container_name in `docker ps | awk '{print $NF}' | grep elasticsearch`
do
  docker exec -i $container_name sed -i "s#words_location#$ES_LUXICON_URL#g" /usr/share/elasticsearch/config/analysis-ik/IKAnalyzer.cfg.xml
  docker exec -i $container_name sed -i "s#172.16.5.11#$ES_LUXICON_URL_HOST#g" /usr/lib/jvm/java-1.8.0-openjdk-1.8.0.161-0.b14.el7_4.x86_64/jre/lib/security/java.policy
done
docker restart elasticsearch1 elasticsearch2 elasticsearch3

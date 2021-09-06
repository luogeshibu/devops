#!/bin/bash
#The script is executed on the deployment machine.
[ "`docker network ls | grep net_cetcxl_app | awk '{print $3}'`" != "overlay" ] && docker network create --attachable -d overlay net_cetcxl_app
docker run --rm --network net_cetcxl_app -i 172.16.101.214:5000/mysql-cli:stable ping -c 3 mysql
if [ $? -ne 0 ]; then
  echo "Please deploy mysql cluster first!"
  exit 1
fi
docker rm -f alpha-server &> /dev/null
docker run --net net_cetcxl_app --name alpha-server --restart always -d -p 8080:8080 -p 8090:8090 -e "JAVA_OPTS=-Dspring.profiles.active=mysql -Dloader.path=/maven/saga/plugins -Dspring.datasource.url=jdbc:mysql://mysql:3306/saga?useSSL=false" -e "TZ=Asia/Shanghai" 172.16.101.214:5000/alpha-server:stable
sleep 5s
[ `docker ps | grep alpha-server | wc -l` -eq 1 ] && echo "alpha-server service status is ok."

#!/bin/bash
#use swarm network($DOCKER_NETWORK)
#The script is executed on the deployment machine.
[ "`docker network ls | grep net_cetcxl_app | awk '{print $3}'`" != "overlay" ] && docker network create --attachable -d overlay net_cetcxl_app
[ `docker ps | grep tracker* | wc -l` -eq 2 ] && [ `docker ps | grep storage* | wc -l` -eq 2 ]
if [ $? -ne 0 ]; then
  docker rm -f tracker0 tracker1 storage0 storage1 &> /dev/null
  sleep 10s
  docker run -dti --network=net_cetcxl_app --name tracker0  -e PORT=22122 quentinyy/fastdfs-docker:alpine tracker
  docker run -dti --network=net_cetcxl_app --name tracker1  -e PORT=22122 quentinyy/fastdfs-docker:alpine tracker
  docker run -dti --network=net_cetcxl_app --name storage0 -e GROUP_NAME=group1 -e GROUP_COUNT=1 quentinyy/fastdfs-docker:alpine storage tracker0:22122 tracker1:22122
  docker run -dti --network=net_cetcxl_app --name storage1 -e GROUP_NAME=group1 -e GROUP_COUNT=1 quentinyy/fastdfs-docker:alpine storage tracker0:22122 tracker1:22122
  sleep 10s
fi
#docker run -dti --network=net_cetcxl_app --name storage2 -e GROUP_NAME=group1 -e GROUP_COUNT=1 -v /var/fdfs/storage2:/var/fdfs quentinyy/fastdfs-docker:alpine storage tracker0:22122 tracker1:22122
#docker run -dti --network=net_cetcxl_app --name storage3 -e GROUP_NAME=group1 -e GROUP_COUNT=1 -v /var/fdfs/storage3:/var/fdfs quentinyy/fastdfs-docker:alpine storage tracker0:22122 tracker1:22122
tracker_ip_0=`docker inspect -f '{{range .NetworkSettings.Networks}}{{.IPAddress}}{{end}}' tracker0`
tracker_ip_1=`docker inspect -f '{{range .NetworkSettings.Networks}}{{.IPAddress}}{{end}}' tracker1`
storage_ip_0=`docker inspect -f '{{range .NetworkSettings.Networks}}{{.IPAddress}}{{end}}' storage0`
storage_ip_1=`docker inspect -f '{{range .NetworkSettings.Networks}}{{.IPAddress}}{{end}}' storage1`
[ ! -z "$tracker_ip_0" ] && [ ! -z "$tracker_ip_1" ] && [ ! -z "$storage_ip_0" ] && [ ! -z "$storage_ip_1" ] || exit 1
docker rm -f tracker0 tracker1 storage0 storage1 &> /dev/null
rm -rf /var/production/fdfs/
docker run -dti --network=net_cetcxl_app --ip $tracker_ip_0 --restart always --name tracker0  -e PORT=22122 -v /var/production/fdfs/tracker0:/var/fdfs quentinyy/fastdfs-docker:alpine tracker
docker run -dti --network=net_cetcxl_app --ip $tracker_ip_1 --restart always --name tracker1  -e PORT=22122 -v /var/production/fdfs/tracker1:/var/fdfs quentinyy/fastdfs-docker:alpine tracker
docker run -dti --network=net_cetcxl_app --ip $storage_ip_0 --restart always --name storage0 -e GROUP_NAME=group1 -e GROUP_COUNT=1 -v /var/production/fdfs/storage0:/var/fdfs quentinyy/fastdfs-docker:alpine storage tracker0:22122 tracker1:22122
docker run -dti --network=net_cetcxl_app --ip $storage_ip_1 --restart always --name storage1 -e GROUP_NAME=group1 -e GROUP_COUNT=1 -v /var/production/fdfs/storage1:/var/fdfs quentinyy/fastdfs-docker:alpine storage tracker0:22122 tracker1:22122
sleep 5s
[ `docker ps | grep tracker* | wc -l` -eq 2 ] && [ `docker ps | grep storage* | wc -l` -eq 2 ] || exit 1

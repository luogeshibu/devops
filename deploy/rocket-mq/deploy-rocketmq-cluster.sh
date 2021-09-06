#!/bin/bash
#do not execute on jenkins work node(agent), so you need $1 $2......
#The script is executed on the deployment machine.
#use swarm network(net_cetcxl_app)
[ "`docker network ls | grep net_cetcxl_app | awk '{print $3}'`" != "overlay" ] && docker network create --attachable -d overlay net_cetcxl_app
docker rm -f rmqserver rmqbroker rmqconsole &> /dev/null
#docker service rm cetcxl_rmqserver &> /dev/null
#docker service rm cetcxl_rmqbroker &> /dev/null
#docker service rm cetcxl_rmqconsole &> /dev/null
DEPLOY_HOST_IP=$1
#deploy rocketmq (one master)
echo "$DEPLOY_HOST_IP" | grep "^172"
if [ $? -eq 0 ]; then
  echo "brokerIP1 = $DEPLOY_HOST_IP" >> /deploy/rocket-mq/broker.conf
else
  echo "brokerIP1 = rmqbroker" >> /deploy/rocket-mq/broker.conf
fi
sleep 5s
docker run -d -p 9876:9876 --network net_cetcxl_app --restart always --name rmqserver  172.16.101.214:5000/rocketmq:server
docker run -d --network net_cetcxl_app -p 10911:10911 -p 10909:10909 --restart always --name rmqbroker --link rmqserver:namesrv -e "NAMESRV_ADDR=namesrv:9876" -e "JAVA_OPTS=-Duser.home=/opt"  -e "JAVA_OPT_EXT=-server -Xms128m -Xmx128m -Xmn128m" -v /deploy/rocket-mq/broker.conf:/etc/rocketmq/broker.conf  172.16.101.214:5000/rocketmq:broker
docker run -d --network net_cetcxl_app --restart always --name rmqconsole --link rmqserver:namesrv -e "JAVA_OPTS=-Drocketmq.namesrv.addr=namesrv:9876 -Dcom.rocketmq.sendMessageWithVIPChannel=false" -p 8180:8080 -t 172.16.101.214:5000/rocketmq-console-ng
#docker stack deploy -c /deploy/rocket-mq/rocketmq-compose.yml cetcxl 2> /dev/null
sleep 5s
[ `docker ps | grep "rmq" | wc -l` -eq 3 ] && echo "rocket-mq service status is ok."

#!/bin/bash
#do not execute on jenkins work node(agent)
#The script is executed on the deployment machine.
#[warning]use swarm network(net_cetcxl_app)!
[ "`docker network ls | grep net_cetcxl_app | awk '{print $3}'`" != "overlay" ] && docker network create --attachable -d overlay net_cetcxl_app
SET_MYSQL_PASSWORD=root001
rm -rf /var/production/mysql*
docker rm -f mysql-slave-ks mysql-slave-notice mysql-slave-score mysql-master-sys mysql &> /dev/null
#mysql-cluster
#[ "$SET_MYSQL_PASSWORD" == "root001" ] || sed -i "s/root001/$SET_MYSQL_PASSWORD/g" /deploy/mysql-cluster/mysql-cluster-compose.yml
docker-compose -f /deploy/mysql-cluster/mysql-cluster-compose.yml up -d 2> /dev/null
sleep 120s
#config master
docker exec -i mysql-master-sys mysql -u root --password=$SET_MYSQL_PASSWORD -e 'grant replication slave on *.* to "rep"@"10.%.%.%" identified by "'"$SET_MYSQL_PASSWORD"'";'
docker exec -i mysql-master-sys mysql -u root --password=$SET_MYSQL_PASSWORD -e 'FLUSH PRIVILEGES;'
master_log_file=`docker exec -i mysql-master-sys mysql -u root --password=$SET_MYSQL_PASSWORD -e 'show master status;' | grep mysql-bin  | awk '{print $1}'`
position=`docker exec -i mysql-master-sys mysql -u root --password=$SET_MYSQL_PASSWORD -e 'show master status;' | grep mysql-bin  | awk '{print $2}'`
#master_ip=`docker inspect -f '{{range .NetworkSettings.Networks}}{{.IPAddress}}{{end}}' mysql-master-sys`
#config slave
for container_name in $(docker ps | grep mysql-slave*  | awk '{print $NF}')
do
    docker exec -i $container_name mysql -u root --password=$SET_MYSQL_PASSWORD -e 'stop slave;'
    docker exec -i $container_name mysql -u root --password=$SET_MYSQL_PASSWORD -e 'reset slave;'
    docker exec -i $container_name mysql -u root --password=$SET_MYSQL_PASSWORD -e 'change master to master_host="mysql-master-sys",master_port=3306,master_user="rep",master_password="'"$SET_MYSQL_PASSWORD"'",master_log_file="'"$master_log_file"'",master_log_pos='"$position"';'
    docker exec -i $container_name mysql -u root --password=$SET_MYSQL_PASSWORD -e 'start slave;'
done

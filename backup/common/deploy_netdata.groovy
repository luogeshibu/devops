#!groovy
// Scripted Pipeline
  podTemplate(cloud: "kubernetes-hangli",yaml: """
apiversion: v1
kind: Pod
spec:
  cloud: kubernetes-hangli
  containers:
  - name: jnlp
    image: 'docker.cetcxl.local/jenkins-slave:test'
    imagePullPolicy: Always
    args: ['\$(JENKINS_SECRET)', '\$(JENKINS_NAME)']
    resources:
      limits:
        memory: "512Mi"
        cpu: "1000m"
      requests:
        memory: "256Mi"
        cpu: "500m"
  - name: alpine
    image: 'docker.cetcxl.local/alpine:ci-v1.0'
    imagePullPolicy: Always
    command: ['sh']
    tty: true
    resources:
      limits:
        memory: "512Mi"
        cpu: "1000m"
      requests:
        memory: "256Mi"
        cpu: "500m"
  imagePullSecrets:
  - name: docker-hangli
"""
  ){
    node(POD_LABEL){
	stage("部署"){
		container('alpine'){
			checkout([$class: 'GitSCM',
				branches: [[name: "master"]],
				doGenerateSubmoduleConfigurations: false,
				extensions: [[$class: 'RelativeTargetDirectory', relativeTargetDir: 'netdata']],
				submoduleCfg: [],
				userRemoteConfigs: [[credentialsId: 'gitlab', url: 'http://172.16.101.211/devops/deploy.git']]
			   ])
			// copy binary to remot
            sh "cd netdata;sshpass -p ${params.PASSWD} scp -o StrictHostKeyChecking=no -r ./netdata ${params.USER}@${params.TARGET_IP}:/root;"
            // install netdata on remote server
            sh "sshpass -p ${params.PASSWD} ssh -o StrictHostKeyChecking=no ${params.USER}@${params.TARGET_IP} bash /root/netdata/binary/kickstart-static64.sh --dont-wait --local-files /root/netdata/binary/netdata-v1.31.0.gz.run /root/netdata/binary/sha256sums.txt"
            // config netdata
            sh "sshpass -p ${params.PASSWD} ssh -o StrictHostKeyChecking=no ${params.USER}@${params.TARGET_IP} cp /root/netdata/binary/netdata.conf /opt/netdata/etc/netdata"
            sh "sshpass -p ${params.PASSWD} ssh -o StrictHostKeyChecking=no ${params.USER}@${params.TARGET_IP} systemctl restart netdata"
            sh "sshpass -p ${params.PASSWD} ssh -o StrictHostKeyChecking=no ${params.USER}@${params.TARGET_IP} systemctl status netdata"
            //regist to consul
            data = "{\"name\": \"monitor-hangli_${params.TARGET_IP}\", \"meta\": {\"host_ip\": \"${params.TARGET_IP}\"}, \"address\": \"${params.TARGET_IP}\", \"port\": 19999, \"tags\": [\"monitor-hangli\"]}"
            header = "X-Consul-Token: cd26394d-9e7a-87c9-b421-dbc5e6926727"
            sh "curl -X PUT -H '${header}' -d '${data}' https://consul.scba.org.cn/v1/agent/service/register"
        }//container
	}//stage
	
	}//node
  
 }//podTemplate


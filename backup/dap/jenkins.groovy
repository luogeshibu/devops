#!groovy
// Scripted Pipeline
  podTemplate(cloud: "kubernetes-${params.K8S}",yaml: """
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
  - name: npm
    image: docker.cetcxl.local/node:stretch
    imagePullPolicy: Always
    command: ['cat']
    tty: true
    resources:
      limits:
        memory: "3072Mi"
        cpu: "2"
      requests:
        memory: "1024Mi"
        cpu: "1"
  - name: maven
    image: docker.cetcxl.local/mvn:ci
    imagePullPolicy: Always
    command: ['cat']
    tty: true
    resources:
      limits:
        memory: "3072Mi"
        cpu: "2"
      requests:
        memory: "1024Mi"
        cpu: "1"
  - name: kaniko
    image: docker.cetcxl.local/kaniko-executor:debug-v0.24.0
    imagePullPolicy: IfNotPresent
    command: ['/busybox/cat']
    tty: true
    resources:
      limits:
        memory: "512Mi"
        cpu: "1000m"
      requests:
        memory: "256Mi"
        cpu: "500m"
    volumeMounts:
      - name: jenkins-docker-cfg
        mountPath: /kaniko/.docker
  imagePullSecrets:
  - name: docker-hangli
  volumes:
  - name: jenkins-docker-cfg
    configMap:
      name: nexus-cred
"""
  ){
	
    // Pipeline Parameters
    properties([
      parameters([
        gitParameter(name: 'BRANCH_BACKEND',
                     description: '选择后端代码分支',
                     branchFilter: 'origin/(.*)',
                     type: 'PT_BRANCH_TAG',
                     defaultValue: 'master',
                     useRepository: '.*dap-back-end-service.git',
                     sortMode: 'ASCENDING'),				 
		choice(choices: ['test', 'prod'], description: '测试环境为test，生产环境为prod', name: 'CONFIG'),
		choice(choices: ['hangli', 'telecom'], description: 'k8s集群位置,默认hangli为测试环境，telecom为线上环境', name: 'K8S'),
		choice(choices: ['xyf', 'dap', 'dap-test'], description: '部署namespace名称', name: 'NAMESPACE'),
		booleanParam(defaultValue: true, description: '是否编译代码', name: 'compile'),
		extendedChoice(defaultValue: 'dap-data-hive-sync-server,dap-data-manage-platform,dap-open-platform,dap-operate-platform,dap-front-opendata-platform,dap-front-operation-management-platform,dap-front-data-management-platform', description: '选择需要部署的组件', descriptionPropertyValue: '部署dap-data-hive-sync-server,部署dap-data-manage-platform,部署dap-open-platform,部署dap-operate-platform,部署dap-front-opendata-platform,部署dap-front-operation-management-platform,部署dap-front-data-management-platform', multiSelectDelimiter: ',', name: 'deploy_list', quoteValue: false, saveJSONParameterToFile: false, type: 'PT_CHECKBOX', value: 'dap-data-hive-sync-server,dap-data-manage-platform,dap-open-platform,dap-operate-platform,dap-front-opendata-platform,dap-front-operation-management-platform,dap-front-data-management-platform', visibleItemCount: 7)	
      ])
    ])
	
    node(POD_LABEL) {
	
	env.IMAGE_TAG_BACKEND = 'latest'
    if (params.BRANCH_BACKEND != 'master') {
        env.IMAGE_TAG_BACKEND = "${params.BRANCH_BACKEND}"
    }

	env.NAMESPACE = params.NAMESPACE
	env.CONFIG = params.CONFIG
	list = env.deploy_list
	echo list
	echo ","
	String[] str;
    str = list.split(",");
    str.each{
	    echo it
		if ( it == 'dap-data-hive-sync-server'){
            env.dap_data_hive_sync_server=true
			echo "是否部署dap-data-hive-sync-server:${dap_data_hive_sync_server}"
        }
		if ( it == 'dap-data-manage-platform'){
            env.dap_data_manage_platform=true
			echo "是否部署dap-data-manage-platform:${dap_data_manage_platform}"
        }
        if ( it == 'dap-open-platform'){
            env.dap_open_platform=true
			echo "是否部署dap-open-platform:${dap_open_platform}"
        }
        if ( it == 'dap-operate-platform'){
            env.dap_operate_platform=true
			echo "是否部署dap-operate-platform:${dap_operate_platform}"
        }
    }
	stage('编译') {
		download_code:{
			container('maven'){
				checkout([$class: 'GitSCM',
						branches: [[name: "${params.BRANCH_BACKEND}"]],
						doGenerateSubmoduleConfigurations: false,
						extensions: [[$class: 'RelativeTargetDirectory', relativeTargetDir: 'dap']],
						submoduleCfg: [],
						userRemoteConfigs: [[credentialsId: 'gitlab', url: 'http://gitlab.cetcxl.local/dap/dap-back-end-service.git']]
					   ])						
			}	
		}
		if (env.compile == 'true') {
    	    parallel build_dap_back_end_service: {
    			if (env.dap_data_hive_sync_server == 'true' || env.dap_data_manage_platform == 'true' || env.dap_open_platform == 'true' || env.dap_operate_platform == 'true') {
    				container('maven'){
    				    echo "====== [DEBUG]: Before building dap_back_end_service======"
    				    echo "Branch/Tag: ${params.BRANCH_BACKEND}"
    				    echo "Image tag: ${env.BRANCH_BACKEND}"
    				    sh 'cd dap;mvn clean install -U -Dmaven.test.skip=true'
    				    echo "====== [DEBUG]: After building ======"
    				}			
    			}		
    		}		    
		}

	}
	

    stage('build Docker image'){
        if (env.compile == 'true') {
    		build_dap_data_hive_sync_server: {
    			if (env.dap_data_hive_sync_server == 'true') {
    				container('kaniko') {
    					sh "/kaniko/executor -f `pwd`/dap/dap-data-hive-sync-server/Dockerfile -c `pwd`/dap/dap-data-hive-sync-server --insecure --skip-tls-verify --cache=true --destination=docker.cetcxl.local/dap-data-hive-sync-server:${IMAGE_TAG_BACKEND}"
    				}			
    			}			
    		}
			
    		build_dap_data_manage_platform: {
    			if (env.dap_data_manage_platform == 'true') {
    				container('kaniko') {
    					sh "/kaniko/executor -f `pwd`/dap/dap-data-manage-platform/Dockerfile -c `pwd`/dap/dap-data-manage-platform --insecure --skip-tls-verify --cache=true --destination=docker.cetcxl.local/dap-data-manage-platform:${IMAGE_TAG_BACKEND}"
    				}			
    			}			
    		}
			
    		build_dap_open_platform: {
    			if (env.dap_open_platform == 'true') {
    				container('kaniko') {
    					sh "/kaniko/executor -f `pwd`/dap/dap-open-platform/Dockerfile -c `pwd`/dap/dap-open-platform --insecure --skip-tls-verify --cache=true --destination=docker.cetcxl.local/dap-open-platform:${IMAGE_TAG_BACKEND}"
    				}			
    			}			
    		}

    		build_dap_operate_platform: {
    			if (env.dap_operate_platform == 'true') {
    				container('kaniko') {
    					sh "/kaniko/executor -f `pwd`/dap/dap-operate-platform/Dockerfile -c `pwd`/dap/dap-operate-platform --insecure --skip-tls-verify --cache=true --destination=docker.cetcxl.local/dap-operate-platform:${IMAGE_TAG_BACKEND}"
    				}			
    			}			
    		}			
        }
        

    }//stage('build Docker image')
	
    stage('部署'){
		download_yaml: {
			checkout([$class: 'GitSCM',
						branches: [[name: "*/master"]],
						doGenerateSubmoduleConfigurations: false,
						extensions: [[$class: 'RelativeTargetDirectory', relativeTargetDir: 'ci']],
						submoduleCfg: [],
						userRemoteConfigs: [[credentialsId: 'gitlab', url: 'http://gitlab.cetcxl.local/devops/ci-k8s.git']]
					])	
		}		
		cp_k8s_config: {  
			container('jnlp') {	
			    sh "scp ./ci/dap/k8s-cluster/${params.K8S}/config /home/jenkins/.kube"
			}
		}
		deploy_dap_data_hive_sync_server: {
			if (env.dap_data_hive_sync_server == 'true') {
				env.IMAGE_TAG=env.IMAGE_TAG_BACKEND
				container('jnlp') {	
					sh "set +e;kubectl -n ${NAMESPACE} delete configmap dap-data-hive-sync-server;set -e"
					sh "kubectl -n ${NAMESPACE} create configmap dap-data-hive-sync-server --from-env-file=./ci/dap/config/${CONFIG}/dap-data-hive-sync-server.env"	
					sh "envsubst < `pwd`/ci/dap/deploy_yaml/dap-data-hive-sync-server.yaml|kubectl -n ${NAMESPACE} apply -f -"				
				}
			}
		}
		deploy_dap_data_manage_platform: {
			if (env.dap_data_manage_platform == 'true') {
				env.IMAGE_TAG=env.IMAGE_TAG_BACKEND	
				container('jnlp') {
					sh "set +e;kubectl -n ${NAMESPACE} delete configmap dap-data-manage-platform;set -e"
					sh "kubectl -n ${NAMESPACE} create configmap dap-data-manage-platform --from-env-file=./ci/dap/config/${CONFIG}/dap-data-manage-platform.env"	
					sh "envsubst < `pwd`/ci/dap/deploy_yaml/dap-data-manage-platform.yaml|kubectl -n ${NAMESPACE} apply -f -"	
				}
			}
		}
		deploy_dap_open_platform: {
			if (env.dap_open_platform == 'true') {
				env.IMAGE_TAG=env.IMAGE_TAG_BACKEND
				container('jnlp') {				
					sh "set +e;kubectl -n ${NAMESPACE} delete configmap dap-open-platform;set -e"
					sh "kubectl -n ${NAMESPACE} create configmap dap-open-platform --from-env-file=./ci/dap/config/${CONFIG}/dap-open-platform.env"	
					sh "envsubst < `pwd`/ci/dap/deploy_yaml/dap-open-platform.yaml|kubectl -n ${NAMESPACE} apply -f -"	
				}
			}
		}
		deploy_dap_operate_platform: {
			if (env.dap_operate_platform == 'true') {
				env.IMAGE_TAG=env.IMAGE_TAG_BACKEND
				container('jnlp') {				
					sh "set +e;kubectl -n ${NAMESPACE} delete configmap dap-operate-platform;set -e"
					sh "kubectl -n ${NAMESPACE} create configmap dap-operate-platform --from-env-file=./ci/dap/config/${CONFIG}/dap-operate-platform.env"	
					sh "envsubst < `pwd`/ci/dap/deploy_yaml/dap-operate-platform.yaml|kubectl -n ${NAMESPACE} apply -f -"	
				}
			}
		}		
		
		//if (env.CONFIG == 'prod') {
    		deploy_ingress: {
    		    	sh "kubectl -n ${NAMESPACE} apply -f `pwd`/ci/dap/deploy_yaml/dap-ingress.yaml"
    				sh "sleep 10s"
    				sh "kubectl -n ${NAMESPACE} get po -o wide"
    		}
		//}
    }//stage('部署')
      
    }//node(POD_LABEL)
  }//podTemplate


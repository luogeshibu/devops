#!groovy
pipeline {
  agent {
    kubernetes {
      //cloud 'kubernetes'
      defaultContainer 'kaniko'
      yaml """
kind: Pod
spec:
  containers:
  - name: jnlp
    image: 'jenkins/jnlp-slave:3.27-1'
    imagePullPolicy: IfNotPresent
    args: ['\$(JENKINS_SECRET)', '\$(JENKINS_NAME)']
    resources:
      limits:
        memory: "512Mi"
        cpu: "1000m"
      requests:
        memory: "256Mi"
        cpu: "500m"
  - name: kaniko
    image: 172.16.101.214:5000/kaniko-executor:debug-v0.24.0
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
  volumes:
  - name: jenkins-docker-cfg
    configMap:
      name: nexus-cred
  imagePullSecrets:
  - name: ci-nexus-cred
"""
    }
  }
  parameters {
    gitParameter name: 'BRANCH_TAG',
                 branchFilter: 'origin/(.*)',
                 type: 'PT_BRANCH_TAG',
                 defaultValue: 'master',
                 useRepository: '.*hellopy.git'
  }
  stages {
    stage('Build with Kaniko') {
      steps {
        script {
          def image_tag = 'latest'
          if (params.BRANCH_TAG != 'master') {
            image_tag = "${params.BRANCH_TAG}"
          }

          container('kaniko') {
          //git credentialsId: 'ted_gitlab', url: 'http://172.16.101.211/zhangpeng/ci.git'
          checkout([$class: 'GitSCM',
                    branches: [[name: "${params.BRANCH_TAG}"]],
                    doGenerateSubmoduleConfigurations: false,
                    extensions: [],
                    submoduleCfg: [],
                    userRemoteConfigs: [[credentialsId: 'ted_gitlab', url: 'http://172.16.101.211/zhangpeng/hellopy.git']]
                   ])
          //sh "pwd"
          //sh "ls"
          sh "/kaniko/executor -f `pwd`/Dockerfile -c `pwd` --insecure --skip-tls-verify --cache=true --verbosity=debug --destination=172.16.101.214:5000/hellopy:${image_tag}"
          }
        }//script
      }
    }
  }
}

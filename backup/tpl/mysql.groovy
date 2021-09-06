#!groovy
// Scripted Pipeline
timestamps{
  podTemplate(yaml: """
kind: Pod
spec:
  containers:
  - name: jnlp
    image: 172.16.101.214:5000/jenkins/jnlp-slave:4.3-7-alpine
    imagePullPolicy: IfNotPresent
    args: ['\$(JENKINS_SECRET)', '\$(JENKINS_NAME)']
    resources:
      limits:
        memory: "512Mi"
        cpu: "1000m"
      requests:
        memory: "256Mi"
        cpu: "500m"
  imagePullSecrets:
  - name: ci-nexus-cred
  volumes:
  - name: jenkins-docker-cfg
    configMap:
      name: nexus-cred
"""
  ){
    // Pipeline Parameters
    properties([
      parameters([
        //gitParameter(name: 'BRANCH_TAG',
        //             branchFilter: 'origin/(.*)',
        //             type: 'PT_BRANCH_TAG',
        //             defaultValue: 'master',
        //             useRepository: '.*.git',
        //             sortMode: 'ASCENDING')
        //]),
        //string(name: "REPO", defaultValue, "abc", description: "指定代码仓库地址")
        string(name: "REPO", description: "指定代码仓库地址"),
        string(name: "VERSION", description: "指定版本或TAG")
      ])
    ])

    node(POD_LABEL){
      stage('Gitttttt'){

        container('jnlp'){
          checkout([$class: 'GitSCM',
                    branches: [[name: "${params.VERSION}"]],
                    doGenerateSubmoduleConfigurations: false,
                    extensions: [],
                    submoduleCfg: [],
                    userRemoteConfigs: [[credentialsId: 'zhangpeng-gitlab', url: "${params.REPO}"]]
                   ])

          env.BRANCH_TAG = "${params.VERSION}"
          env.IMAGE_TAG = 'latest'
          env.REPO = "${params.REPO}"

          if (env.BRANCH_TAG != 'master'){
            env.IMAGE_TAG = "${env.BRANCH_TAG}"
          }

          echo "------ [DEBUG]: Before building ------"
          echo "Brangh/Tag: ${env.BRANCH_TAG}"
          echo "Image tag: ${env.IMAGE_TAG}"
          echo "Repo: ${env.REPO}"
          sh "ls"
          sh "pwd"
          echo "-------------------------------------"

          echo "------ [DEBUG]: After building ------"
          echo "-------------------------------------"
        }
      }//stage


    }//node
  }//podTemplate
}//timestamps

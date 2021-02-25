
@Library('tetrasoft') _

properties([[$class: 'RebuildSettings', autoRebuild: false, rebuildDisabled: false],
    parameters([
        booleanParam(defaultValue: true, description: 'Выполнить тесты', name: 'TEST'),
        booleanParam(defaultValue: true, description: 'Запустить статический анализатор кода', name: 'SONAR')
    ])
])

pipeline {
  agent any
  stages {
    stage('Test') {
      when {expression {params.TEST}}
      steps {
        sh 'mvn clean test'
      }
      post {
        always {
            junit '*/target/surefire-reports/*xml'
        }
        unstable {
          emailext to: 'dev@tetra-soft.ru',
             subject: "Pipeline: ${currentBuild.fullDisplayName}",
             body: "Tests failed with ${env.BUILD_URL}",
             mimeType: 'text/html',
             replyTo: 'dev@tetra-soft.ru'
        }
      }
    }

    stage('SonarQube analysis') {
      when {expression {params.SONAR}}
      steps {
        script {
          scannerHome = tool 'SonarQube Scanner 3';
        }
        withSonarQubeEnv('SonarServer') {
          sh "npm install -D typescript"
          sh "${scannerHome}/bin/sonar-scanner -Dsonar.projectVersion=${BRANCH_NAME}_build_${BUILD_NUMBER}"
        }
      }
    }
  }

  post {
    failure {
      emailext to: 'dev@tetra-soft.ru',
          subject: "Pipeline: ${currentBuild.fullDisplayName}",
          body: "Something is wrong with ${env.BUILD_URL}",
          mimeType: 'text/html',
          replyTo: 'dev@tetra-soft.ru'
    }
  }

  tools {
    maven 'Maven'
    nodejs 'NodeJS 8'
  }

  environment {
    PROJECT_NAME = 'rig_space'
  }

  options {
    buildDiscarder(logRotator(numToKeepStr: '7'))
    disableConcurrentBuilds()
  }
}

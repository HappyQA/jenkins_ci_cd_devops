properties([[$class: 'RebuildSettings', autoRebuild: false, rebuildDisabled: false],
            parameters([
                    booleanParam(defaultValue: true, description: 'Clean Database', name: 'clean_db'),
                    booleanParam(defaultValue: true, description: 'Run Frontend Tests', name: 'frontend'),
                    booleanParam(defaultValue: true, description: 'Run Backend Tests', name: 'backend'),
                    booleanParam(defaultValue: false, description: 'Clean workspace', name: 'ws')
            ])
])

pipeline {

    agent {
        label ''
    }

    stages {
        stage ('Clear Workspace') {
            when {expression {params.ws}}
            steps {
                script {
                    cleanWs()
                }
            }
        }

        stage ('Clear Test Directory') {
            when {expression {params.frontend}}
            steps {
                script {
                    sh "mvn clean"
                }
            }
        }

        stage ('Write credentials') {
            steps {
                script {
                    withCredentials([file(credentialsId: '', variable: 'CONF')]) {
                        def conf = readFile encoding: 'UTF8', file: "$CONF"
                        writeFile encoding: 'UTF-8', file: './src/main/resources/config.properties', text: conf
                    }
                    withCredentials([file(credentialsId: '', variable: 'TESTRAIL')]) {
                        def testrail = readFile encoding: 'UTF8', file: "$TESTRAIL"
                        writeFile encoding: 'UTF-8', file: './src/main/resources/testrail.properties', text: testrail
                    }
                    withCredentials([file(credentialsId: '', variable: 'REST')]) {
                        def rest = readFile encoding: 'UTF8', file: "$REST"
                        writeFile encoding: 'UTF-8', file: './src/main/resources/restclient.properties', text: rest
                    }
                }
            }
        }

        stage ('Clean Database') {
            when {expression {params.clean_db}}
            environment {
                MONGODB = credentials('mongo-mbt-qa')
            }
            steps {
                script {
                    sh('mongo --username $MONGODB_USR --password $MONGODB_PSW --authenticationDatabase admin --host "" --port "" < connect.js')
                    echo "Collection Drop"
                }
            }
        }

        stage ('Running Backend / API Tests') {
            when {expression {params.backend}}
            steps {
                script {
                    sh "mvn test -Ptags=backend"
                }
            }
            post {
                always {
                    allure([
                            includeProperties: false,
                            jdk: '',
                            reportBuildPolicy: 'ALWAYS',
                            results: [[path: 'target/allure-results']]
                    ])
                }
            }
        }

        stage ('Running Frontend / UI Tests') {
            when {expression {params.frontend}}
            steps {
                script {
                    sh "mvn test -Dbrowser=CHROME -Ptags=frontend"
                }
            }
            post {
                always {
                    allure([
                            includeProperties: false,
                            jdk: '',
                            reportBuildPolicy: 'ALWAYS',
                            results: [[path: 'target/allure-results']]
                    ])
                }
            }
        }
    }

    post {
        failure {
            script {
                echo "Send Email with test's Result"
                emailext to: '',
                        subject: "[MBM QA] Regression Test's №${BUILD_NUMBER} build",
                        body: """
                    <br><b>Test's Resut:</b><br>
                    <br>Build number: <b>${BUILD_NUMBER}</b>
                    <br>Build status: <b>${currentBuild.result}</b>
                    <br><b><a href="">Allure Report</a></b>
                    """,
                    mimeType: 'text/html'
            }
        }
    }
}

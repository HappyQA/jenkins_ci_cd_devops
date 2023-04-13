def prepareTestData() {
    sh(mongoPath + '/mongorestore --username $MONGODB_USR --password $MONGODB_PSW --authenticationDatabase admin ' +
            '--host=$HOST --port=$PORT --db=$DBNAME dump/ --objcheck --drop')
}

properties([[$class: 'RebuildSettings', autoRebuild: false, rebuildDisabled: false],
            parameters([
                    choice(choices: ['None', '', '', '',
                                     '', '', '', '', ''], defaultValue: 'None', description: 'Test Suite for testing Use Cases', name: 'TestSuiteUseCases'),
                    choice(choices: ['None', '', '', '',
                                     '', '', '', '', ''], defaultValue: 'None', description: 'Test Suite for testing Validation Cases', name: 'TestSuiteValidationCases'),
                    choice(choices: ['None', '', '', '',
                                     '', '', '', '', ''], defaultValue: 'None', description: 'Test Suite for testing User Access Cases', name: 'TestSuiteUserAccessCases'),
                    choice(choices: ['None', '', '', ''], defaultValue: 'None', description: 'Test Suite for testing Email Notification', name: 'TestSuiteEmailNotificationCases'),
                    choice(choices: ['None', '', '', ''], defaultValue: 'None', description: 'Test Suite for testing Chat Notification', name: 'TestSuiteChatNotificationCases'),
                    choice(choices: ['None', '', '', '',
                                     '', '', '', '', ''], defaultValue: 'None', description: 'Test Suite for testing API', name: 'TestSuiteAPI'),
                    text(defaultValue: '', description: 'Set Test Run Id from TestRail', name: 'TestRailRunId'),
                    booleanParam(defaultValue: true, description: '', name: 'PrepareTestData')
            ])
])

pipeline {

    agent {
        label 'slave&&jdk17'
    }

    environment {
        mongoPath = tool name: 'MongoTools-100.5.1', type: 'com.cloudbees.jenkins.plugins.customtools.CustomTool'
        MONGODB = credentials('')
    }

    stages {
        stage('Write credentials') {
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

        stage('Clear Test Artifacts') {
            steps {
                script {
                    sh "mvn clean"
                }
            }
        }

        stage('Prepare Test Data') {
            when { expression { params.PrepareTestData } }
            steps {
                script {
                    prepareTestData()
                }
            }
        }

        stage('Running Backend / API Tests') {
            steps {
                script {
                    if (params.TestSuiteAPI != "None") {
                        sh "mvn test -Dgroups=$TestSuiteAPI-API -DrunId=$TestRailRunId"
                    }
                }
            }
        }

        stage('Running Frontend / UI Tests') {
            steps {
                script {
                    if (params.TestSuiteUseCases != "None") {
                        echo "Running $TestSuiteUseCases Use Cases"
                        sh "mvn test -Dgroups=$TestSuiteUseCases-UseCases -DrunId=$TestRailRunId"
                    }
                    if (params.TestSuiteValidationCases != "None") {
                        echo "Running $TestSuiteValidationCases Validation Cases"
                        sh "mvn test -Dgroups=$TestSuiteValidationCases-ValidationCases -DrunId=$TestRailRunId"
                    }
                    if (params.TestSuiteUserAccessCases != "None") {
                        echo "Running $TestSuiteUserAccessCases User Access Cases"
                        sh "mvn test -Dgroups=$TestSuiteUserAccessCases-UserAccessCases -DrunId=$TestRailRunId"
                    }
                    if (params.TestSuiteEmailNotificationCases != "None") {
                        echo "Running $TestSuiteEmailNotificationCases Email Notification Cases"
                        sh "mvn test -Dgroups=$TestSuiteEmailNotificationCases-EmailNotificationCases -DrunId=$TestRailRunId"
                    }
                    if (params.TestSuiteChatNotificationCases != "None") {
                        echo "Running $TestSuiteChatNotificationCases Chat Notification Cases"
                        sh "mvn test -Dgroups=$TestSuiteChatNotificationCases-ChatNotificationCases -DrunId=$TestRailRunId"
                    }
                }
            }
        }
    }

    post {
        failure {
            emailext to: '',
                    subject: "[] Regression Test's №${BUILD_NUMBER} build",
                    body: """
                       <br><b>Test's Result:</b><br>
                       <br>Build number: <b>${BUILD_NUMBER}</b>
                       <br>Build status: <b>${currentBuild.result}</b>
                       <br><b><a href=\"https://testrail.host/index.php?/runs/view/${TestRailRunId}&group_by=cases:section_id&group_order=asc\">TestRail Run</a></b>
                       <br><b><a href=\"https://jenkins.host/job/${BUILD_NUMBER}/allure/\">Allure Report</a></b>
                       """,
                    mimeType: 'text/html'
        }
        always {
            allure includeProperties: false, jdk: 'jdk11', results: [[path: 'target/allure-results/']]
        }
    }
}

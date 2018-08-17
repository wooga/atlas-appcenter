#!groovy
@Library('github.com/wooga/atlas-jenkins-pipeline@0.0.3') _

pipeline {
    agent none

    stages {
        stage('Preparation') {
            agent any

            steps {
                sendSlackNotification "STARTED", true
            }
        }

        stage('check') {
            parallel {
                stage('macOS') {
                    agent {
                        label 'osx&&atlas&&secondary'
                    }

                    environment {
                        ATLAS_HOCKEY_INTEGRATION_API_TOKEN                  = credentials('atlas_hockey_integration_api_token')
                        ATLAS_HOCKEY_INTEGRATION_APPLICATION_IDENTIFIER     = credentials('atlas_hockey_integration_application_identifier')
                        COVERALLS_REPO_TOKEN                                = credentials('atlas_hockey_coveralls_token')
                        TRAVIS_JOB_NUMBER                                   = "${BUILD_NUMBER}.MACOS"
                    }

                    steps {
                        gradleWrapper "check"
                    }

                    post {
                        success {
                            gradleWrapper "jacocoTestReport coveralls"
                            publishHTML([
                                allowMissing: true,
                                alwaysLinkToLastBuild: true,
                                keepAll: true,
                                reportDir: 'build/reports/jacoco/test/html',
                                reportFiles: 'index.html',
                                reportName: 'Coverage',
                                reportTitles: ''
                            ])
                        }

                        always {
                            junit allowEmptyResults: true, testResults: 'build/test-results/**/*.xml'
                        }
                    }
                }
            }

            post {
                always {
                    sendSlackNotification currentBuild.result, true
                }
            }
        }
    }
}

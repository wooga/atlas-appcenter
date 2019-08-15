#!groovy
@Library('github.com/wooga/atlas-jenkins-pipeline@1.x') _

withCredentials([string(credentialsId: 'atlas_hockey_integration_api_token', variable: 'hockeyToken'),
                 string(credentialsId: 'atlas_hockey_integration_application_identifier', variable: 'hockeyAppId'),
                 string(credentialsId: 'atlas_hockey_coveralls_token', variable: 'coveralls_token')]) {

    def testEnvironment = [
                            "ATLAS_HOCKEY_INTEGRATION_API_TOKEN=${hockeyToken}",
                            "ATLAS_HOCKEY_INTEGRATION_APPLICATION_IDENTIFIER=${hockeyAppId}"
                          ]

    buildGradlePlugin plaforms: ['osx', 'windows', 'linux'], coverallsToken: coveralls_token, testEnvironment: testEnvironment
}

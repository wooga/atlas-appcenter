#!groovy
@Library('github.com/wooga/atlas-jenkins-pipeline@1.x') _

withCredentials([string(credentialsId: 'atlas_hockey_integration_api_token', variable: 'hockeyToken'),
                 string(credentialsId: 'atlas_hockey_integration_application_identifier', variable: 'hockeyAppId'),
                 string(credentialsId: 'atlas_hockey_integration_appcenter_token', variable: 'appcenterToken'),
                 string(credentialsId: 'atlas_hockey_integration_appcenter_application_identifier', variable: 'appcenterAppId'),
                 string(credentialsId: 'atlas_hockey_integration_appcenter_application_owner', variable: 'appcenterOwner'),
                 string(credentialsId: 'atlas_hockey_coveralls_token', variable: 'coveralls_token')
                 ]) {

    def testEnvironment = [
                            "ATLAS_HOCKEY_INTEGRATION_API_TOKEN=${hockeyToken}",
                            "ATLAS_HOCKEY_INTEGRATION_APPLICATION_IDENTIFIER=${hockeyAppId}",
                            "ATLAS_APP_CENTER_INTEGRATION_API_TOKEN=${appcenterToken}",
                            "ATLAS_APP_CENTER_OWNER=${appcenterOwner}",
                            "ATLAS_APP_CENTER_INTEGRATION_APPLICATION_IDENTIFIER=${appcenterAppId}"
                          ]

    buildGradlePlugin plaforms: ['osx', 'windows', 'linux'], coverallsToken: coveralls_token, testEnvironment: testEnvironment
}

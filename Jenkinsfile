#!groovy
@Library('github.com/wooga/atlas-jenkins-pipeline@1.x') _

withCredentials([string(credentialsId: 'atlas_appcenter_integration_token', variable: 'appcenterToken'),
                 string(credentialsId: 'atlas_appcenter_integration_application_identifier_ios_1', variable: 'appcenterAppIdIosOSX'),
                 string(credentialsId: 'atlas_appcenter_integration_application_identifier_ios_2', variable: 'appcenterAppIdIosWin'),
                 string(credentialsId: 'atlas_appcenter_integration_application_identifier_ios_3', variable: 'appcenterAppIdIosLinux'),

                 string(credentialsId: 'atlas_appcenter_integration_application_identifier_android_1', variable: 'appcenterAppIdAndroidOSX'),
                 string(credentialsId: 'atlas_appcenter_integration_application_identifier_android_2', variable: 'appcenterAppIdAndroidWin'),
                 string(credentialsId: 'atlas_appcenter_integration_application_identifier_android_3', variable: 'appcenterAppIdAndroidLinux'),

                 string(credentialsId: 'atlas_appcenter_integration_application_owner', variable: 'appcenterOwner'),
                 string(credentialsId: 'atlas_appcenter_coveralls_token', variable: 'coveralls_token'),
                 string(credentialsId: 'atlas_plugins_sonar_token', variable: 'sonar_token')
                 ]) {

    def testEnvironment = [
                            'osx':
                                [
                                        "ATLAS_APP_CENTER_INTEGRATION_API_TOKEN=${appcenterToken}",
                                        "ATLAS_APP_CENTER_OWNER=${appcenterOwner}",
                                        "ATLAS_APP_CENTER_INTEGRATION_APPLICATION_IDENTIFIER_IOS=${appcenterAppIdIosOSX}",
                                        "ATLAS_APP_CENTER_INTEGRATION_APPLICATION_IDENTIFIER_ANDROID=${appcenterAppIdAndroidOSX}"
                                ],
                            'windows':
                                [
                                        "ATLAS_APP_CENTER_INTEGRATION_API_TOKEN=${appcenterToken}",
                                        "ATLAS_APP_CENTER_OWNER=${appcenterOwner}",
                                        "ATLAS_APP_CENTER_INTEGRATION_APPLICATION_IDENTIFIER_IOS=${appcenterAppIdIosWin}",
                                        "ATLAS_APP_CENTER_INTEGRATION_APPLICATION_IDENTIFIER_ANDROID=${appcenterAppIdAndroidWin}"
                                ],
                            'linux':
                                [
                                        "ATLAS_APP_CENTER_INTEGRATION_API_TOKEN=${appcenterToken}",
                                        "ATLAS_APP_CENTER_OWNER=${appcenterOwner}",
                                        "ATLAS_APP_CENTER_INTEGRATION_APPLICATION_IDENTIFIER_IOS=${appcenterAppIdIosLinux}",
                                        "ATLAS_APP_CENTER_INTEGRATION_APPLICATION_IDENTIFIER_ANDROID=${appcenterAppIdAndroidLinux}"
                                ],
                          ]

    buildGradlePlugin plaforms: ['osx', 'windows', 'linux'], coverallsToken: coveralls_token, sonarToken: sonar_token, testEnvironment: testEnvironment
}

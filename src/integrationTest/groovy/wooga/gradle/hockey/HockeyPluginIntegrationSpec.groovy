/*
 * Copyright 2018 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *       http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package wooga.gradle.hockey

class HockeyPluginIntegrationSpec extends IntegrationSpec{

    // HockeyApp used for testing: https://rink.hockeyapp.net/manage/apps/814327
    static String apiToken              = System.env["ATLAS_HOCKEY_INTEGRATION_API_TOKEN"]
    static String applicationIdentifier = System.env["ATLAS_HOCKEY_INTEGRATION_APPLICATION_IDENTIFIER"]

    def setup() {
        buildFile << """
            ${applyPlugin(HockeyPlugin)}
            publishHockey {
                apiToken = "$apiToken"
                applicationIdentifier = "$applicationIdentifier"
            }
        """.stripIndent()
    }

    def "uploads dummy ipa to HockeyApp successfully"() {
        given: "a dummy ipa binary to upload"

        def testFile = getClass().getClassLoader().getResource("test.ipa").path
        buildFile << """
            publishHockey.binary = "$testFile"
        """.stripIndent()

        expect:
        runTasksSuccessfully("publishHockey")
    }

    def "writes json file with uploaded version meta data"() {
        given: "a dummy ipa binary to upload"

        def testFile = getClass().getClassLoader().getResource("test.ipa").path
        buildFile << """
            publishHockey.binary = "$testFile"
        """.stripIndent()

        and: "a future version meta file"
        def versionMeta = new File(projectDir,"build/tmp/publishHockey/${applicationIdentifier}.json")
        assert !versionMeta.exists()

        when:
        runTasksSuccessfully("publishHockey")

        then:
        versionMeta.exists()
    }

    def "can access version model after a successfull publish"() {
        given: "a dummy ipa binary to upload"

        def testFile = getClass().getClassLoader().getResource("test.ipa").path
        buildFile << """
            publishHockey.binary = "$testFile"
        """.stripIndent()

        and: "a task that depends on publishHockey"
        buildFile << """
            task workAfterPublish {
                dependsOn publishHockey

                doLast {
                    def appVersion = publishHockey.appVersion
                    println("published app: " + appVersion.title)
                }
            }
        """.stripIndent()

        when:
        def result = runTasksSuccessfully("workAfterPublish")

        then:
        result.wasExecuted("publishHockey")
        outputContains(result, "published app: WoogaUnityUnifiedBuildSystemTest")
    }

    def "uploading invalid ipa to HockeyApp fails"() {
        given: "a generated invalid file"

        def emptyFile = createFile("test.ipa").path

        buildFile << """
            publishHockey.binary = "$emptyFile"
        """.stripIndent()

        expect:
        runTasksWithFailure("publishHockey")
    }
}

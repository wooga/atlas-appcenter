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

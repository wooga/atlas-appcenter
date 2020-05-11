/*
 * Copyright 2019 Wooga GmbH
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
 *
 */

package wooga.gradle.appcenter.tasks

import groovy.json.JsonOutput
import groovy.json.JsonSlurper
import org.apache.http.HttpResponse
import org.apache.http.client.HttpClient
import org.apache.http.client.methods.HttpGet
import org.apache.http.client.methods.HttpPost
import org.apache.http.client.methods.HttpDelete
import org.apache.http.entity.ContentType
import org.apache.http.entity.StringEntity
import org.apache.http.impl.client.HttpClientBuilder
import spock.lang.Unroll
import wooga.gradle.appcenter.AppCenterPlugin
import wooga.gradle.appcenter.IntegrationSpec

class AppCenterUploadTaskIntegrationSpec extends IntegrationSpec {
    static String apiToken = System.env["ATLAS_APP_CENTER_INTEGRATION_API_TOKEN"]
    static String owner = System.env["ATLAS_APP_CENTER_OWNER"]
    static String applicationIdentifier = System.env["ATLAS_APP_CENTER_INTEGRATION_APPLICATION_IDENTIFIER"]

    def setup() {
        buildFile << """
            version = "0.1.0"
            ${applyPlugin(AppCenterPlugin)}
            publishAppCenter {
                owner = "$owner"
                apiToken = "$apiToken"
                applicationIdentifier = "$applicationIdentifier"
            }
        """.stripIndent()
    }

    def "uploads dummy ipa to AppCenter successfully"() {
        given: "a dummy ipa binary to upload"

        "true".toBoolean()

        def testFile = getClass().getClassLoader().getResource("test.ipa").path
        buildFile << """
            publishAppCenter.binary = "$testFile"
        """.stripIndent()

        expect:
        runTasksSuccessfully("publishAppCenter")
    }

    def "writes json file with uploaded version meta data"() {
        given: "a dummy ipa binary to upload"
        def testFile = getClass().getClassLoader().getResource("test.ipa").path
        buildFile << """
            publishAppCenter.binary = "$testFile"
        """.stripIndent()

        and: "a future version meta file"
        def versionMeta = new File(projectDir, "build/tmp/publishAppCenter/${owner}_${applicationIdentifier}.json")
        assert !versionMeta.exists()

        when:
        runTasksSuccessfully("publishAppCenter")

        then:
        versionMeta.exists()
    }

    def "publishes to Collaborators group when no groups are configured"() {
        given: "a dummy ipa binary to upload"
        def testFile = getClass().getClassLoader().getResource("test.ipa").path
        buildFile << """
            publishAppCenter.binary = "$testFile"
        """.stripIndent()

        and: "a future version meta file"
        def versionMeta = new File(projectDir, "build/tmp/publishAppCenter/${owner}_${applicationIdentifier}.json")
        assert !versionMeta.exists()

        and: "no configured distribution groups"

        when:
        runTasksSuccessfully("publishAppCenter")

        then:
        def release = getRelease(versionMeta)
        def destinations = release["destinations"]
        destinations.any { it["name"] == "Collaborators" }
    }

    def "can publish to custom distribution groups"() {
        given: "a new distribution group"
        ensureDistributionGroup("Test")
        ensureDistributionGroup("Test2")

        buildFile << """
            publishAppCenter.destinations = ["Test", "Test2"]
        """.stripIndent()

        and: "a dummy ipa binary to upload"
        def testFile = getClass().getClassLoader().getResource("test.ipa").path
        buildFile << """
            publishAppCenter.binary = "$testFile"
        """.stripIndent()

        and: "a future version meta file"
        def versionMeta = new File(projectDir, "build/tmp/publishAppCenter/${owner}_${applicationIdentifier}.json")
        assert !versionMeta.exists()

        when:
        runTasksSuccessfully("publishAppCenter")

        then:
        def release = getRelease(versionMeta)
        def destinations = release["destinations"]
        destinations.any { it["name"] == "Test" || it["name"] == "Test2" }
        !destinations.any { it["name"] == "Collaborators" }
    }

    def "can publish to custom distribution group in addition to default groups"() {
        given: "a new distribution group"
        ensureDistributionGroup("Test")
        ensureDistributionGroup("Test2")

        buildFile << """
            publishAppCenter.destination "Test"
            publishAppCenter.destination "Test2"
        """.stripIndent()

        and: "a dummy ipa binary to upload"
        def testFile = getClass().getClassLoader().getResource("test.ipa").path
        buildFile << """
            publishAppCenter.binary = "$testFile"
        """.stripIndent()

        and: "a future version meta file"
        def versionMeta = new File(projectDir, "build/tmp/publishAppCenter/${owner}_${applicationIdentifier}.json")
        assert !versionMeta.exists()

        when:
        runTasksSuccessfully("publishAppCenter")

        then:
        def release = getRelease(versionMeta)
        def destinations = release["destinations"]
        destinations.any { it["name"] == "Test" || it["name"] == "Test2" || it["name"] == "Collaborators" }
    }

    def "fails when distribution group is invalid"() {
        given: "a publish task with invalid distribution groups"
        buildFile << """
            publishAppCenter.destination "some value", "some other group"
        """.stripIndent()

        and: "a dummy ipa binary to upload"
        def testFile = getClass().getClassLoader().getResource("test.ipa").path
        buildFile << """
            publishAppCenter.binary = "$testFile"
        """.stripIndent()

        expect:
        runTasksWithFailure("publishAppCenter")
    }

    def "can publish custom build infos"() {
        given: "publish task with build infos"
        buildFile << """
            publishAppCenter.buildInfo {
                branchName = "master"
                commitHash = "000000000000"
                commitMessage = "Fix tests"
            }
        """.stripIndent()

        and: "a dummy ipa binary to upload"
        def testFile = getClass().getClassLoader().getResource("test.ipa").path
        buildFile << """
            publishAppCenter.binary = "$testFile"
        """.stripIndent()

        and: "a future version meta file"
        def versionMeta = new File(projectDir, "build/tmp/publishAppCenter/${owner}_${applicationIdentifier}.json")
        assert !versionMeta.exists()

        when:
        runTasksSuccessfully("publishAppCenter")

        then:
        versionMeta.exists()
        def jsonSlurper = new JsonSlurper()
        def releaseMeta = jsonSlurper.parse(versionMeta)

        def releaseId = releaseMeta["release_id"]
        def release = getRelease(releaseId)

        def buildInfo = release["build"]
        buildInfo["branch_name"] == "master"
        buildInfo["commit_hash"] == "000000000000"
        buildInfo["commit_message"] == "Fix tests"
    }

    def "can publish release notes"() {
        given: "publish task with release notes text"
        buildFile << """
            publishAppCenter.releaseNotes = "${releaseNotes}"
        """.stripIndent()

        and: "a dummy ipa binary to upload"
        def testFile = getClass().getClassLoader().getResource("test.ipa").path
        buildFile << """
            publishAppCenter.binary = "$testFile"
        """.stripIndent()

        and: "a future version meta file"
        def versionMeta = new File(projectDir, "build/tmp/publishAppCenter/${owner}_${applicationIdentifier}.json")
        assert !versionMeta.exists()

        when:
        runTasksSuccessfully("publishAppCenter")

        then:
        versionMeta.exists()
        def jsonSlurper = new JsonSlurper()
        def releaseMeta = jsonSlurper.parse(versionMeta)

        def releaseId = releaseMeta["release_id"]
        def release = getRelease(releaseId)
        release['release_notes'] == releaseNotes

        where:
        releaseNotes = "publish new version"
    }

    @Unroll()
    def "can add publish destinations with :#method and type '#type'"() {
        given: "some configured property"
        buildFile << "publishAppCenter.${method}(${value})"

        and: "available groups"
        expectedGroups.each {
            if (it != "Collaborators") {
                ensureDistributionGroup(it)
            }
        }

        and: "a dummy ipa binary to upload"
        def testFile = getClass().getClassLoader().getResource("test.ipa").path
        buildFile << """
            publishAppCenter.binary = "$testFile"
        """.stripIndent()

        and: "a future version meta file"
        def versionMeta = new File(projectDir, "build/tmp/publishAppCenter/${owner}_${applicationIdentifier}.json")
        assert !versionMeta.exists()

        when:
        runTasksSuccessfully("publishAppCenter")

        then:
        def release = getRelease(versionMeta)
        def destinations = release["destinations"] as List<Map>
        expectedGroups.every { group ->
            destinations.find { it["name"] == group }
        }

        where:
        property       | method            | rawValue           | type           | expectedGroups
        "destinations" | "destination"     | ["Test1"]          | "List<String>" | ["Collaborators", "Test1"]
        "destinations" | "destination"     | "Test1"            | "String"       | ["Collaborators", "Test1"]
        "destinations" | "destination"     | ["Test1", "Test2"] | "List<String>" | ["Collaborators", "Test1", "Test2"]
        "destinations" | "destination"     | ["Test1", "Test2"] | "String..."    | ["Collaborators", "Test1", "Test2"]
        //"destinations" | "destinationId"   | "groupId"            | "String"       | [[name: "Collaborators"], [id: "groupId"]]
        "destinations" | "setDestinations" | ["Test1", "Test2"] | "List<String>" | ["Test1", "Test2"]
        value = wrapValueBasedOnType(rawValue, type)
    }

    def "can add publish destinations with :destinationId"() {
        given: "a new group"
        def group = ensureDistributionGroup("TestGroupFromId")

        and: "configuring destination group by id"
        buildFile << "publishAppCenter.destinationId('${group['id']}')"

        and: "a dummy ipa binary to upload"
        def testFile = getClass().getClassLoader().getResource("test.ipa").path
        buildFile << """
            publishAppCenter.binary = "$testFile"
        """.stripIndent()

        and: "a future version meta file"
        def versionMeta = new File(projectDir, "build/tmp/publishAppCenter/${owner}_${applicationIdentifier}.json")
        assert !versionMeta.exists()

        when:
        runTasksSuccessfully("publishAppCenter")

        then:
        def release = getRelease(versionMeta)
        def destinations = release["destinations"] as List<Map>
        destinations.any {
            it["id"] == group["id"] && it["name"] == group["name"]
        }
    }


    void deleteDistributionGroup(String name) {
        HttpClient client = HttpClientBuilder.create().build()
        HttpDelete request = new HttpDelete("https://api.appcenter.ms/v0.1/apps/${owner}/${applicationIdentifier}/distribution_groups/${URLEncoder.encode(name, "UTF-8")}")

        request.setHeader("Accept", 'application/json')
        request.setHeader("X-API-Token", apiToken)

        HttpResponse response = client.execute(request)

        if (response.statusLine.statusCode != 204) {
            throw new Exception("Failed to delete distribution group")
        }
    }

    Map<String, String> ensureDistributionGroup(String name) {
        HttpClient client = HttpClientBuilder.create().build()
        HttpPost request = new HttpPost("https://api.appcenter.ms/v0.1/apps/${owner}/${applicationIdentifier}/distribution_groups")

        request.setHeader("Accept", 'application/json')
        request.setHeader("X-API-Token", apiToken)

        def body = ["name": name]
        request.setEntity(new StringEntity(JsonOutput.toJson(body), ContentType.APPLICATION_JSON))

        HttpResponse response = client.execute(request)

        if (response.statusLine.statusCode != 201 && response.statusLine.statusCode != 409) {
            throw new Exception("Failed to create distribution group")
        } else if (response.statusLine.statusCode == 409) {
            return loadDistributionGroup(name)
        }

        def jsonSlurper = new JsonSlurper()
        jsonSlurper.parseText(response.entity.content.text) as Map
    }

    Map<String, String> loadDistributionGroup(String name) {
        HttpClient client = HttpClientBuilder.create().build()
        HttpGet request = new HttpGet("https://api.appcenter.ms/v0.1/apps/${owner}/${applicationIdentifier}/distribution_groups/${name}")

        request.setHeader("Accept", 'application/json')
        request.setHeader("X-API-Token", apiToken)

        HttpResponse response = client.execute(request)

        if (response.statusLine.statusCode != 200) {
            throw new Exception("Failed to load distribution group")
        }

        def jsonSlurper = new JsonSlurper()
        jsonSlurper.parseText(response.entity.content.text) as Map
    }

    Map getRelease(File versionMeta) {
        def jsonSlurper = new JsonSlurper()
        def releaseMeta = jsonSlurper.parse(versionMeta)

        String releaseId = releaseMeta["release_id"]
        getRelease(releaseId)
    }

    Map getRelease(String releaseId) {
        HttpClient client = HttpClientBuilder.create().build()
        HttpGet request = new HttpGet("https://api.appcenter.ms/v0.1/apps/${owner}/${applicationIdentifier}/releases/${releaseId}")

        request.setHeader("Accept", 'application/json')
        request.setHeader("X-API-Token", apiToken)

        HttpResponse response = client.execute(request)
        def jsonSlurper = new JsonSlurper()
        jsonSlurper.parseText(response.entity.content.text) as Map
    }
}

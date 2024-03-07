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
import org.apache.http.client.methods.HttpDelete
import org.apache.http.client.methods.HttpGet
import org.apache.http.client.methods.HttpPost
import org.apache.http.entity.ContentType
import org.apache.http.entity.StringEntity
import org.apache.http.impl.client.HttpClientBuilder
import spock.lang.Unroll
import wooga.gradle.appcenter.AppCenterPlugin
import wooga.gradle.appcenter.IntegrationSpec

import static com.wooga.gradle.PlatformUtils.escapedPath

class AppCenterUploadTaskIntegrationSpec extends IntegrationSpec {
    static String apiToken = System.env["ATLAS_APP_CENTER_INTEGRATION_API_TOKEN"]
    static String owner = System.env["ATLAS_APP_CENTER_OWNER"]
    static String applicationIdentifierIos = System.env["ATLAS_APP_CENTER_INTEGRATION_APPLICATION_IDENTIFIER_IOS"]
    static String applicationIdentifierAndroid = System.env["ATLAS_APP_CENTER_INTEGRATION_APPLICATION_IDENTIFIER_ANDROID"]

    def setup() {
        buildFile << """
            version = "0.1.0"
            ${applyPlugin(AppCenterPlugin)}
            publishAppCenter.configure { t ->
                t.owner = "$owner"
                t.apiToken = "$apiToken"
                t.applicationIdentifier = "$applicationIdentifierIos"
            }
        """.stripIndent()
    }

    void writeRandomData(File destination, Long fileSize) {
        def random = new Random()
        def chunkSize = 1024 * 1024 * 4
        def chunksToWrite = fileSize / chunkSize

        destination.withDataOutputStream {
            for (int i = 0; i < chunksToWrite; i++) {
                def chunk = new byte[chunkSize]
                random.nextBytes(chunk)
                it.write(chunk)
            }
        }
    }

    File createBigUploadBinary(File baseBinary, File destinationDir, Long fileSize) {
        def output = new File(destinationDir, baseBinary.name)
        def packagePayloadDir = File.createTempDir(baseBinary.name, "payload")

        def ant = new AntBuilder()
        ant.unzip(src: baseBinary,
                dest: packagePayloadDir.path,
                overwrite: "false")

        writeRandomData(new File(packagePayloadDir, "test.bin"), fileSize - baseBinary.size())

        ant.zip(destfile: output.path, basedir: packagePayloadDir.path)

        output
    }

    @Unroll("uploads big dummy #fileType to AppCenter successfully")
    def "uploads big artifacts"() {
        given: "a dummy ipa binary increased in filesize to upload"
        def testFile = new File(getClass().getClassLoader().getResource(fileName).path)
        testFile = createBigUploadBinary(testFile, File.createTempDir("testUpload", "fileType"), 1024 * 1024 * desiredFileSize)

        buildFile << """
            publishAppCenter.configure {
                binary.set(file("${escapedPath(testFile.path)}"))
                applicationIdentifier.set("$applicationIdentifier")
            }
        """.stripIndent()

        expect:
        runTasksSuccessfully("publishAppCenter")

        where:
        fileType | fileName   | applicationIdentifier        | desiredFileSize
        "ipa"    | "test.ipa" | applicationIdentifierIos     | 160
        "apk"    | "test.apk" | applicationIdentifierAndroid | 160
    }

    @Unroll("uploads small dummy #fileType to AppCenter successfully")
    def "uploads artifacts"() {
        given: "a dummy ipa binary to upload"
        def testFile = getClass().getClassLoader().getResource(fileName).path
        buildFile << """
            publishAppCenter.configure {
                binary.set(file("$testFile"))
                applicationIdentifier.set("$applicationIdentifier")
            }
        """.stripIndent()

        expect:
        runTasksSuccessfully("publishAppCenter")

        where:
        fileType | fileName   | applicationIdentifier
        "ipa"    | "test.ipa" | applicationIdentifierIos
        "apk"    | "test.apk" | applicationIdentifierAndroid
    }

    def "writes json file with uploaded version meta data"() {
        given: "a dummy ipa binary to upload"
        def testFile = getClass().getClassLoader().getResource("test.ipa").path
        buildFile << """
            publishAppCenter.configure {
                binary.set(file("$testFile"))
            }
        """.stripIndent()

        and: "a future version meta file"
        def versionMeta = new File(projectDir, "build/tmp/publishAppCenter/${owner}_${applicationIdentifierIos}.json")
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
            publishAppCenter.configure {
                binary.set(file("$testFile"))
            }
        """.stripIndent()

        and: "a future version meta file"
        def versionMeta = new File(projectDir, "build/tmp/publishAppCenter/${owner}_${applicationIdentifierIos}.json")
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
            publishAppCenter.configure {
                destinations = ["Test", "Test2"]
            }
        """.stripIndent()

        and: "a dummy ipa binary to upload"
        def testFile = getClass().getClassLoader().getResource("test.ipa").path
        buildFile << """
            publishAppCenter.configure {
                binary.set(file("$testFile"))
            }
        """.stripIndent()

        and: "a future version meta file"
        def versionMeta = new File(projectDir, "build/tmp/publishAppCenter/${owner}_${applicationIdentifierIos}.json")
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
            publishAppCenter.configure {
                destination "Test"
                destination "Test2"
            }
        """.stripIndent()

        and: "a dummy ipa binary to upload"
        def testFile = getClass().getClassLoader().getResource("test.ipa").path
        buildFile << """
            publishAppCenter.configure { 
                binary.set(file("$testFile"))
            }
        """.stripIndent()

        and: "a future version meta file"
        def versionMeta = new File(projectDir, "build/tmp/publishAppCenter/${owner}_${applicationIdentifierIos}.json")
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
            publishAppCenter.configure {
                destination "some value", "some other group"
            }
        """.stripIndent()

        and: "a dummy ipa binary to upload"
        def testFile = getClass().getClassLoader().getResource("test.ipa").path
        buildFile << """
            publishAppCenter.configure {
                binary = "$testFile"
            }
        """.stripIndent()

        expect:
        runTasksWithFailure("publishAppCenter")
    }

    def "can publish custom build infos"() {
        given: "publish task with build infos"
        buildFile << """
            publishAppCenter.configure {
                buildInfo {
                    branchName = "master"
                    commitHash = "000000000000"
                    commitMessage = "Fix tests"
                }
            }
        """.stripIndent()

        and: "a dummy ipa binary to upload"
        def testFile = getClass().getClassLoader().getResource("test.ipa").path
        buildFile << """
            publishAppCenter.configure {
                binary.set(file("$testFile"))
            }
        """.stripIndent()

        and: "a future version meta file"
        def versionMeta = new File(projectDir, "build/tmp/publishAppCenter/${owner}_${applicationIdentifierIos}.json")
        assert !versionMeta.exists()

        when:
        runTasksSuccessfully("publishAppCenter")

        then:
        versionMeta.exists()
        def jsonSlurper = new JsonSlurper()
        def releaseMeta = jsonSlurper.parse(versionMeta)

        String releaseId = releaseMeta["id"]
        def release = getRelease(releaseId)

        def buildInfo = release["build"]
        buildInfo["branch_name"] == "master"
        buildInfo["commit_hash"] == "000000000000"
        buildInfo["commit_message"] == "Fix tests"
    }

    def "can publish release notes"() {
        given: "publish task with release notes text"
        buildFile << """
            publishAppCenter.configure { releaseNotes = "${releaseNotes}" }
        """.stripIndent()

        and: "a dummy ipa binary to upload"
        def testFile = getClass().getClassLoader().getResource("test.ipa").path
        buildFile << """
            publishAppCenter.configure { binary.set(file("$testFile")) }
        """.stripIndent()

        and: "a future version meta file"
        def versionMeta = new File(projectDir, "build/tmp/publishAppCenter/${owner}_${applicationIdentifierIos}.json")
        assert !versionMeta.exists()

        when:
        runTasksSuccessfully("publishAppCenter")

        then:
        versionMeta.exists()
        def jsonSlurper = new JsonSlurper()
        def releaseMeta = jsonSlurper.parse(versionMeta)

        String releaseId = releaseMeta["id"]
        def release = getRelease(releaseId)
        release['release_notes'] == releaseNotes

        where:
        releaseNotes = "publish new version"
    }

    @Unroll()
    def "can add publish destinations with :#method and type '#type'"() {
        given: "some configured property"
        buildFile << "publishAppCenter.configure { ${method}(${value}) }"

        and: "available groups"
        expectedGroups.each {
            if (it != "Collaborators") {
                ensureDistributionGroup(it)
            }
        }

        and: "a dummy ipa binary to upload"
        def testFile = getClass().getClassLoader().getResource("test.ipa").path
        buildFile << """
            publishAppCenter.configure { binary.set(file("$testFile")) }
        """.stripIndent()

        and: "a future version meta file"
        def versionMeta = new File(projectDir, "build/tmp/publishAppCenter/${owner}_${applicationIdentifierIos}.json")
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
        "destinations" | "setDestinations" | ["Test1", "Test2"] | "List<String>" | ["Test1", "Test2"]
        "destinations" | "setDestinations" | ["Test1", "Test2"] | "String..."    | ["Test1", "Test2"]
        value = wrapValueBasedOnType(rawValue, type)
    }

    @Unroll
    def "can set property :#property with :#method and type '#type'"() {
        given: "a task to print publishAppCenter properties"
        buildFile << """
            tasks.register("custom") {
                doLast {
                    def value = publishAppCenter.${property}.get()
                    println("publishAppCenter.${property}: " + value)
                }
            }
        """

        and: "the test value with replace placeholders"
        if (value instanceof String) {
            value = value.replaceAll("#projectDir#", escapedPath(projectDir.path))
        }

        and: "some configured property"
        buildFile << "publishAppCenter.configure { t -> t.${method}(${value}) }"

        when: ""
        def result = runTasksSuccessfully("custom")

        then:
        if (property == "binary") {
            rawValue = new File(rawValue.replaceAll("#projectDir#", escapedPath(projectDir.path))).path
        }

        result.standardOutput.contains("publishAppCenter.${property}: ${rawValue}")

        where:
        property                | method                      | rawValue                     | type
        "apiToken"              | "apiToken.set"              | "testToken2"                 | "String"
        "apiToken"              | "apiToken.set"              | "testToken3"                 | "Provider<String>"
        "apiToken"              | "setApiToken"               | "testToken4"                 | "String"
        "apiToken"              | "setApiToken"               | "testToken5"                 | "Provider<String>"
        "owner"                 | "owner.set"                 | "owner2"                     | "String"
        "owner"                 | "owner.set"                 | "owner3"                     | "Provider<String>"
        "owner"                 | "setOwner"                  | "owner4"                     | "String"
        "owner"                 | "setOwner"                  | "owner5"                     | "Provider<String>"
        "buildVersion"          | "buildVersion.set"          | "buildVersion2"              | "String"
        "buildVersion"          | "buildVersion.set"          | "buildVersion3"              | "Provider<String>"
        "buildVersion"          | "setBuildVersion"           | "buildVersion4"              | "String"
        "buildVersion"          | "setBuildVersion"           | "buildVersion5"              | "Provider<String>"
        "applicationIdentifier" | "applicationIdentifier.set" | "applicationIdentifier2"     | "String"
        "applicationIdentifier" | "applicationIdentifier.set" | "applicationIdentifier3"     | "Provider<String>"
        "applicationIdentifier" | "setApplicationIdentifier"  | "applicationIdentifier4"     | "String"
        "applicationIdentifier" | "setApplicationIdentifier"  | "applicationIdentifier5"     | "Provider<String>"
        "releaseNotes"          | "releaseNotes.set"          | "releaseNotes2"              | "String"
        "releaseNotes"          | "releaseNotes.set"          | "releaseNotes3"              | "Provider<String>"
        "releaseNotes"          | "setReleaseNotes"           | "releaseNotes4"              | "String"
        "releaseNotes"          | "setReleaseNotes"           | "releaseNotes5"              | "Provider<String>"
        "binary"                | "binary.set"                | "#projectDir#/some/binary/3" | "File"
        "binary"                | "binary.set"                | "#projectDir#/some/binary/4" | "Provider<RegularFile>"
        "binary"                | "setBinary"                 | "#projectDir#/some/binary/6" | "File"
        "binary"                | "setBinary"                 | "#projectDir#/some/binary/7" | "Provider<RegularFile>"

        "retryCount"            | "retryCount.set"            | 2                            | "Integer"
        "retryCount"            | "retryCount.set"            | 3                            | "Provider<Integer>"
        "retryCount"            | "setRetryCount"             | 4                            | "Integer"

        "retryTimeout"          | "retryTimeout.set"          | 2000L                        | "Long"
        "retryTimeout"          | "retryTimeout.set"          | 3000L                        | "Provider<Long>"
        "retryTimeout"          | "setRetryTimeout"           | 4000L                        | "Long"
        value = wrapValueBasedOnType(rawValue, type)
    }

    def "can add publish destinations with :destinationId"() {
        given: "a new group"
        def group = ensureDistributionGroup("TestGroupFromId")

        and: "configuring destination group by id"
        buildFile << "publishAppCenter.configure { destinationId('${group['id']}') }"

        and: "a dummy ipa binary to upload"
        def testFile = getClass().getClassLoader().getResource("test.ipa").path
        buildFile << """
        publishAppCenter.configure{ binary.set(file("$testFile")) }
        """.stripIndent()

        and: "a future version meta file"
        def versionMeta = new File(projectDir, "build/tmp/publishAppCenter/${owner}_${applicationIdentifierIos}.json")
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
        HttpDelete request = new HttpDelete("https://api.appcenter.ms/v0.1/apps/${owner}/${applicationIdentifierIos}/distribution_groups/${URLEncoder.encode(name, "UTF-8")}")

        request.setHeader("Accept", 'application/json')
        request.setHeader("X-API-Token", apiToken)

        HttpResponse response = client.execute(request)

        if (response.statusLine.statusCode != 204) {
            throw new Exception("Failed to delete distribution group")
        }
    }

    Map<String, String> ensureDistributionGroup(String name) {
        HttpClient client = HttpClientBuilder.create().build()
        HttpPost request = new HttpPost("https://api.appcenter.ms/v0.1/apps/${owner}/${applicationIdentifierIos}/distribution_groups")

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
        HttpGet request = new HttpGet("https://api.appcenter.ms/v0.1/apps/${owner}/${applicationIdentifierIos}/distribution_groups/${name}")

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

        String releaseId = releaseMeta["id"]
        getRelease(releaseId)
    }

    Map getRelease(String releaseId) {
        HttpClient client = HttpClientBuilder.create().build()
        HttpGet request = new HttpGet("https://api.appcenter.ms/v0.1/apps/${owner}/${applicationIdentifierIos}/releases/${releaseId}")

        request.setHeader("Accept", 'application/json')
        request.setHeader("X-API-Token", apiToken)

        HttpResponse response = client.execute(request)
        def jsonSlurper = new JsonSlurper()
        jsonSlurper.parseText(response.entity.content.text) as Map
    }
}

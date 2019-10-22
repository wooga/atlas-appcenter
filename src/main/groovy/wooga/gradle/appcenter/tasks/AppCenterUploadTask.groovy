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
import org.apache.http.HttpEntity
import org.apache.http.HttpResponse
import org.apache.http.client.HttpClient
import org.apache.http.client.methods.HttpPatch
import org.apache.http.client.methods.HttpPost
import org.apache.http.entity.ContentType
import org.apache.http.entity.StringEntity
import org.apache.http.entity.mime.MultipartEntityBuilder
import org.apache.http.entity.mime.content.FileBody
import org.apache.http.impl.client.HttpClientBuilder
import org.gradle.api.Action
import org.gradle.api.GradleException
import org.gradle.api.file.FileCollection
import org.gradle.api.internal.ConventionTask
import org.gradle.api.tasks.*
import org.gradle.internal.impldep.com.google.gson.JsonObject
import wooga.gradle.appcenter.api.AppCenterBuildInfo
import static org.gradle.util.ConfigureUtil.configureUsing

import java.util.concurrent.Callable

class AppCenterUploadTask extends ConventionTask {

    private Object apiToken

    @Input
    String getApiToken() {
        convertToString(apiToken)
    }

    void setApiToken(Object value) {
        apiToken = value
    }

    AppCenterUploadTask apiToken(Object apiToken) {
        setApiToken(apiToken)
        this
    }

    private Object owner

    @Input
    String getOwner() {
        convertToString(owner)
    }

    void setOwner(Object value) {
        owner = value
    }

    AppCenterUploadTask owner(Object owner) {
        setOwner(owner)
        this
    }

    private Object buildVersion

    @Input
    String getBuildVersion() {
        convertToString(buildVersion)
    }

    void setBuildVersion(Object value) {
        buildVersion = value
    }

    AppCenterUploadTask buildVersion(Object buildVersion) {
        setBuildVersion(buildVersion)
        this
    }

    private Object releaseId

    @Optional
    @Input
    int getReleaseId() {
        if (!releaseId) {
            return 0
        }

        Integer.parseInt(convertToString(releaseId))
    }

    void setReleaseId(Object value) {
        releaseId = value
    }

    AppCenterUploadTask releaseId(Object releaseId) {
        setReleaseId(releaseId)
        this
    }

    private Object applicationIdentifier

    @Input
    String getApplicationIdentifier() {
        convertToString(applicationIdentifier)
    }

    void setApplicationIdentifier(Object value) {
        applicationIdentifier = value
    }

    AppCenterUploadTask applicationIdentifier(Object applicationIdentifier) {
        setApplicationIdentifier(applicationIdentifier)
        this
    }

    private List<Map<String,String>> destinations = new ArrayList<>()

    @Input
    protected List<Map<String,String>> getDestinations() {
        destinations
    }

    void destination(String name) {
        destinations.add(["name": name])
    }

    void destination(Iterable<String> destinations) {
        this.destinations.addAll(destinations.collect {["name": it]})
    }

    void destination(String... destinations) {
        this.destinations.addAll(destinations.collect {["name": it]})
    }

    void destinationId(String id) {
        destinations.add(["id": id])
    }

    private AppCenterBuildInfo buildInfo = new AppCenterBuildInfo()

    @Nested
    protected AppCenterBuildInfo getBuildInfo() {
        buildInfo
    }

    void buildInfo(Closure closure) {
        buildInfo(configureUsing(closure))
    }

    void buildInfo(Action<? super AppCenterBuildInfo> action) {
        action.execute(buildInfo)
    }

    private Object binary

    @SkipWhenEmpty
    @InputFiles
    protected FileCollection getInputFiles() {
        if (!binary) {
            return project.files()
        }
        return project.files(binary)
    }

    File getBinary() {

        def files = getInputFiles()
        if (files.size() > 0) {
            return files.getSingleFile()
        }
        return null
    }

    void setBinary(Object value) {
        binary = value
    }

    AppCenterUploadTask binary(Object binary) {
        setBinary(binary)
        this
    }

    @OutputFiles
    protected FileCollection getOutputFiles() {
        return project.files(getUploadVersionMetaData())
    }

    File getUploadVersionMetaData() {
        new File(temporaryDir, "${getOwner()}_${getApplicationIdentifier()}.json")
    }

    private static Map createUploadResource(HttpClient client, String owner, String applicationIdentifier, String apiToken, String buildVersion, int releaseId = 0) {
        // curl -X POST --header 'Content-Type: application/json' --header 'Accept: application/json'
        //      --header 'X-API-Token: xxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxx'
        //      'https://api.appcenter.ms/v0.1/apps/JoshuaWeber/APIExample/release_uploads'

        def uri = "https://api.appcenter.ms/v0.1/apps/${owner}/${applicationIdentifier}/release_uploads"
        HttpPost post = new HttpPost(uri)
        post.setHeader("Accept", 'application/json')
        post.setHeader("X-API-Token", apiToken)


        def body = ["build_version": buildVersion, "release_id": releaseId]

        post.setEntity(new StringEntity(JsonOutput.toJson(body), ContentType.APPLICATION_JSON))

        HttpResponse response = client.execute(post)
        if (response.statusLine.statusCode != 201) {
            throw new GradleException("unable to create upload resource for ${owner}/${applicationIdentifier}")
        }

        def jsonSlurper = new JsonSlurper()
        jsonSlurper.parseText(response.entity.content.text) as Map
    }

    private static uploadResources(HttpClient client, String apiToken, String uploadUrl, File binary) {
        HttpPost post = new HttpPost(uploadUrl)
        FileBody ipa = new FileBody(binary)
        post.setHeader("X-API-Token", apiToken)
        HttpEntity content = MultipartEntityBuilder.create()
                .addPart("ipa", ipa)
                .build()

        post.setEntity(content)
        HttpResponse response = client.execute(post)

        if (response.statusLine.statusCode != 204) {
            throw new GradleException("unable to upload to provided upload url" + response.statusLine.toString())
        }
    }

    private static Map commitResource(HttpClient client, String owner, String applicationIdentifier, String apiToken, String uploadId) {
        // curl -X PATCH --header 'Content-Type: application/json'
        //               --header 'Accept: application/json'
        //               --header 'X-API-Token: xxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxx' -d '{ "status": "committed"  }'
        //               'https://api.appcenter.ms/v0.1/apps/JoshuaWeber/APITesting/release_uploads/c18df340-069f-0135-3290-22000b559634'
        // https://openapi.appcenter.ms/#/distribute/releaseUploads_complete

        def uri = "https://api.appcenter.ms/v0.1/apps/${owner}/${applicationIdentifier}/release_uploads/${uploadId}"
        HttpPatch patch = new HttpPatch(uri)
        patch.setHeader("Accept", 'application/json')
        patch.setHeader("X-API-Token", apiToken)

        def body = ["status": "committed"]
        patch.setEntity(new StringEntity(JsonOutput.toJson(body), ContentType.APPLICATION_JSON))

        HttpResponse response = client.execute(patch)
        if (response.statusLine.statusCode != 200) {
            throw new GradleException("unable to commit upload resource ${uploadId} for ${owner}/${applicationIdentifier}")
        }

        def jsonSlurper = new JsonSlurper()
        jsonSlurper.parseText(response.entity.content.text) as Map
    }

    private static void distribute(HttpClient client, String owner, String applicationIdentifier, String apiToken, String releaseId, List<Map<String,String>> destinations, AppCenterBuildInfo buildInfo) {
        //     curl -X PATCH --header 'Content-Type: application/json'
        //                   --header 'Accept: application/json'
        //                   --header 'X-API-Token: xxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxx'
        //                   -d '{ "destinations": [{"name":"QA Testers"}], "release_notes": "Example new release via the APIs" }'
        //                   'https://api.appcenter.ms/v0.1/apps/JoshuaWeber/APITesting/releases/2'
        //
        // https://openapi.appcenter.ms/#/distribute/releases_update

        def uri = "https://api.appcenter.ms/v0.1/apps/${owner}/${applicationIdentifier}/releases/${releaseId}"
        HttpPatch patch = new HttpPatch(uri)
        patch.setHeader("Accept", 'application/json')
        patch.setHeader("X-API-Token", apiToken)

        def build = [:]

        if(buildInfo.branchName && !buildInfo.branchName.empty) {
            build["branch_name"] = buildInfo.branchName
        }

        if(buildInfo.commitHash && !buildInfo.commitHash.empty) {
            build["commit_hash"] = buildInfo.commitHash
        }

        if(buildInfo.commitMessage && !buildInfo.commitMessage.empty) {
            build["commit_message"] = buildInfo.commitMessage
        }

        def body = ["destinations": destinations, "build": build]
        patch.setEntity(new StringEntity(JsonOutput.toJson(body), ContentType.APPLICATION_JSON))

        HttpResponse response = client.execute(patch)

        if (response.statusLine.statusCode != 200) {
            throw new GradleException("unable to distribute release ${releaseId} for ${owner}/${applicationIdentifier}")
        }
    }

    @TaskAction
    protected void upload() {
        HttpClient client = HttpClientBuilder.create().build()
        def uploadResource = createUploadResource(client, getOwner(), getApplicationIdentifier(), getApiToken(), getBuildVersion(), getReleaseId())

        String uploadUrl = uploadResource["upload_url"]
        String uploadId = uploadResource["upload_id"]

        uploadResources(client, getApiToken(), uploadUrl, getBinary())

        def resource = commitResource(client, getOwner(), getApplicationIdentifier(), getApiToken(), uploadId)
        String releaseId = resource["release_id"].toString()
        String releaseUrl = resource["release_url"].toString()

        distribute(client, getOwner(), getApplicationIdentifier(), getApiToken(), releaseId, getDestinations(), getBuildInfo())

        logger.info("published to AppCenter release: ${releaseId}")
        logger.info("release_url: ${releaseUrl}")

        getUploadVersionMetaData() << JsonOutput.prettyPrint(JsonOutput.toJson(resource))
    }

    private static String convertToString(Object value) {
        if (!value) {
            return null
        }

        if (value instanceof Callable) {
            value = ((Callable) value).call()
        }

        value.toString()
    }
}

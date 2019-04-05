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

package wooga.gradle.hockey.tasks

import groovy.json.JsonOutput
import org.apache.http.HttpEntity
import org.apache.http.HttpResponse
import org.apache.http.client.HttpClient
import org.apache.http.client.methods.HttpPost
import org.apache.http.entity.mime.MultipartEntityBuilder
import org.apache.http.entity.mime.content.FileBody
import org.apache.http.impl.client.HttpClientBuilder
import org.gradle.api.GradleException
import org.gradle.api.file.FileCollection
import org.gradle.api.internal.ConventionTask
import org.gradle.api.tasks.*
import wooga.gradle.hockey.api.AppVersion
import wooga.gradle.hockey.api.internal.DefaultAppVersion

import java.util.concurrent.Callable

class HockeyUploadTask extends ConventionTask {

    private Object apiToken

    @Input
    String getApiToken() {
        convertToString(apiToken)
    }

    void setApiToken(Object value) {
        apiToken = value
    }

    HockeyUploadTask apiToken(Object apiToken) {
        setApiToken(apiToken)
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

    HockeyUploadTask applicationIdentifier(Object applicationIdentifier) {
        setApplicationIdentifier(applicationIdentifier)
        this
    }

    @OutputFiles
    protected FileCollection getOutputFiles() {
        return project.files(getUploadVersionMetaData())
    }

    File getUploadVersionMetaData() {
        new File(temporaryDir, "${getApplicationIdentifier()}.json")
    }

    private DefaultAppVersion appVersion

    AppVersion getAppVersion() {
        appVersion
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

    HockeyUploadTask binary(Object binary) {
        setBinary(binary)
        this
    }

    @TaskAction
    protected void upload() {
        HttpClient client = HttpClientBuilder.create().build()
        HttpPost post = new HttpPost("https://rink.hockeyapp.net/api/2/apps/${getApplicationIdentifier()}/app_versions/upload")

        post.setHeader("X-HockeyAppToken", getApiToken())

        FileBody binary = new FileBody(getBinary())

        // TODO: make status and notify configurable, add changelog support
        HttpEntity content = MultipartEntityBuilder.create()
                .addPart("ipa", binary)
                .addTextBody("status", "2")
                .addTextBody("notify", "2")
                .build()

        post.setEntity(content)
        HttpResponse response = client.execute(post)

        if(response.statusLine.statusCode != 201) {
            throw new GradleException("unable to upload to hockey")
        }

        getUploadVersionMetaData() << JsonOutput.prettyPrint(response.entity.content.text)

        appVersion = new DefaultAppVersion(getUploadVersionMetaData())
        logger.info("Created new Version ${appVersion.version}")
        logger.info("visit ${appVersion.publicUrl}")
        logger.info("build: ${appVersion.buildUrl}")
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

/*
 * Copyright 2018-2021 Wooga GmbH
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *         http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package wooga.gradle.appcenter.tasks

import groovy.json.JsonOutput
import org.apache.http.client.HttpClient
import org.apache.http.client.config.RequestConfig
import org.apache.http.impl.client.HttpClientBuilder
import org.apache.tools.ant.types.resources.FileProvider
import org.gradle.api.Action
import org.gradle.api.DefaultTask
import org.gradle.api.file.DirectoryProperty
import org.gradle.api.file.RegularFile
import org.gradle.api.file.RegularFileProperty
import org.gradle.api.provider.ListProperty
import org.gradle.api.provider.Property
import org.gradle.api.provider.Provider
import org.gradle.api.tasks.*
import wooga.gradle.appcenter.api.AppCenterBuildInfo
import wooga.gradle.appcenter.api.AppCenterReleaseUploader
import wooga.gradle.appcenter.api.AppCenterRetryStrategy

import static org.gradle.util.ConfigureUtil.configureUsing

class AppCenterUploadTask extends DefaultTask {

    @Input
    private final Property<String> apiToken

    Property<String> getApiToken() {
        apiToken
    }

    void setApiToken(Provider<String> value) {
        apiToken.set(value)
    }

    @Input
    private final Property<String> owner

    Property<String> getOwner() {
        owner
    }

    void setOwner(Provider<String> value) {
        owner.set(value)
    }

    @Input
    private final Property<String> buildVersion

    Property<String> getBuildVersion() {
        buildVersion
    }

    void setBuildVersion(Provider<String> value) {
        buildVersion.set(value)
    }

    @Optional
    @Input
    private final Property<String> buildNumber

    Property<String> getBuildNumber() {
        buildNumber
    }

    void setBuildNumber(Provider<String> value) {
        buildNumber.set(value)
    }

    @Input
    private final Property<String> applicationIdentifier

    Property<String> getApplicationIdentifier() {
        applicationIdentifier
    }

    void setApplicationIdentifier(Provider<String> value) {
        applicationIdentifier.set(value)
    }

    @Optional
    @Input
    private final Property<String> releaseNotes

    Property<String> getReleaseNotes() {
        releaseNotes
    }

    void setReleaseNotes(Provider<String> value) {
        releaseNotes.set(value)
    }

    @Optional
    @Input
    private final ListProperty<Map<String, String>> destinations

    ListProperty<Map<String, String>> getDestinations() {
        destinations
    }

    void setDestinations(Iterable<String> value) {
        destinations.set(value.collect { ["name": it] })
    }

    void setDestinations(String... value) {
        destinations.set(value.collect { ["name": it] })
    }

    void destination(String name) {
        destinations.add(["name": name])
    }

    void destination(Iterable<String> destinations) {
        this.destinations.addAll(destinations.collect { ["name": it] })
    }

    void destination(String... destinations) {
        this.destinations.addAll(destinations.collect { ["name": it] })
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

    @InputFile
    final RegularFileProperty binary

    RegularFileProperty getBinary(){
        binary
    }

    void setBinary(Provider<RegularFile> value) {
        binary.set(value)
    }

    @Internal
    protected final DirectoryProperty outputDir

    @OutputFile
    final Provider<RegularFile> uploadVersionMetaData

    @Internal
    private final Property<Long> retryTimeout

    Property<Long> getRetryTimeout() {
        retryTimeout
    }

    void setRetryTimeout(Provider<Long> value) {
        retryTimeout.set(value)
    }

    @Internal
    private final Property<Integer> retryCount

    Property<Integer> getRetryCount() {
        retryCount
    }

    void setRetryCount(Provider<Integer> value) {
        retryCount.set(value)
    }

    AppCenterUploadTask() {
        apiToken = project.objects.property(String)
        owner = project.objects.property(String)
        buildVersion = project.objects.property(String)
        buildNumber = project.objects.property(String)
        applicationIdentifier = project.objects.property(String)
        releaseNotes = project.objects.property(String)
        destinations = project.objects.listProperty(Map)
        retryTimeout = project.objects.property(Long)
        retryCount = project.objects.property(Integer)
        binary = project.objects.fileProperty()
        outputDir = project.objects.directoryProperty()

        outputDir.set(temporaryDir)
        uploadVersionMetaData = outputDir.file(owner.map({ owner -> "${owner}_${applicationIdentifier.get()}.json" }))
    }

    @TaskAction
    protected void upload() {
        int timeout = 30
        RequestConfig config = RequestConfig.custom()
                .setConnectTimeout(timeout * 1000)
                .setConnectionRequestTimeout(timeout * 1000)
                .setSocketTimeout(timeout * 1000).build()

        HttpClient client = HttpClientBuilder.create()
                .setDefaultRequestConfig(config)
                .setServiceUnavailableRetryStrategy(new AppCenterRetryStrategy(retryCount.get(), retryTimeout.get().toInteger()))
                .build()

        AppCenterReleaseUploader uploader = new AppCenterReleaseUploader(client,
                owner.get(),
                applicationIdentifier.get(),
                apiToken.get())

        AppCenterReleaseUploader.DistributionSettings distributionSettings = uploader.distributionSettings
        AppCenterReleaseUploader.UploadVersion version = uploader.version

        version.buildNumber = buildNumber.getOrNull()
        version.buildVersion = buildVersion.getOrNull()

        distributionSettings.releaseNotes = releaseNotes.getOrElse("")
        distributionSettings.destinations = destinations.getOrElse([])
        distributionSettings.buildInfo = getBuildInfo()

        AppCenterReleaseUploader.UploadResult result = uploader.upload(binary.get().asFile)
        logger.info("published to AppCenter release: ${result.releaseID}")
        logger.info("download_url: ${result.downloadUrl}")
        logger.info("install_url: ${result.installUrl}")
        uploadVersionMetaData.get().asFile << JsonOutput.prettyPrint(JsonOutput.toJson(result.release))
    }
}

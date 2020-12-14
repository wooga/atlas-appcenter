package wooga.gradle.appcenter.tasks

import groovy.json.JsonOutput
import org.apache.http.client.HttpClient
import org.apache.http.impl.client.HttpClientBuilder
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
import wooga.gradle.appcenter.api.AppCenterRest
import wooga.gradle.compat.ProjectLayout

import static org.gradle.util.ConfigureUtil.configureUsing

class AppCenterUploadTask extends DefaultTask {

    @Input
    final Property<String> apiToken

    void setApiToken(String value) {
        apiToken.set(value)
    }

    void apiToken(String value) {
        setApiToken(value)
    }

    @Input
    final Property<String> owner

    void setOwner(String value) {
        owner.set(value)
    }

    void owner(String value) {
        setOwner(value)
    }

    @Input
    final Property<String> buildVersion

    void setBuildVersion(String value) {
        buildVersion.set(value)
    }

    void buildVersion(String value) {
        setBuildVersion(value)
    }

    @Optional
    @Input
    final Property<String> buildNumber

    void setBuildNumber(String value) {
        buildNumber.set(value)
    }

    void releaseId(String value) {
        setBuildNumber(value)
    }

    @Input
    final Property<String> applicationIdentifier

    void setApplicationIdentifier(String value) {
        applicationIdentifier.set(value)
    }

    void applicationIdentifier(String value) {
        setApplicationIdentifier(value)
    }

    @Optional
    @Input
    final Property<String> releaseNotes

    void setReleaseNotes(String value) {
        releaseNotes.set(value)
    }

    void releaseNotes(String value) {
        setReleaseNotes(value)
    }

    @Optional
    @Input
    final ListProperty<Map<String, String>> destinations

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

    void setBinary(String value) {
        binary.set(project.file(value))
        RegularFile
    }

    void setBinary(File value) {
        binary.set(value)
    }

    void binary(String value) {
        setBinary(value)
    }

    void binary(File value) {
        setBinary(value)
    }

    @Internal
    protected final DirectoryProperty outputDir

    @OutputFile
    final Provider<RegularFile> uploadVersionMetaData

    @Internal
    final Property<Long> retryTimeout

    void setRetryTimeout(Long value) {
        this.retryTimeout.set(value)
    }

    void retryTimeout(Long value) {
        setRetryTimeout(value)
    }

    void setRetryCount(Integer value) {
        retryCount.set(value)
    }

    void retryCount(Integer value) {
        setRetryCount(value)
    }

    final Property<Integer> retryCount

    AppCenterUploadTask() {
        def projectLayout = new ProjectLayout(project)
        apiToken = project.objects.property(String)
        owner = project.objects.property(String)
        buildVersion = project.objects.property(String)
        buildNumber = project.objects.property(String)
        applicationIdentifier = project.objects.property(String)
        releaseNotes = project.objects.property(String)
        destinations = project.objects.listProperty(Map)
        retryTimeout = project.objects.property(Long)
        retryCount = project.objects.property(Integer)
        binary = projectLayout.fileProperty()
        outputDir = projectLayout.directoryProperty()
        outputDir.set(temporaryDir)

        uploadVersionMetaData = outputDir.file(owner.map({ owner -> "${owner}_${applicationIdentifier.get()}.json" }))
    }

    @TaskAction
    protected void upload() {
        HttpClient client = HttpClientBuilder.create().build()
        String owner = owner.get()
        String applicationIdentifier = applicationIdentifier.get()
        String apiToken = apiToken.get()
        String buildVersion = buildVersion.getOrNull()
        String buildNumber = buildNumber.getOrNull()
        File binary = binary.get().asFile
        List<Map<String, String>> destinations = destinations.getOrElse([])

        String releaseNotes = releaseNotes.getOrElse("")

        def releaseUpload = AppCenterRest.createReleaseUpload(client, owner, applicationIdentifier, apiToken, buildVersion, buildNumber)

        AppCenterRest.uploadFile(client, releaseUpload, binary)
        AppCenterRest.updateReleaseUpload(client, owner, applicationIdentifier, apiToken, releaseUpload.id, AppCenterRest.ReleaseUploadStatus.uploadFinished)

        def releaseId = AppCenterRest.pollForReleaseId(client, owner, applicationIdentifier, apiToken, releaseUpload.id)
        def release = AppCenterRest.getRelease(client, owner, applicationIdentifier, apiToken, releaseId)

        String downloadUrl = release["download_url"].toString()
        String installUrl = release["install_url"].toString()

        AppCenterRest.distribute(client, owner, applicationIdentifier, apiToken, releaseId, destinations, getBuildInfo(), releaseNotes)

        logger.info("published to AppCenter release: ${releaseId}")
        logger.info("download_url: ${downloadUrl}")
        logger.info("install_url: ${installUrl}")

        uploadVersionMetaData.get().asFile << JsonOutput.prettyPrint(JsonOutput.toJson(release))
    }
}

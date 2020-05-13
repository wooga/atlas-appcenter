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
import org.gradle.api.DefaultTask
import org.gradle.api.GradleException
import org.gradle.api.file.DirectoryProperty
import org.gradle.api.file.RegularFile
import org.gradle.api.file.RegularFileProperty
import org.gradle.api.provider.ListProperty
import org.gradle.api.provider.Property
import org.gradle.api.provider.Provider
import org.gradle.api.tasks.*
import wooga.gradle.appcenter.api.AppCenterBuildInfo
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
    final Property<Integer> releaseId

    void setReleaseId(Integer value) {
        releaseId.set(value)
    }

    void releaseId(Integer value) {
        setReleaseId(value)
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
        destinations.set(value.collect {["name": it]})
    }

    void setDestinations(String... value) {
        destinations.set(value.collect {["name": it]})
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

    AppCenterUploadTask() {
        def projectLayout = new ProjectLayout(project)
        apiToken = project.objects.property(String)
        owner = project.objects.property(String)
        buildVersion = project.objects.property(String)
        releaseId = project.objects.property(Integer)
        applicationIdentifier = project.objects.property(String)
        releaseNotes = project.objects.property(String)
        destinations = project.objects.listProperty(Map)

        binary = projectLayout.fileProperty()
        outputDir = projectLayout.directoryProperty()
        outputDir.set(temporaryDir)

        uploadVersionMetaData = outputDir.file(owner.map({ owner -> "${owner}_${applicationIdentifier.get()}.json" }))
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

    private static void distribute(HttpClient client, String owner, String applicationIdentifier, String apiToken, String releaseId, List<Map<String, String>> destinations, AppCenterBuildInfo buildInfo, String releaseNotes) {
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

        if (buildInfo.branchName && !buildInfo.branchName.empty) {
            build["branch_name"] = buildInfo.branchName
        }

        if (buildInfo.commitHash && !buildInfo.commitHash.empty) {
            build["commit_hash"] = buildInfo.commitHash
        }

        if (buildInfo.commitMessage && !buildInfo.commitMessage.empty) {
            build["commit_message"] = buildInfo.commitMessage
        }

        def body = ["destinations": destinations, "build": build, "release_notes": releaseNotes]

        patch.setEntity(new StringEntity(JsonOutput.toJson(body), ContentType.APPLICATION_JSON))

        HttpResponse response = client.execute(patch)

        if (response.statusLine.statusCode != 200) {
            throw new GradleException("unable to distribute release ${releaseId} for ${owner}/${applicationIdentifier}")
        }
    }

    @TaskAction
    protected void upload() {
        HttpClient client = HttpClientBuilder.create().build()
        String owner = owner.get()
        String applicationIdentifier = applicationIdentifier.get()
        String apiToken = apiToken.get()
        String buildVersion = buildVersion.get()
        Integer releaseId = releaseId.getOrElse(0)
        File binary = binary.get().asFile
        List<Map<String, String>> destinations = destinations.getOrElse([])

        String releaseNotes = releaseNotes.getOrElse("")

        def uploadResource = createUploadResource(client, owner, applicationIdentifier, apiToken, buildVersion, releaseId)

        String uploadUrl = uploadResource["upload_url"]
        String uploadId = uploadResource["upload_id"]

        uploadResources(client, apiToken, uploadUrl, binary)

        def resource = commitResource(client, owner, applicationIdentifier, apiToken, uploadId)
        String finalReleaseId = resource["release_id"].toString()
        String releaseUrl = resource["release_url"].toString()

        distribute(client, owner, applicationIdentifier, apiToken, finalReleaseId, destinations, getBuildInfo(), releaseNotes)

        logger.info("published to AppCenter release: ${finalReleaseId}")
        logger.info("release_url: ${releaseUrl}")

        uploadVersionMetaData.get().asFile << JsonOutput.prettyPrint(JsonOutput.toJson(resource))
    }

}

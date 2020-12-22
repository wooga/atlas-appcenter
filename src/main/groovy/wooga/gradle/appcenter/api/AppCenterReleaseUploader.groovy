package wooga.gradle.appcenter.api

import groovy.json.JsonOutput
import org.apache.commons.io.FilenameUtils
import org.apache.http.HttpRequest
import org.apache.http.HttpResponse
import org.apache.http.client.HttpClient
import org.apache.http.client.methods.HttpGet
import org.apache.http.client.methods.HttpPatch
import org.apache.http.client.methods.HttpPost
import org.apache.http.client.utils.URIBuilder
import org.apache.http.entity.ByteArrayEntity
import org.apache.http.entity.ContentType
import org.apache.http.entity.StringEntity
import org.gradle.api.GradleException
import wooga.gradle.appcenter.api.AppCenterRest.AppCenterError

import java.util.logging.Logger
import static wooga.gradle.appcenter.api.AppCenterRest.getResponseBody

class AppCenterReleaseUploader {

    enum ReleaseUploadStatus {
        uploadStarted, uploadFinished, readyToBePublished, malwareDetected, error
    }

    static class UploadVersion {
        String buildVersion
        String buildNumber
    }

    static class DistributionSettings {
        List<Map<String, String>> destinations = []
        String releaseNotes = ""
        AppCenterBuildInfo buildInfo = new AppCenterBuildInfo()
    }

    static class RetrySettings {
        Long timeout = DEFAULT_WAIT_RATE_LIMIT_DURATION
        Integer maxRetries = 10
    }

    static class UploadResult {
        final Map release
        final String releaseID
        final String downloadUrl
        final String installUrl

        UploadResult(Map release) {
            this.release = release
            this.releaseID = release['release_Id']
            this.downloadUrl = release["download_url"].toString()
            this.installUrl = release["install_url"].toString()
        }
    }

    static class CreateReleaseUpload {
        final String id
        final String uploadDomain
        final String token
        final String urlEncodedToken
        final String packageAssetId

        private CreateReleaseUpload(String id, String uploadDomain, String token, String urlEncodedToken, String packageAssetId) {
            this.id = id
            this.uploadDomain = uploadDomain
            this.token = token
            this.urlEncodedToken = urlEncodedToken
            this.packageAssetId = packageAssetId
        }

        static CreateReleaseUpload fromResponse(HttpResponse response) {
            def responseBody = getResponseBody(response)
            String id = responseBody["id"]
            String uploadDomain = responseBody["upload_domain"]
            String token = responseBody["token"]
            String urlEncodedToken = responseBody["url_encoded_token"]
            String packageAssetId = responseBody["package_asset_id"]

            new CreateReleaseUpload(id, uploadDomain, token, urlEncodedToken, packageAssetId)
        }
    }

    static class ReleaseUpload {
        String releaseId
        ReleaseUploadStatus uploadStatus
        String errorDetails
        String releaseDistinctId
        URI releaseUrl

        private ReleaseUpload(String releaseId, ReleaseUploadStatus uploadStatus, String errorDetails, String releaseDistinctId, URI releaseUrl) {
            this.releaseId = releaseId
            this.uploadStatus = uploadStatus
            this.errorDetails = errorDetails
            this.releaseDistinctId = releaseDistinctId
            this.releaseUrl = releaseUrl
        }

        static ReleaseUpload fromResponse(HttpResponse response) {
            def responseBody = getResponseBody(response)
            String releaseId = responseBody['id']
            ReleaseUploadStatus uploadStatus = responseBody['upload_status'] as ReleaseUploadStatus
            String releaseDistinctId = null
            URI releaseUrl = null
            String errorDetails = null

            switch (uploadStatus) {
                case ReleaseUploadStatus.readyToBePublished:
                    releaseDistinctId = responseBody['release_distinct_id']
                    releaseUrl = new URI(responseBody['release_url'].toString())
                    break
                case ReleaseUploadStatus.error:
                    errorDetails = responseBody['error_details']
                    break
            }

            new ReleaseUpload(releaseId, uploadStatus, errorDetails, releaseDistinctId, releaseUrl)
        }
    }

    static Logger logger = Logger.getLogger(AppCenterRest.name)
    static API_BASE_URL = AppCenterRest.API_BASE_URL
    static final Integer DEFAULT_WAIT_RATE_LIMIT_DURATION = 1000 * 60

    final HttpClient client
    final String owner
    final String applicationIdentifier
    final String apiToken

    private retryCounter = 0

    final DistributionSettings distributionSettings = new DistributionSettings()
    final UploadVersion version = new UploadVersion()
    final RetrySettings retrySettings = new RetrySettings()

    AppCenterReleaseUploader(HttpClient client,
                             String owner,
                             String applicationIdentifier,
                             String apiToken) {
        this.client = client
        this.owner = owner
        this.applicationIdentifier = applicationIdentifier
        this.apiToken = apiToken
    }

    UploadResult upload(File binary) {
        retryCounter = 0
        CreateReleaseUpload releaseUpload = createReleaseUpload(version)
        uploadFile(releaseUpload, binary)
        updateReleaseUpload(releaseUpload.id, ReleaseUploadStatus.uploadFinished)

        String releaseId = pollForReleaseId(releaseUpload.id)
        Map release = getRelease(releaseId)
        distribute(releaseId, distributionSettings)

        UploadResult result = new UploadResult(release)
        return result
    }

    private CreateReleaseUpload createReleaseUpload(UploadVersion version) {

        logger.info("create new release upload for ${owner}/${applicationIdentifier}".toString())

        HttpPost request = new HttpPost(getUploadReleasesUri())
        setHeaders(request)

        def body = [:]

        if (version.buildVersion && !version.buildVersion.isEmpty()) {
            body["build_version"] = version.buildVersion
        }

        if (version.buildNumber && !version.buildNumber.isEmpty()) {
            body["build_number"] = version.buildNumber
        }

        request.setEntity(new StringEntity(JsonOutput.toJson(body), ContentType.APPLICATION_JSON))

        HttpResponse response = client.execute(request)
        switch (response.statusLine.statusCode) {
            case 404:
            case 400:
                def error = AppCenterError.fromResponse(response)
                throw new GradleException("unable to create release upload for ${owner}/${applicationIdentifier}: ${error.code} ${error.message}")
            case 201:
                return CreateReleaseUpload.fromResponse(response)
            default:
                onUnhandledResponse("Unable to create release upload", response)
        }
    }

    private updateReleaseUpload(String uploadId, ReleaseUploadStatus status) {
        logger.info("update release upload ${uploadId} for ${owner}/${applicationIdentifier}".toString())
        HttpPatch request = new HttpPatch(getUploadReleasesUri(uploadId))
        setHeaders(request)

        def body = [
                "upload_status": status.toString(),
                "id"           : uploadId
        ]
        request.setEntity(new StringEntity(JsonOutput.toJson(body), ContentType.APPLICATION_JSON))

        HttpResponse response = client.execute(request)
        switch (response.statusLine.statusCode) {
            case 404:
            case 400:
                def error = AppCenterError.fromResponse(response)
                throw new GradleException("unable to update release upload ${uploadId} for ${owner}/${applicationIdentifier}: ${error.code} ${error.message}")
            case 200:
                logger.info("release upload ${uploadId} for ${owner}/${applicationIdentifier} status updated")
                break
            default:
                onUnhandledResponse("Unable to update release upload", response)
        }
    }

    private String pollForReleaseId(String uploadId) {
        logger.info("poll for releaseId of upload ${uploadId} for ${owner}/${applicationIdentifier}".toString())
        HttpGet request = new HttpGet(getUploadReleasesUri(uploadId))
        setHeaders(request)

        while(true) {
            HttpResponse response = client.execute(request)
            switch (response.statusLine.statusCode) {
                case 404:
                case 400:
                    def error = AppCenterError.fromResponse(response)
                    throw new GradleException("unable to poll release id of upload ${uploadId} ${owner}/${applicationIdentifier}: ${error.code} ${error.message}")
                case 200:
                    ReleaseUpload upload = ReleaseUpload.fromResponse(response)

                    switch (upload.uploadStatus) {
                        case ReleaseUploadStatus.readyToBePublished:
                            logger.info("fetched release URL ${upload.releaseUrl}")
                            logger.info("fetched releaseId ${upload.releaseDistinctId}")
                            return upload.releaseDistinctId
                        case ReleaseUploadStatus.error:
                            throw new GradleException("Error fetching release: ${upload.errorDetails}")
                            break
                        case ReleaseUploadStatus.malwareDetected:
                            throw new GradleException("Error fetching release: Malware Detected")
                            break
                        default:
                            logger.info("wait and poll for releaseId again")
                            sleep(2000)
                            break
                    }
                    break
                default:
                    onUnhandledResponse("Unable to poll for release id '${uploadId}'", response)
            }
        }
    }

    private Map getRelease(String releaseId) {
        logger.info("get release ${releaseId} for ${owner}/${applicationIdentifier}".toString())
        HttpGet request = new HttpGet(getReleasesUri(releaseId))
        setHeaders(request)

        HttpResponse response = client.execute(request)
        switch (response.statusLine.statusCode) {
            case 404:
            case 400:
                def error = AppCenterError.fromResponse(response)
                throw new GradleException("unable to update release ${releaseId} for ${owner}/${applicationIdentifier}: ${error.code} ${error.message}")
            case 200:
                return getResponseBody(response)
            default:
                onUnhandledResponse("Unable to get release id ${releaseId}", response)
        }
    }

    private void distribute(String releaseId, DistributionSettings settings) {
        logger.info("distribute release ${releaseId} for ${owner}/${applicationIdentifier}".toString())

        HttpPatch request = new HttpPatch(getReleasesUri(releaseId))
        setHeaders(request)

        def build = [:]
        def buildInfo = settings.buildInfo
        if (buildInfo.branchName && !buildInfo.branchName.empty) {
            build["branch_name"] = buildInfo.branchName
        }

        if (buildInfo.commitHash && !buildInfo.commitHash.empty) {
            build["commit_hash"] = buildInfo.commitHash
        }

        if (buildInfo.commitMessage && !buildInfo.commitMessage.empty) {
            build["commit_message"] = buildInfo.commitMessage
        }

        def body = ["destinations": settings.destinations, "build": build, "release_notes": settings.releaseNotes]

        logger.fine("request body:")
        logger.fine(body.toString())

        request.setEntity(new StringEntity(JsonOutput.toJson(body), ContentType.APPLICATION_JSON))

        HttpResponse response = client.execute(request)
        switch (response.statusLine.statusCode) {
            case 404:
            case 400:
                def error = AppCenterError.fromResponse(response)
                throw new GradleException("unable to distribute release ${releaseId} for ${owner}/${applicationIdentifier}: ${error.code} ${error.message}")
            case 200:
                logger.info("distribute release ${releaseId} for ${owner}/${applicationIdentifier} successfull")
                break
            default:
                onUnhandledResponse("unable to distribute release ${releaseId}", response)
        }
    }

    private void uploadFile(CreateReleaseUpload uploadCreation, File binary) {
        logger.info("upload file ${binary.path}".toString())
        def chunkSize = setReleaseUploadMetadata(uploadCreation, binary)
        uploadChunks(uploadCreation, binary, chunkSize)
        uploadFinish(uploadCreation)
    }

    private Integer setReleaseUploadMetadata(CreateReleaseUpload uploadCreation, File binary) {
        logger.info("set upload file metadata".toString())
        HttpPost post = new HttpPost(getUploadSetMetadataUri(uploadCreation, binary))
        post.setHeader("Accept", 'application/json')
        HttpResponse response = client.execute(post)

        if (response.statusLine.statusCode >= 200 && response.statusLine.statusCode < 300) {
            def responseBody = getResponseBody(response)
            def chunkSize = responseBody["chunk_size"] as Integer
            logger.info("appcenter requests upload in chunks: ${chunkSize}")
            return chunkSize
        }

        throw new GradleException("Set metadata didn't return chunk size")
    }

    private void uploadChunks(CreateReleaseUpload uploadCreation, File binary, Integer chunksize) {
        logger.info("upload file in chunks".toString())
        def blockNumber = 1
        def s = binary.newDataInputStream()
        s.eachByte(chunksize) { byte[] bytes, Integer size ->
            logger.info("upload chunk ${blockNumber}".toString())
            HttpPost request = new HttpPost(getUploadChunkUri(uploadCreation, blockNumber))
            request.setEntity(new ByteArrayEntity(bytes, 0, size, ContentType.APPLICATION_OCTET_STREAM))

            HttpResponse response = client.execute(request)

            if (response.statusLine.statusCode >= 200 && response.statusLine.statusCode < 300) {
                def responseBody = getResponseBody(response)
                if (responseBody['error'] == false) {
                    logger.info("upload chunk ${blockNumber} succesfull".toString())
                    blockNumber = blockNumber + 1
                } else {
                    throw new GradleException("Error while uploading chunk")
                }
            } else {
                throw new GradleException("Error while uploading chunk")
            }
        }
        logger.info("upload file complete".toString())
    }

    private void uploadFinish(CreateReleaseUpload uploadCreation) {
        logger.info("finish file upload".toString())
        HttpPost request = new HttpPost(getUploadFinishedUri(uploadCreation))
        HttpResponse response = client.execute(request)

        if (response.statusLine.statusCode != 200) {
            throw new GradleException("Failed to finish upload")
        }

        def responseBody = getResponseBody(response)
        if (responseBody['error'] == true) {
            throw new GradleException("unable to finish upload ${responseBody['message']}")
        }
    }

    private URI getUploadReleasesUri(String uploadId = "") {
        def builder = new URIBuilder("${API_BASE_URL}/${owner}/${applicationIdentifier}/uploads/releases/${uploadId}")
        builder.build()
    }

    private URI getReleasesUri(String releaseId) {
        def builder = new URIBuilder("${API_BASE_URL}/${owner}/${applicationIdentifier}/releases/${releaseId}")
        builder.build()
    }

    private setHeaders(HttpRequest request) {
        request.setHeader("Accept", 'application/json')
        request.setHeader("X-API-Token", apiToken)
    }

    private onUnhandledResponse(String message, HttpResponse response) {
        logger.severe(message)
        logger.severe(response.toString())
        throw new GradleException("${message} [For owner(${owner}), applicationIdentifier(${applicationIdentifier})] RESPONSE STATUS CODE: ${response.statusLine.statusCode}")
    }

    private static URI getUploadSetMetadataUri(CreateReleaseUpload uploadCreation, File binary) {
        String contentType = AppCenterRest.CONTENT_TYPES[FilenameUtils.getExtension(binary.name).toLowerCase()] ?: "application/octet-stream"
        def builder = new URIBuilder("${uploadCreation.uploadDomain}/upload/set_metadata/${uploadCreation.packageAssetId}")
        builder.setParameter("file_name", binary.name)
        builder.setParameter("file_size", binary.size().toString())
        builder.setParameter("token", uploadCreation.token)
        builder.setParameter("content_type", contentType)
        builder.build()
    }

    private static URI getUploadChunkUri(CreateReleaseUpload uploadCreation, Integer blockNumber) {
        def builder = new URIBuilder("${uploadCreation.uploadDomain}/upload/upload_chunk/${uploadCreation.packageAssetId}")
        builder.setParameter("token", uploadCreation.token)
        builder.setParameter("block_number", blockNumber.toString())
        builder.build()
    }

    private static URI getUploadFinishedUri(CreateReleaseUpload uploadCreation) {
        def builder = new URIBuilder("${uploadCreation.uploadDomain}/upload/finished/${uploadCreation.packageAssetId}")
        builder.setParameter("token", uploadCreation.token)
        builder.build()
    }
}

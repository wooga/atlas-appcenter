package wooga.gradle.appcenter.api

import groovy.json.JsonOutput
import groovy.json.JsonSlurper
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

import java.util.logging.Logger

class AppCenterRest {
    static Logger logger = Logger.getLogger(AppCenterRest.name)

    static CONTENT_TYPES = [
            apk       : "application/vnd.android.package-archive",
            aab       : "application/vnd.android.package-archive",
            msi       : "application/x-msi",
            plist     : "application/xml",
            aetx      : "application/c-x509-ca-cert",
            cer       : "application/pkix-cert",
            xap       : "application/x-silverlight-app",
            appx      : "application/x-appx",
            appxbundle: "application/x-appxbundle",
            appxupload: "application/x-appxupload",
            appxsym   : "application/x-appxupload",
            msix      : "application/x-msix",
            msixbundle: "application/x-msixbundle",
            msixupload: "application/x-msixupload",
            msixsym   : "application/x-msixupload",
    ]

    enum AppCenterErrorCode {
        BadRequest, Conflict, NotAcceptable, NotFound, InternalServerError, Unauthorized, TooManyRequests
    }

    static class AppCenterError {
        final String code
        final String message

        private AppCenterError(String code, String message) {
            this.code = code
            this.message = message
        }

        static AppCenterError fromResponse(HttpResponse response) {
            def responseBody = responsBody(response)
            String code = (responseBody["code"])
            String message = responseBody["message"]

            new AppCenterError(code, message)
        }
    }

    enum ReleaseUploadStatus {
        uploadStarted, uploadFinished, readyToBePublished, malwareDetected, error
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
            def responseBody = responsBody(response)
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
            def responseBody = responsBody(response)
            String id = responseBody["id"]
            String uploadDomain = responseBody["upload_domain"]
            String token = responseBody["token"]
            String urlEncodedToken = responseBody["url_encoded_token"]
            String packageAssetId = responseBody["package_asset_id"]

            new CreateReleaseUpload(id, uploadDomain, token, urlEncodedToken, packageAssetId)
        }
    }

    static final API_BASE_URL = "https://api.appcenter.ms/v0.1/apps/"

    private static URI getUploadSetMetadataUri(CreateReleaseUpload uploadCreation, File binary) {
        String contentType = CONTENT_TYPES[FilenameUtils.getExtension(binary.name).toLowerCase()] ?: "application/octet-stream"
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

    private static URI getUploadReleasesUri(String owner, String applicationIdentifier, String uploadId = "") {
        def builder = new URIBuilder("${API_BASE_URL}/${owner}/${applicationIdentifier}/uploads/releases/${uploadId}")
        builder.build()
    }

    private static URI getReleasesUri(String owner, String applicationIdentifier, String releaseId) {
        def builder = new URIBuilder("${API_BASE_URL}/${owner}/${applicationIdentifier}/releases/${releaseId}")
        builder.build()
    }

    private static Map responsBody(HttpResponse response) {
        def jsonSlurper = new JsonSlurper()
        jsonSlurper.parseText(response.entity.content.text) as Map
    }

    private static setHeaders(HttpRequest request, String apiToken) {
        request.setHeader("Accept", 'application/json')
        request.setHeader("X-API-Token", apiToken)
    }

    static CreateReleaseUpload createReleaseUpload(HttpClient client, String owner, String applicationIdentifier, String apiToken, String buildVersion = null, String buildNumber = null) {
        logger.info("create new release upload for ${owner}/${applicationIdentifier}".toString())

        HttpPost request = new HttpPost(getUploadReleasesUri(owner, applicationIdentifier))
        setHeaders(request, apiToken)

        def body = [:]

        if (buildVersion) {
            body["build_version"] = buildVersion
        }

        if (buildNumber) {
            body["build_number"] = buildNumber
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
        }
    }

    static updateReleaseUpload(HttpClient client, String owner, String applicationIdentifier, String apiToken, String uploadId, ReleaseUploadStatus status) {
        logger.info("update release upload ${uploadId} for ${owner}/${applicationIdentifier}".toString())
        HttpPatch request = new HttpPatch(getUploadReleasesUri(owner, applicationIdentifier, uploadId))
        setHeaders(request, apiToken)

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
        }
    }

    static void uploadFile(HttpClient client, CreateReleaseUpload uploadCreation, File binary) {
        logger.info("upload file ${binary.path}".toString())
        def chunkSize = setReleaseUploadMetadata(client, uploadCreation, binary)
        uploadChunks(client, uploadCreation, binary, chunkSize)
        uploadFinish(client, uploadCreation)
    }

    private static Integer setReleaseUploadMetadata(HttpClient client, CreateReleaseUpload uploadCreation, File binary) {
        logger.info("set upload file metadata".toString())
        HttpPost post = new HttpPost(getUploadSetMetadataUri(uploadCreation, binary))
        post.setHeader("Accept", 'application/json')
        HttpResponse response = client.execute(post)

        if (response.statusLine.statusCode >= 200 && response.statusLine.statusCode < 300) {
            def responsBody = responsBody(response)
            def chunkSize = responsBody["chunk_size"] as Integer
            logger.info("appcenter requests upload in chunks: ${chunkSize}")
            return chunkSize
        }

        throw new GradleException("Set metadata didn't return chunk size")
    }

    private static void uploadChunks(HttpClient client, CreateReleaseUpload uploadCreation, File binary, Integer chunksize) {
        logger.info("upload file in chunks".toString())
        def blockNumber = 1
        def s = binary.newDataInputStream()
        s.eachByte(chunksize) { byte[] bytes, Integer size ->
            logger.info("upload chunk ${blockNumber}".toString())
            HttpPost request = new HttpPost(getUploadChunkUri(uploadCreation, blockNumber))
            request.setEntity(new ByteArrayEntity(bytes, 0, size, ContentType.APPLICATION_OCTET_STREAM))

            HttpResponse response = client.execute(request)

            if (response.statusLine.statusCode >= 200 && response.statusLine.statusCode < 300) {
                def responseBody = responsBody(response)
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

    static void uploadFinish(HttpClient client, CreateReleaseUpload uploadCreation) {
        logger.info("finish file upload".toString())
        HttpPost request = new HttpPost(getUploadFinishedUri(uploadCreation))
        HttpResponse response = client.execute(request)

        if (response.statusLine.statusCode != 200) {
            throw new GradleException("Failed to finish upload")
        }

        def responseBody = responsBody(response)
        if (responseBody['error'] == true) {
            throw new GradleException("unable to finish upload ${responseBody['message']}")
        }
    }

    static String pollForReleaseId(HttpClient client, String owner, String applicationIdentifier, String apiToken, String uploadId) {
        logger.info("poll for releaseId of upload ${uploadId} for ${owner}/${applicationIdentifier}".toString())
        HttpGet request = new HttpGet(getUploadReleasesUri(owner, applicationIdentifier, uploadId))
        setHeaders(request, apiToken)

        while (true) {
            HttpResponse response = client.execute(request)

            switch (response.statusLine.statusCode) {
                case 404:
                case 400:
                    def error = AppCenterError.fromResponse(response)
                    throw new GradleException("unable to poll release id of upload ${uploadId} ${owner}/${applicationIdentifier}: ${error.code} ${error.message}")
                case 200:
                    def body = ReleaseUpload.fromResponse(response)

                    switch (body.uploadStatus) {
                        case ReleaseUploadStatus.readyToBePublished:
                            logger.info("fetched release URL ${body.releaseUrl}")
                            logger.info("fetched releaseId ${body.releaseDistinctId}")
                            return body.releaseDistinctId
                        case ReleaseUploadStatus.error:
                            throw new GradleException("Error fetching release: ${body.errorDetails}")
                            break
                        case ReleaseUploadStatus.malwareDetected:
                            throw new GradleException("Error fetching release: Malware Detected")
                            break
                        default:
                            logger.info("wait and poll for releaseId again")
                            sleep(1000)
                            break
                    }
            }
        }
    }

    static Map getRelease(HttpClient client, String owner, String applicationIdentifier, String apiToken, String releaseId) {
        logger.info("get release ${releaseId} for ${owner}/${applicationIdentifier}".toString())
        HttpGet request = new HttpGet(getReleasesUri(owner, applicationIdentifier, releaseId))
        setHeaders(request, apiToken)

        HttpResponse response = client.execute(request)

        switch (response.statusLine.statusCode) {
            case 404:
            case 400:
                def error = AppCenterError.fromResponse(response)
                throw new GradleException("unable to update release ${releaseId} for ${owner}/${applicationIdentifier}: ${error.code} ${error.message}")
            case 200:
                return responsBody(response)
        }
    }

    static void distribute(HttpClient client, String owner, String applicationIdentifier, String apiToken, String releaseId, List<Map<String, String>> destinations, AppCenterBuildInfo buildInfo, String releaseNotes) {
        logger.info("distribute release ${releaseId} for ${owner}/${applicationIdentifier}".toString())

        HttpPatch request = new HttpPatch(getReleasesUri(owner, applicationIdentifier, releaseId))
        setHeaders(request, apiToken)

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

        logger.fine("request body:")
        logger.fine(body.toString())

        request.setEntity(new StringEntity(JsonOutput.toJson(body), ContentType.APPLICATION_JSON))

        HttpResponse response = client.execute(request)

        if (response.statusLine.statusCode != 200) {
            throw new GradleException("unable to distribute release ${releaseId} for ${owner}/${applicationIdentifier}")
        }
    }
}

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

    static final API_BASE_URL = "https://api.appcenter.ms/v0.1/apps/"

    private static URI getUploadSetMetadataUri(String uploadDomain, String assetId, String token, File binary) {
        String contentType = CONTENT_TYPES[FilenameUtils.getExtension(binary.name).toLowerCase()] ?: "application/octet-stream"
        def builder = new URIBuilder("${uploadDomain}/upload/set_metadata/${assetId}")
        builder.setParameter("file_name", binary.name)
        builder.setParameter("file_size", binary.size().toString())
        builder.setParameter("token", token)
        builder.setParameter("content_type", contentType)
        builder.build()
    }

    private static URI getUploadChunkUri(String uploadDomain, String assetId, String token, Integer blockNumber) {
        def builder = new URIBuilder("${uploadDomain}/upload/upload_chunk/${assetId}")
        builder.setParameter("token", token)
        builder.setParameter("block_number", blockNumber.toString())
        builder.build()
    }

    private static URI getUploadFinishedUri(String uploadDomain, String assetId, String token) {
        def builder = new URIBuilder("${uploadDomain}/upload/finished/${assetId}")
        builder.setParameter("token", token)
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

    static Map createReleaseUpload(HttpClient client, String owner, String applicationIdentifier, String apiToken, String buildVersion = null, String buildNumber = null) {
        logger.info("create new release upload for ${owner}/${applicationIdentifier}".toString())

        HttpPost request = new HttpPost(getUploadReleasesUri(owner, applicationIdentifier))
        setHeaders(request, apiToken)

        def body = [:]

        if(buildVersion) {
            body["build_version"] = buildVersion
        }

        if(buildNumber) {
            body["build_number"] = buildNumber
        }

        request.setEntity(new StringEntity(JsonOutput.toJson(body), ContentType.APPLICATION_JSON))

        HttpResponse response = client.execute(request)
        if (response.statusLine.statusCode != 201) {
            throw new GradleException("unable to create release upload for ${owner}/${applicationIdentifier}")
        }

        responsBody(response)
    }

    static Map updateReleaseUpload(HttpClient client, String owner, String applicationIdentifier, String apiToken, String uploadId, String status) {
        logger.info("update release upload ${uploadId} for ${owner}/${applicationIdentifier}".toString())
        HttpPatch request = new HttpPatch(getUploadReleasesUri(owner, applicationIdentifier, uploadId))
        setHeaders(request, apiToken)

        def body = [
                "upload_status": status,
                "id"           : uploadId
        ]
        request.setEntity(new StringEntity(JsonOutput.toJson(body), ContentType.APPLICATION_JSON))

        HttpResponse response = client.execute(request)
        if (response.statusLine.statusCode != 200) {
            throw new GradleException("unable to update release upload ${uploadId} for ${owner}/${applicationIdentifier}")
        }

        responsBody(response)
    }

    static void uploadFile(HttpClient client, String uploadDomain, String assetId, String token, File binary) {
        logger.info("upload file ${binary.path}".toString())
        def chunkSize = setReleaseUploadMetadata(client, uploadDomain, assetId, token, binary)
        uploadChunks(client, uploadDomain, assetId, token, binary, chunkSize)
        uploadFinish(client, uploadDomain, assetId, token)
    }

    private static Integer setReleaseUploadMetadata(HttpClient client, String uploadDomain, String assetId, String token, File binary) {
        logger.info("set upload file metadata".toString())
        HttpPost post = new HttpPost(getUploadSetMetadataUri(uploadDomain, assetId, token, binary))
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

    private static void uploadChunks(HttpClient client, String uploadDomain, String assetId, String token, File binary, Integer chunksize) {
        logger.info("upload file in chunks".toString())
        def blockNumber = 1
        def s = binary.newDataInputStream()
        s.eachByte(chunksize) { byte[] bytes, Integer size ->
            logger.info("upload chunk ${blockNumber}".toString())
            HttpPost request = new HttpPost(getUploadChunkUri(uploadDomain, assetId, token, blockNumber))
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

    static void uploadFinish(HttpClient client, String uploadDomain, String assetId, String token) {
        logger.info("finish file upload".toString())
        HttpPost request = new HttpPost(getUploadFinishedUri(uploadDomain, assetId, token))
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

            if (response.statusLine.statusCode != 200) {
                throw new GradleException("unable to poll release id of upload ${uploadId} for ${owner}/${applicationIdentifier}")
            }

            def responseBody = responsBody(response)
            switch (responseBody['upload_status']) {
                case "readyToBePublished":
                    def releaseId = responseBody['release_distinct_id']
                    logger.info("fetched releaseId ${releaseId}")
                    return releaseId
                case "error":
                    throw new GradleException("Error fetching release: ${responseBody['error_details']}")
                    break
                default:
                    logger.info("wait and poll for releaseId again")
                    sleep(1000)
                    break
            }
        }
    }

    static Map getRelease(HttpClient client, String owner, String applicationIdentifier, String apiToken, String releaseId) {
        logger.info("get release ${releaseId} for ${owner}/${applicationIdentifier}".toString())
        HttpGet request = new HttpGet(getReleasesUri(owner, applicationIdentifier, releaseId))
        setHeaders(request, apiToken)

        HttpResponse response = client.execute(request)
        if (response.statusLine.statusCode != 200) {
            throw new GradleException("unable to update release ${releaseId} for ${owner}/${applicationIdentifier}")
        }

        responsBody(response)
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

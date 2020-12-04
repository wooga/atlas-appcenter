package wooga.gradle.appcenter.api

import org.apache.http.HttpEntity
import org.apache.http.HttpResponse
import org.apache.http.client.HttpClient
import org.apache.http.client.methods.HttpPost
import org.apache.http.entity.mime.MultipartEntityBuilder
import org.apache.http.entity.mime.content.FileBody
import org.gradle.api.GradleException

import java.util.logging.Logger

class AppCenterRest {
    static Logger logger = Logger.getLogger(AppCenterRest.name)

    static Boolean uploadResources(HttpClient client, String apiToken, String uploadUrl, File binary, Integer retryCount = 0, Long retryTimeout = 0) {
        HttpPost post = new HttpPost(uploadUrl)
        FileBody ipa = new FileBody(binary)
        post.setHeader("X-API-Token", apiToken)
        HttpEntity content = MultipartEntityBuilder.create()
                .addPart("ipa", ipa)
                .build()

        post.setEntity(content)
        HttpResponse response = client.execute(post)

        if (response.statusLine.statusCode >= 500) {
            logger.warning("unable to upload to provided upload url ${uploadUrl}".toString())
            logger.warning(response.statusLine.reasonPhrase)

            if (retryCount > 0) {
                logger.warning("wait and retry after ${(retryTimeout) / 1000} s".toString())
                sleep(retryTimeout)
                return uploadResources(client, apiToken, uploadUrl, binary, retryCount - 1, retryTimeout)
            }
        }

        if (response.statusLine.statusCode != 204) {
            throw new GradleException("unable to upload to provided upload url ${uploadUrl}" + response.statusLine.toString())
        }

        true
    }
}

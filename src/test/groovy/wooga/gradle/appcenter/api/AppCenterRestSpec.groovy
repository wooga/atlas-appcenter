package wooga.gradle.appcenter.api

import org.apache.http.HttpResponse
import org.apache.http.StatusLine
import org.apache.http.client.HttpClient
import org.gradle.api.GradleException
import spock.lang.Specification

class AppCenterRestSpec extends Specification {

    def httpClient = Mock(HttpClient)
    def statusLine = Mock(StatusLine)
    def response = Mock(HttpResponse)

    def setup() {
        response.statusLine >> statusLine
        statusLine.reasonPhrase >> "some error reason"

    }

    def "uploadResources fails with exception when status code is not 204"() {
        given:
        statusLine.statusCode >> 404
        httpClient.execute(_) >> response

        when:
        AppCenterRest.uploadResources(httpClient, "", "", File.createTempFile("test", "binary"))

        then:
        def e = thrown(GradleException)
        e.message.startsWith("unable to upload to provided upload url")
    }

    def "uploadResources throws no exception when status code is 204"() {
        given:
        statusLine.statusCode >> 204
        httpClient.execute(_) >> response

        when:
        AppCenterRest.uploadResources(httpClient, "", "", File.createTempFile("test", "binary"))

        then:
        noExceptionThrown()
    }

    def "uploadResources retries when status code is >= 500"() {
        given:
        statusLine.statusCode >> 503
        6 * httpClient.execute(_) >> response

        when:
        AppCenterRest.uploadResources(httpClient, "", "", File.createTempFile("test", "binary"), 5)

        then:
        def e = thrown(GradleException)
        e.message.startsWith("unable to upload to provided upload url")
    }

    def "uploadResources retries with timeout when status code is >= 500"() {
        given:
        statusLine.statusCode >> 503
        4 * httpClient.execute(_) >> response

        when:
        def startTime = System.currentTimeMillis()
        AppCenterRest.uploadResources(httpClient, "", "", File.createTempFile("test", "binary"), retryCount, retryTimeout)

        then:
        thrown(GradleException)
        def duration = System.currentTimeMillis() - startTime
        duration >= expectedDuration - 200 && duration <= expectedDuration + 200

        where:
        retryCount | retryTimeout
        3          | 2000
        expectedDuration = retryTimeout * retryCount
    }

    def "uploadResources retries until retryCount or success"() {
        given:
        statusLine.statusCode >>> [503, 500, 204]
        3 * httpClient.execute(_) >> response

        when:
        def startTime = System.currentTimeMillis()
        AppCenterRest.uploadResources(httpClient, "", "", File.createTempFile("test", "binary"), retryCount, retryTimeout)

        then:
        noExceptionThrown()
        def duration = System.currentTimeMillis() - startTime
        duration >= expectedDuration - 200 && duration <= expectedDuration + 200

        where:
        retryCount | retryTimeout | actualRetries
        3          | 2000         | 2
        expectedDuration = retryTimeout * actualRetries
    }
}

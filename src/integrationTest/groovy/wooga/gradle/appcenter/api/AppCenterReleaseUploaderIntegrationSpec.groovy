package wooga.gradle.appcenter.api

import org.apache.http.client.HttpClient
import org.apache.http.client.HttpRequestRetryHandler
import org.apache.http.client.config.RequestConfig
import org.apache.http.impl.client.DefaultServiceUnavailableRetryStrategy
import org.apache.http.impl.client.HttpClientBuilder
import org.apache.http.protocol.HttpContext
import spock.lang.Specification
import spock.lang.Unroll

class AppCenterReleaseUploaderIntegrationSpec extends Specification {
    static String apiToken = System.env["ATLAS_APP_CENTER_INTEGRATION_API_TOKEN"]
    static String owner = System.env["ATLAS_APP_CENTER_OWNER"]
    static String applicationIdentifierIos = System.env["ATLAS_APP_CENTER_INTEGRATION_APPLICATION_IDENTIFIER_IOS"]
    static String applicationIdentifierAndroid = System.env["ATLAS_APP_CENTER_INTEGRATION_APPLICATION_IDENTIFIER_ANDROID"]

    void writeRandomData(File destination, Long fileSize) {
        def random = new Random()
        def chunkSize = 1024 * 1024 * 4
        def chunksToWrite = fileSize / chunkSize

        destination.withDataOutputStream {
            for (int i = 0; i < chunksToWrite; i++) {
                def chunk = new byte[chunkSize]
                random.nextBytes(chunk)
                it.write(chunk)
            }
        }
    }

    File createBigUploadBinary(File baseBinary, File destinationDir, Long fileSize) {
        def output = new File(destinationDir, baseBinary.name)
        def packagePayloadDir = File.createTempDir(baseBinary.name, "payload")

        def ant = new AntBuilder()
        ant.unzip(src: baseBinary,
                dest: packagePayloadDir.path,
                overwrite: "false")

        writeRandomData(new File(packagePayloadDir, "test.bin"), fileSize - baseBinary.size())

        ant.zip(destfile: output.path, basedir: packagePayloadDir.path)

        output
    }

    @Unroll("uploads big dummy #fileType to AppCenter successfully")
    def "uploads big artifacts"() {
        given: "a dummy ipa binary increased in filesize to upload"
        def testFile = new File(getClass().getClassLoader().getResource(fileName).path)
        testFile = createBigUploadBinary(testFile, File.createTempDir("testUpload", "fileType"), 1024 * 1024 * desiredFileSize)

        int timeout = 30
        RequestConfig config = RequestConfig.custom()
                .setConnectTimeout(timeout * 1000)
                .setConnectionRequestTimeout(timeout * 1000)
                .setSocketTimeout(timeout * 1000).build()

        HttpClient client = HttpClientBuilder.create()
                .setDefaultRequestConfig(config)
                .setServiceUnavailableRetryStrategy(new AppCenterRetryStrategy())
                .build()
        def uploader = new AppCenterReleaseUploader(client, owner, applicationIdentifier, apiToken)

        when:
        uploader.upload(testFile)

        then:
        noExceptionThrown()

        where:
        fileType | fileName   | applicationIdentifier        | desiredFileSize
        "ipa"    | "test.ipa" | applicationIdentifierIos     | 160
        "apk"    | "test.apk" | applicationIdentifierAndroid | 160
    }
}

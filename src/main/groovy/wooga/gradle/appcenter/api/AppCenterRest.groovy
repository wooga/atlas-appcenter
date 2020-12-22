package wooga.gradle.appcenter.api

import groovy.json.JsonSlurper
import org.apache.http.HttpResponse

class AppCenterRest {

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
            def responseBody = getResponseBody(response)
            String code = (responseBody["code"])
            String message = responseBody["message"]

            new AppCenterError(code, message)
        }
    }

    static final API_BASE_URL = "https://api.appcenter.ms/v0.1/apps"

    static Map getResponseBody(HttpResponse response) {
        def jsonSlurper = new JsonSlurper()
        jsonSlurper.parseText(response.entity.content.text) as Map
    }
}

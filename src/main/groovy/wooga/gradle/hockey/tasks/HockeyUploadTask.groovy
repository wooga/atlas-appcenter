/*
 * Copyright 2017 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *       http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *
 */

package wooga.gradle.hockey.tasks

import org.gradle.api.internal.ConventionTask
import org.gradle.api.tasks.Input
import org.gradle.api.tasks.TaskAction

import java.util.concurrent.Callable

class HockeyUploadTask extends ConventionTask {

    private Object apiToken

    @Input
    String getApiToken() {
        convertToString(apiToken)
    }

    void setApiToken(Object value) {
        apiToken = value
    }

    HockeyUploadTask apiToken(Object apiToken) {
        setApiToken(apiToken)
        this
    }

    private Object applicationIdentifier

    @Input
    String getApplicationIdentifier() {
        convertToString(applicationIdentifier)
    }

    void setApplicationIdentifier(Object value) {
        applicationIdentifier = value
    }

    HockeyUploadTask applicationIdentifier(Object applicationIdentifier) {
        setApplicationIdentifier(applicationIdentifier)
        this
    }

    private Object buildFile

    @Input
    File getBuildFile() {
        if(!buildFile) {
            return null
        }

        project.file(buildFile)
    }

    void setBuildFile(Object value) {
        buildFile = value
    }

    HockeyUploadTask buildFile(Object buildFile) {
        setBuildFile(buildFile)
        this
    }

    @TaskAction
    protected void upload() {
        project.exec {
            executable "curl"

            // TODO: make status and notify configurable, add changelog support
            args "-F", "status=2"
            args "-F", "notify=2"
            args "-F", "ipa=@" + getBuildFile()
            args "-H", "X-HockeyAppToken: " + getApiToken()
            args "https://rink.hockeyapp.net/api/2/apps/" + getApplicationIdentifier() + "/app_versions/upload"
            args "-v", "-f"
        }
    }

    private static String convertToString(Object value) {
        if (!value) {
            return null
        }

        if (value instanceof Callable) {
            value = ((Callable) value).call()
        }

        value.toString()
    }
}

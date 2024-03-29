/*
 * Copyright 2018-2022 Wooga GmbH
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *         http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package wooga.gradle.appcenter.tasks


import groovy.json.JsonOutput
import org.apache.http.client.HttpClient
import org.apache.http.client.config.RequestConfig
import org.apache.http.impl.client.HttpClientBuilder
import org.gradle.api.DefaultTask
import org.gradle.api.file.DirectoryProperty
import org.gradle.api.file.RegularFile
import org.gradle.api.provider.Provider
import org.gradle.api.tasks.Internal
import org.gradle.api.tasks.OutputFile
import org.gradle.api.tasks.TaskAction
import wooga.gradle.appcenter.api.AppCenterReleaseUploader
import wooga.gradle.appcenter.api.AppCenterRetryStrategy
import wooga.gradle.appcenter.error.RetryableException

import java.util.function.Supplier

class AppCenterUploadTask extends DefaultTask implements AppCenterTaskSpec {

    private final DirectoryProperty outputDir

    @Internal
    protected DirectoryProperty getOutputDir() {
        outputDir
    }

    @OutputFile
    final Provider<RegularFile> uploadVersionMetaData

    AppCenterUploadTask() {
        outputDir = project.objects.directoryProperty()
        outputDir.set(temporaryDir)
        uploadVersionMetaData = outputDir.file(owner.map({ owner -> "${owner}_${applicationIdentifier.get()}.json" }))
    }

    @TaskAction
    protected void upload() {
        int timeout = 30
        RequestConfig config = RequestConfig.custom()
            .setConnectTimeout(timeout * 1000)
            .setConnectionRequestTimeout(timeout * 1000)
            .setSocketTimeout(timeout * 1000).build()

        HttpClient client = HttpClientBuilder.create()
            .setDefaultRequestConfig(config)
            .useSystemProperties()
            .setServiceUnavailableRetryStrategy(new AppCenterRetryStrategy(retryCount.get(), retryTimeout.get().toInteger()))
            .build()

        AppCenterReleaseUploader uploader = new AppCenterReleaseUploader(client,
            owner.get(),
            applicationIdentifier.get(),
            apiToken.get())

        AppCenterReleaseUploader.DistributionSettings distributionSettings = uploader.distributionSettings
        AppCenterReleaseUploader.UploadVersion version = uploader.version

        version.buildNumber = buildNumber.getOrNull()
        version.buildVersion = buildVersion.getOrNull()

        distributionSettings.releaseNotes = releaseNotes.getOrElse("")
        distributionSettings.destinations = destinations.getOrElse([])
        distributionSettings.buildInfo = getBuildInfo()

        AppCenterReleaseUploader.UploadResult result = retry(retryCount.get(), retryTimeout.get()) {
            return uploader.upload(binary.get().asFile)
        }
        logger.info("published to AppCenter release: ${result.releaseID}")
        logger.info("download_url: ${result.downloadUrl}")
        logger.info("install_url: ${result.installUrl}")
        uploadVersionMetaData.get().asFile << JsonOutput.prettyPrint(JsonOutput.toJson(result.release))
    }

    <T> T retry(int maxRetries, long waitMs, Supplier<T> operation) {
        try {
            return operation.get()
        } catch(RetryableException e) {
            maxRetries--
            if(maxRetries > 0) {
                logger.warn("Retrying at task level, waiting for $waitMs ms. ${maxRetries} retries left")
                Thread.sleep(waitMs)
                return (T) retry(maxRetries, waitMs, operation)
            } else {
                throw new Exception("Operation exceed maximum amount of retries", e.cause)
            }
        }
    }
}

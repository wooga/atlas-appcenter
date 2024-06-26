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

package wooga.gradle.appcenter

import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.api.artifacts.PublishArtifact
import org.gradle.api.internal.provider.MissingValueException
import org.gradle.api.publish.plugins.PublishingPlugin
import wooga.gradle.appcenter.internal.DefaultAppCenterPluginExtension

import wooga.gradle.appcenter.tasks.AppCenterUploadTask

class AppCenterPlugin implements Plugin<Project> {

    static final String EXTENSION_NAME = "appCenter"
    static final String PUBLISH_APP_CENTER_TASK_NAME = "publishAppCenter"
    static final String PUBLISH_APP_CENTER_TASK_DESCRIPTION = "Upload binary to AppCenter."

    @Override
    void apply(Project project) {

        project.pluginManager.apply(PublishingPlugin.class)
        def extension = createAndConfigureExtension(project)
        createAndConfigureTasks(project, extension)
    }

    protected static DefaultAppCenterPluginExtension createAndConfigureExtension(Project project) {
        def extension = project.extensions.create(AppCenterPluginExtension, EXTENSION_NAME, DefaultAppCenterPluginExtension) as DefaultAppCenterPluginExtension

        extension.defaultDestinations.set(AppCenterConsts.defaultDestinations.getStringValueProvider(project)
            .map({
                it.split(',').collect { ["name": it.trim()] }
            })
        )

        extension.apiToken.convention(AppCenterConsts.apiToken.getStringValueProvider(project))
        extension.owner.convention(AppCenterConsts.owner.getStringValueProvider(project))
        extension.applicationIdentifier.convention(AppCenterConsts.applicationIdentifier.getStringValueProvider(project))
        extension.publishEnabled.convention(AppCenterConsts.publishEnabled.getBooleanValueProvider(project))
        extension.retryTimeout.convention(AppCenterConsts.retryTimeout.getValueProvider(project, {Long.parseLong(it)}))
        extension.retryCount.convention(AppCenterConsts.retryCount.getIntegerValueProvider(project))
        extension.binary.convention(AppCenterConsts.binary.getFileValueProvider(project))
        extension.releaseNotes.convention(AppCenterConsts.releaseNotes.getStringValueProvider(project))

        def metadataDir = project.layout.buildDirectory.dir("outputs/appCenter")
        def metadataFile = extension.owner
                .zip(extension.applicationIdentifier) { owner, appId -> "upload_${owner}_${appId}.json"}
                .zip(metadataDir) { fileName, dir -> dir.file(fileName) }
        extension.uploadResultMetadata.convention(AppCenterConsts.uploadResultMetadata.getFileValueProvider(project).orElse(metadataFile))
        return extension
    }

    private static void createAndConfigureTasks(Project project, DefaultAppCenterPluginExtension extension) {
        def tasks = project.tasks

        def publishAppCenter = tasks.register(PUBLISH_APP_CENTER_TASK_NAME, AppCenterUploadTask, { t ->
            t.group = PublishingPlugin.PUBLISH_TASK_GROUP
            t.description = PUBLISH_APP_CENTER_TASK_DESCRIPTION
        })

        tasks.withType(AppCenterUploadTask).configureEach { t ->
            t.buildVersion.convention(project.providers.provider({ project.version.toString() }))
            t.destinations.set(extension.defaultDestinations)
            t.applicationIdentifier.convention(extension.applicationIdentifier)
            t.apiToken.convention(extension.apiToken)
            t.owner.convention(extension.owner)
            t.retryCount.convention(extension.retryCount)
            t.retryTimeout.convention(extension.retryTimeout)
            t.binary.convention(extension.binary)
            t.releaseNotes.convention(extension.releaseNotes)
            if(extension.artifact.present && !t.binary.present) {
                def artifactFile = extension.artifact.map { PublishArtifact it ->
                    try {
                        return project.layout.projectDirectory.file(it.file.absolutePath)
                    } catch (MissingValueException ignore) { return null }
                }
                t.binary.convention(extension.binary.orElse(artifactFile))
                dependsOn(extension.artifact.map{ it.buildDependencies })
            }
            t.uploadVersionMetaData.convention(extension.uploadResultMetadata)
        }
        project.afterEvaluate {
            if (extension.isPublishEnabled().get()) {
                tasks.named(PublishingPlugin.PUBLISH_LIFECYCLE_TASK_NAME).configure { task ->
                    task.dependsOn(publishAppCenter)
                }
            }
        }
    }
}

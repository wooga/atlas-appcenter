/*
 * Copyright 2018-2021 Wooga GmbH
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

import org.gradle.api.Action
import org.gradle.api.Plugin
import org.gradle.api.Project
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

        def extension = create_and_configure_extension(project)

        def tasks = project.tasks

        def publishAppCenter = tasks.create(name: PUBLISH_APP_CENTER_TASK_NAME, type: AppCenterUploadTask, group: PublishingPlugin.PUBLISH_TASK_GROUP)
        publishAppCenter.description = PUBLISH_APP_CENTER_TASK_DESCRIPTION

        tasks.withType(AppCenterUploadTask, new Action<AppCenterUploadTask>() {
            @Override
            void execute(AppCenterUploadTask t) {
                t.buildVersion.convention(project.providers.provider({ project.version.toString() }))
                t.destinations.set(extension.defaultDestinations)
                t.applicationIdentifier.convention(extension.applicationIdentifier)
                t.apiToken.convention(extension.apiToken)
                t.owner.convention(extension.owner)
                t.retryCount.convention(extension.retryCount)
                t.retryTimeout.convention(extension.retryTimeout)
        }})

        project.afterEvaluate(new Action<Project>() {
            @Override
            void execute(Project _) {
                if (extension.isPublishEnabled().get()) {
                    def lifecyclePublishTask = tasks.getByName(PublishingPlugin.PUBLISH_LIFECYCLE_TASK_NAME)
                    lifecyclePublishTask.dependsOn(publishAppCenter)
                }
            }
        })
    }

    protected static AppCenterPluginExtension create_and_configure_extension(Project project) {
        def extension = project.extensions.create(AppCenterPluginExtension, EXTENSION_NAME, DefaultAppCenterPluginExtension, project)

        extension.defaultDestinations.set(project.provider({
            String rawValue = (project.properties[AppCenterConsts.DEFAULT_DESTINATIONS_OPTION]
                    ?: System.getenv()[AppCenterConsts.DEFAULT_DESTINATIONS_ENV_VAR]) as String

            if (rawValue) {
                return rawValue.split(',').collect { ["name": it.trim()] }
            }

            AppCenterConsts.defaultDestinations
        }))

        extension.apiToken.set(project.provider({
            (project.properties[AppCenterConsts.API_TOKEN_OPTION]
                    ?: System.getenv()[AppCenterConsts.API_TOKEN_ENV_VAR]) as String
        }))

        extension.owner.set(project.provider({
            (project.properties[AppCenterConsts.OWNER_OPTION]
                    ?: System.getenv()[AppCenterConsts.OWNER_ENV_VAR]) as String
        }))

        extension.applicationIdentifier.set(project.provider({
            (project.properties[AppCenterConsts.APPLICATION_IDENTIFIER_OPTION]
                    ?: System.getenv()[AppCenterConsts.APPLICATION_IDENTIFIER_ENV_VAR]) as String
        }))

        extension.publishEnabled.set(project.provider({
            String rawValue = (project.properties[AppCenterConsts.PUBLISH_ENABLED_OPTION]
                    ?: System.getenv()[AppCenterConsts.PUBLISH_ENABLED_ENV_VAR]) as String

            if (rawValue) {
                return (rawValue == "1" || rawValue.toLowerCase() == "yes" || rawValue.toLowerCase() == "y" || rawValue.toLowerCase() == "true")
            }
            AppCenterConsts.defaultPublishEnabled
        }))

        extension.retryTimeout.set(project.provider({
            String rawRetryTimout = (project.properties[AppCenterConsts.RETRY_TIMEOUT_OPTION]
                    ?: System.getenv()[AppCenterConsts.RETRY_TIMEOUT_ENV_VAR]) as String

            (rawRetryTimout) ? Long.parseLong(rawRetryTimout) : AppCenterConsts.defaultRetryTimeout
        }))

        extension.retryCount.set(project.provider({
            String rawRetryCount = (project.properties[AppCenterConsts.RETRY_COUNT_OPTION]
                    ?: System.getenv()[AppCenterConsts.RETRY_COUNT_ENV_VAR]) as String

            (rawRetryCount) ? Integer.parseInt(rawRetryCount) : AppCenterConsts.defaultRetryCount
        }))

        extension
    }
}

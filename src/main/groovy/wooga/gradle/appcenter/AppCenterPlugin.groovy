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

        def publishAppCenter = tasks.register(PUBLISH_APP_CENTER_TASK_NAME, AppCenterUploadTask,{ t ->
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
        }
        project.afterEvaluate {
            if (extension.isPublishEnabled().get()) {
                tasks.named(PublishingPlugin.PUBLISH_LIFECYCLE_TASK_NAME).configure { task ->
                    task.dependsOn(publishAppCenter)
                }
            }
        }
    }

    protected static AppCenterPluginExtension create_and_configure_extension(Project project) {
        def extension = project.extensions.create(AppCenterPluginExtension, EXTENSION_NAME, DefaultAppCenterPluginExtension, project)

//        extension.defaultDestinations.convention(AppCenterConventions.defaultDestinations.getValueProvider(project,
//            {
//                def result = (List<Map<String, String>> )it
//                if (result == null){
//                    result =it.split(',').collect { ["name": it.trim()] }
//                }
//                result
//            }))

        extension.defaultDestinations.set(project.provider({
            String rawValue = (project.properties[AppCenterConventions.DEFAULT_DESTINATIONS_OPTION]
                    ?: System.getenv()[AppCenterConventions.DEFAULT_DESTINATIONS_ENV_VAR]) as String

            if (rawValue) {
                return rawValue.split(',').collect { ["name": it.trim()] }
            }

            AppCenterConventions.defaultDestinations
        }))

        extension.apiToken.convention(AppCenterConventions.apiToken.getStringValueProvider(project))
//        extension.apiToken.set(project.provider({
//            (project.properties[AppCenterConventions.API_TOKEN_OPTION]
//                    ?: System.getenv()[AppCenterConventions.API_TOKEN_ENV_VAR]) as String
//        }))

        extension.owner.convention(AppCenterConventions.owner.getStringValueProvider(project))
//        extension.owner.set(project.provider({
//            (project.properties[AppCenterConventions.OWNER_OPTION]
//                    ?: System.getenv()[AppCenterConventions.OWNER_ENV_VAR]) as String
//        }))

        extension.applicationIdentifier.convention(AppCenterConventions.applicationIdentifier.getStringValueProvider(project))
//        extension.applicationIdentifier.set(project.provider({
//            (project.properties[AppCenterConventions.APPLICATION_IDENTIFIER_OPTION]
//                    ?: System.getenv()[AppCenterConventions.APPLICATION_IDENTIFIER_ENV_VAR]) as String
//        }))

        extension.publishEnabled.convention(AppCenterConventions.publishEnabled.getBooleanValueProvider(project))
//        extension.publishEnabled.set(project.provider({
//            String rawValue = (project.properties[AppCenterConventions.PUBLISH_ENABLED_OPTION]
//                    ?: System.getenv()[AppCenterConventions.PUBLISH_ENABLED_ENV_VAR]) as String
//
//            if (rawValue) {
//                return (rawValue == "1" || rawValue.toLowerCase() == "yes" || rawValue.toLowerCase() == "y" || rawValue.toLowerCase() == "true")
//            }
//            AppCenterConventions.defaultPublishEnabled
//        }))

        extension.retryTimeout.convention(AppCenterConventions.retryTimeout.getValueProvider(project, {Long.parseLong(it)}))
//        extension.retryTimeout.set(project.provider({
//            String rawRetryTimout = (project.properties[AppCenterConventions.RETRY_TIMEOUT_OPTION]
//                    ?: System.getenv()[AppCenterConventions.RETRY_TIMEOUT_ENV_VAR]) as String
//
//            (rawRetryTimout) ? Long.parseLong(rawRetryTimout) : AppCenterConventions.defaultRetryTimeout
//        }))

        extension.retryCount.convention(AppCenterConventions.retryCount.getIntegerValueProvider(project))
//        extension.retryCount.set(project.provider({
//            String rawRetryCount = (project.properties[AppCenterConventions.RETRY_COUNT_OPTION]
//                    ?: System.getenv()[AppCenterConventions.RETRY_COUNT_ENV_VAR]) as String
//
//            (rawRetryCount) ? Integer.parseInt(rawRetryCount) : AppCenterConventions.defaultRetryCount
//        }))

        extension
    }
}

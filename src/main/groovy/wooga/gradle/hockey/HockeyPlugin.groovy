/*
 * Copyright 2018 the original author or authors.
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
 */

package wooga.gradle.hockey

import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.api.publish.plugins.PublishingPlugin
import wooga.gradle.hockey.tasks.HockeyUploadTask

class HockeyPlugin implements Plugin<Project> {

    static final String TASK_NAME = "publishHockey"
    static final String TASK_DESCRIPTION = "Upload binary to HockeyApp."

    @Override
    void apply(Project project) {

        project.pluginManager.apply(PublishingPlugin.class)

        def tasks = project.tasks

        def publishHockey = tasks.create(name: TASK_NAME, type: HockeyUploadTask, group: PublishingPlugin.PUBLISH_TASK_GROUP)
        publishHockey.description = TASK_DESCRIPTION

        def lifecyclePublishTask = tasks.getByName(PublishingPlugin.PUBLISH_LIFECYCLE_TASK_NAME)
        lifecyclePublishTask.dependsOn(publishHockey)
    }
}

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

package wooga.gradle.appcenter

import nebula.test.ProjectSpec
import org.gradle.api.DefaultTask
import org.gradle.api.publish.plugins.PublishingPlugin
import org.gradle.api.tasks.TaskProvider
import spock.lang.Unroll
import wooga.gradle.appcenter.tasks.AppCenterUploadTask

class AppCenterPluginSpec extends ProjectSpec {
    public static final String PLUGIN_NAME = 'net.wooga.appcenter'

    def 'applies plugin'() {
        given:
        assert !project.plugins.hasPlugin(PLUGIN_NAME)

        when:
        project.plugins.apply(PLUGIN_NAME)

        then:
        project.plugins.hasPlugin(PLUGIN_NAME)
    }

    @Unroll("creates the task #taskName")
    def 'creates task #taskName of type #taskType'(String taskName, Class taskType) {
        given:
        assert !project.plugins.hasPlugin(PLUGIN_NAME)
        assert !project.tasks.findByName(taskName)

        when:
        project.plugins.apply(PLUGIN_NAME)
        def task
        project.afterEvaluate {
            task = project.tasks.findByName(taskName)
        }

        then:
        project.evaluate()
        taskType.isInstance(task)

        where:
        taskName                                     | taskType
        PublishingPlugin.PUBLISH_LIFECYCLE_TASK_NAME | DefaultTask
        "publishAppCenter"                           | AppCenterUploadTask
    }

    @Unroll
    def 'publish task depends on #taskName'() {
        given:
        assert !project.plugins.hasPlugin(PLUGIN_NAME)

        when:
        project.plugins.apply(PLUGIN_NAME)
        def publishTaskProvider
        project.afterEvaluate {
            publishTaskProvider = project.tasks.named(PublishingPlugin.PUBLISH_LIFECYCLE_TASK_NAME)
        }

        then:
        project.evaluate()
        def publish = project.tasks.named(taskName)
        def publishTask = publishTaskProvider.get()
        publishTask.getDependsOn().contains(publish)

        where:
        taskName           | _
        "publishAppCenter" | _
    }
}

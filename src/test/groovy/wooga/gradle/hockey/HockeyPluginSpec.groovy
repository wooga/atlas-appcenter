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

import nebula.test.ProjectSpec
import org.gradle.api.DefaultTask
import org.gradle.api.publish.plugins.PublishingPlugin
import wooga.gradle.hockey.tasks.HockeyUploadTask
import spock.lang.Unroll

class HockeyPluginSpec extends ProjectSpec {
    public static final String PLUGIN_NAME = 'net.wooga.hockey'
    public static final String TASK_NAME = 'publishHockey'

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
        taskName                                         | taskType
        PublishingPlugin.PUBLISH_LIFECYCLE_TASK_NAME     | DefaultTask
        TASK_NAME                                        | HockeyUploadTask
    }

    def 'publish task depends on publishHockey'() {
        given:
        assert !project.plugins.hasPlugin(PLUGIN_NAME)

        when:
        project.plugins.apply(PLUGIN_NAME)
        def publishTask
        project.afterEvaluate {
            publishTask = project.tasks.findByName(PublishingPlugin.PUBLISH_LIFECYCLE_TASK_NAME)
        }

        then:
        project.evaluate()
        def publishHockey = project.tasks.findByName(TASK_NAME)
        publishTask.getDependsOn().contains(publishHockey)
    }
}

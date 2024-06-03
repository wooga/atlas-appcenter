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
import org.gradle.api.Task
import org.gradle.api.publish.plugins.PublishingPlugin
import org.gradle.api.tasks.Copy
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

    def "publishAppCenter task depends on artifact task if artifact is present and no binary was set"() {
        given: "no plugin has been applied yet"
        assert !project.plugins.hasPlugin(PLUGIN_NAME)

        and: "creates artifact generator task"
        def artifactFile = new File(projectDir, "artifactDir/artifact")
        def artifactTask = project.tasks.register("testArtifactTask", Copy) {
            from new File(projectDir, "artifact")
            into artifactFile.parentFile
            rename ".*", artifactFile.name
        }

        and: "stores artifact"
        def artifact = project.with {
            configurations.create("archives")
            artifacts.add("archives", artifactTask.map {
                it.outputs.files.find {it == artifactFile}
            }) {
                it.type = "myartifact"
            }
            return configurations.archives.artifacts.matching { it.type == "myartifact" }.first()
        }

        when: "apply plugin"
        assert project.plugins.apply(PLUGIN_NAME)
        and: "add artifact to extension"
        def appCenter = project.extensions.findByType(AppCenterPluginExtension)
        appCenter.artifact(artifact)
        and: "evaluated project"
        project.evaluate()
        then: "appCenterPublish depends on task used to generate artifact"
        def appCenterPublish = project.tasks.named(AppCenterPlugin.PUBLISH_APP_CENTER_TASK_NAME, AppCenterUploadTask).get()
        def dependencies = appCenterPublish.taskDependencies.getDependencies(appCenterPublish)
        dependencies.contains(artifactTask.get())
    }

    def "publishAppCenter task depends on task used to generate appCenter binary"() {
        given: "no plugin has been applied yet"
        assert !project.plugins.hasPlugin(PLUGIN_NAME)
        and: "a binary generator task"
        def binary = new File(projectDir, "artifactDir/artifact")
        def binaryTask = project.tasks.register("testBinaryTask", Copy) {
            from new File(projectDir, "artifact")
            into binary.parentFile
            rename ".*", binary.name
        }
        def binaryProvider = project.layout.file(binaryTask.map {
            it.outputs.files.find {it == binary} as File
        })

        when: "plugin is applied"
        assert project.plugins.apply(PLUGIN_NAME)
        and: "extension binary property is set"
        def appCenter = project.extensions.findByType(AppCenterPluginExtension)
        appCenter.binary = binaryProvider
        and: "evaluated project"
        project.evaluate()

        then: "appCenterPublish depends on task used to generate artifact"
        def appCenterPublish = project.tasks.named(AppCenterPlugin.PUBLISH_APP_CENTER_TASK_NAME, AppCenterUploadTask).get()
        def dependencies = appCenterPublish.taskDependencies.getDependencies(appCenterPublish)
        dependencies.contains(binaryTask.get())

    }
}

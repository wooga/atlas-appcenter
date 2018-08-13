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

        // TODO: add publish depends on publishHockey

//        project.pluginManager.apply(BasePlugin.class)
//        project.pluginManager.apply(UnityPlugin.class)
//
//        def extension = project.extensions.create(UnityBuildPluginExtension, EXTENSION_NAME, DefaultUnityBuildPluginExtension, project)
//        def exportLifecycleTask = project.tasks.create(EXPORT_ALL_TASK_NAME)
//
//        def baseLifecycleTaskNames = [LifecycleBasePlugin.ASSEMBLE_TASK_NAME,
//                                      LifecycleBasePlugin.CHECK_TASK_NAME,
//                                      LifecycleBasePlugin.BUILD_TASK_NAME,
//                                      PublishingPlugin.PUBLISH_LIFECYCLE_TASK_NAME]
//
//        project.tasks.withType(UnityBuildPlayerTask, new Action<UnityBuildPlayerTask>() {
//            @Override
//            void execute(UnityBuildPlayerTask task) {
//                def conventionMapping = task.getConventionMapping()
//                conventionMapping.map("exportMethodName", {extension.getExportMethodName()})
//                conventionMapping.map("buildEnvironment", {extension.getDefaultEnvironment()})
//                conventionMapping.map("buildPlatform", {extension.getDefaultPlatform()})
//                conventionMapping.map("toolsVersion", {extension.getToolsVersion()})
//                conventionMapping.map("outputDirectoryBase", {extension.getOutputDirectoryBase()})
//            }
//        })
//
//        project.tasks.maybeCreate(PublishingPlugin.PUBLISH_LIFECYCLE_TASK_NAME)
//
//        project.afterEvaluate {
//            extension.platforms.each { String platform ->
//                def platformLifecycleTask = project.tasks.create("export${platform.capitalize()}")
//
//                extension.environments.each { String environment ->
//                    def environmentLifecycleTask = project.tasks.maybeCreate("export${environment.capitalize()}")
//
//                    def exportTask = project.tasks.create("export${platform.capitalize()}${environment.capitalize()}", UnityBuildPlayerTask, new Action<UnityBuildPlayerTask>() {
//                        @Override
//                        void execute(UnityBuildPlayerTask unityBuildPlayerTask) {
//                            unityBuildPlayerTask.buildEnvironment(environment)
//                            unityBuildPlayerTask.buildPlatform(platform)
//                        }
//                    })
//
//                    FileCollection exportInitScripts = project.fileTree(project.projectDir) { it.include('exportInit.gradle') }
//                    List<String> args = []
//                    args << "-Pexport.buildDirBase=../buildCache" << "--project-cache-dir=../buildCache/.gradle"
//
//                    if(exportInitScripts.size() > 0) {
//                        args << "--init-script=${exportInitScripts.files.first().path}".toString()
//                    }
//
//                    baseLifecycleTaskNames.each { String taskName ->
//                        def t = project.tasks.maybeCreate("${taskName}${platform.capitalize()}${environment.capitalize()}", GradleBuild)
//                        t.with {
//                            group = environment.capitalize()
//                            dependsOn exportTask
//                            dir = exportTask.getOutputDirectory()
//                            buildArguments = args
//                            tasks = [taskName]
//                        }
//                    }
//
//                    platformLifecycleTask.dependsOn exportTask
//                    environmentLifecycleTask.dependsOn exportTask
//                    exportLifecycleTask.dependsOn environmentLifecycleTask
//                }
//
//                exportLifecycleTask.dependsOn platformLifecycleTask
//            }
//
//            baseLifecycleTaskNames.each {
//                project.tasks[it].dependsOn project.tasks[getDefaultTaskNameFor(extension, it)]
//            }
//        }
    }
}

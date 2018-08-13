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
import spock.lang.Unroll

class HockeyPluginSpec extends ProjectSpec {
//    public static final String PLUGIN_NAME = 'net.wooga.build-unity'
//
//    def 'Creates the [unity] extension'() {
//        given:
//        assert !project.plugins.hasPlugin(PLUGIN_NAME)
//        assert !project.extensions.findByName(UnityBuildPlugin.EXTENSION_NAME)
//
//        when:
//        project.plugins.apply(PLUGIN_NAME)
//
//        then:
//        def extension = project.extensions.findByName(UnityBuildPlugin.EXTENSION_NAME)
//        extension instanceof DefaultUnityBuildPluginExtension
//    }
//
//    @Unroll("creates the task #taskName")
//    def 'Creates needed tasks'(String taskName, Class taskType) {
//        given:
//        assert !project.plugins.hasPlugin(PLUGIN_NAME)
//        assert !project.tasks.findByName(taskName)
//
//        when:
//        project.plugins.apply(PLUGIN_NAME)
//        def task
//        project.afterEvaluate {
//            task = project.tasks.findByName(taskName)
//        }
//
//        then:
//        project.evaluate()
//        taskType.isInstance(task)
//
//        where:
//        taskName                              | taskType
//        UnityBuildPlugin.EXPORT_ALL_TASK_NAME | DefaultTask
//        "exportIOSCi"                         | UnityBuildPlayerTask
//        "exportAndroidCi"                     | UnityBuildPlayerTask
//        "exportWebGLCi"                       | UnityBuildPlayerTask
//        "exportIOSStaging"                    | UnityBuildPlayerTask
//        "exportAndroidStaging"                | UnityBuildPlayerTask
//        "exportWebGLStaging"                  | UnityBuildPlayerTask
//        "exportIOSProduction"                 | UnityBuildPlayerTask
//        "exportAndroidProduction"             | UnityBuildPlayerTask
//        "exportWebGLProduction"               | UnityBuildPlayerTask
//        "exportCi"                            | DefaultTask
//        "exportStaging"                       | DefaultTask
//        "exportProduction"                    | DefaultTask
//        "exportIOS"                           | DefaultTask
//        "exportAndroid"                       | DefaultTask
//        "exportWebGL"                         | DefaultTask
//        "publish"                             | DefaultTask
//        "publishAndroidCi"                    | DefaultTask
//        "assemble"                            | DefaultTask
//        "assembleAndroidCi"                   | DefaultTask
//        "build"                               | DefaultTask
//        "buildAndroidCi"                      | DefaultTask
//        "check"                               | DefaultTask
//        "checkAndroidCi"                      | DefaultTask
//    }
//
//    @Unroll
//    def 'adds pluginToAdd #pluginToAdd'(String pluginToAdd) {
//        given:
//        assert !project.plugins.hasPlugin(PLUGIN_NAME)
//        assert !project.plugins.hasPlugin(pluginToAdd)
//
//        when:
//        project.plugins.apply(PLUGIN_NAME)
//
//        then:
//        project.plugins.hasPlugin(pluginToAdd)
//
//        where:
//        pluginToAdd << ['base', 'net.wooga.unity']
//    }
}

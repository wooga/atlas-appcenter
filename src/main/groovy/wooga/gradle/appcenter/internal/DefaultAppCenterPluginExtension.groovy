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

package wooga.gradle.appcenter.internal

import org.gradle.api.Project
import org.gradle.api.artifacts.PublishArtifact
import org.gradle.api.provider.Property
import org.gradle.api.provider.Provider
import wooga.gradle.appcenter.AppCenterPluginExtension

class DefaultAppCenterPluginExtension implements AppCenterPluginExtension {

    private final Project project

    final Property<? extends PublishArtifact> artifact = objects.property(PublishArtifact)

    DefaultAppCenterPluginExtension(Project project) {
        this.project = project
    }

    @Override
    void artifact(Object fileLike, Object dependency) {
        def proj = this.project
        def apks = proj.configurations.create('appCenterBinaries')
        Provider fileLikeProvider
        if(fileLike instanceof Provider) {
            fileLikeProvider = fileLike
        } else {
            fileLikeProvider = project.provider { fileLike }
        }

        def artifact = fileLikeProvider.map { file ->
            def artifactsSet = apks.artifacts
            if (artifactsSet.size() > 0) {
                return artifactsSet.first()
            } else {
                return proj.artifacts.add(apks.name, file) {
                    type 'appCenterArtifact'
                    builtBy dependency
                }
            }
        } as Provider<PublishArtifact>
        this.artifact(artifact)
    }

    @Override
    void artifact(Provider<PublishArtifact> artifact) {
        this.artifact.set(artifact)
    }

    @Override
    void artifact(PublishArtifact artifact) {
        this.artifact.set(project.provider{ artifact })
    }
}

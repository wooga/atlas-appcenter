/*
 * Copyright 2019-2021 Wooga GmbH
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
 *
 */

package wooga.gradle.appcenter

import org.gradle.api.artifacts.PublishArtifact
import org.gradle.api.file.RegularFile
import org.gradle.api.file.RegularFileProperty
import org.gradle.api.provider.ListProperty
import org.gradle.api.provider.Property
import org.gradle.api.provider.Provider
import org.gradle.api.tasks.InputFile
import org.gradle.api.tasks.Optional
import org.gradle.api.tasks.OutputFile

trait AppCenterPluginExtension implements AppCenterSpec {

    abstract void artifact(Provider<PublishArtifact> artifact);
    abstract void artifact(PublishArtifact artifact);

    private final Property<String> releaseNotes = objects.property(String)

    Property<String> getReleaseNotes() {
        releaseNotes
    }

    void setReleaseNotes(Provider<String> value) {
        releaseNotes.set(value)
    }

    private final RegularFileProperty binary = objects.fileProperty()

    RegularFileProperty getBinary() {
        binary
    }

    void setBinary(Provider<RegularFile> value) {
        binary.set(value)
    }

    void setBinary(File value) {
        binary.set(value)
    }


    private final RegularFileProperty uploadResultMetadata = objects.fileProperty()

    RegularFileProperty getUploadResultMetadata() {
        return uploadResultMetadata
    }

    void setUploadResultMetadata(Provider<RegularFile> uploadResultMetadata) {
        this.uploadResultMetadata.set(uploadResultMetadata)
    }

    void setUploadResultMetadata(RegularFile uploadResultMetadata) {
        this.uploadResultMetadata.set(uploadResultMetadata)
    }

    void setUploadResultMetadata(File uploadResultMetadata) {
        this.uploadResultMetadata.set(uploadResultMetadata)
    }


    // TODO: Refactor, deprecate to use `destinations` instead?
    private final ListProperty<Map<String, String>> defaultDestinations = objects.listProperty(Map)

    ListProperty<Map<String, String>> getDefaultDestinations() {
        defaultDestinations
    }

    void setDefaultDestinations(Iterable<String> value) {
        defaultDestinations.set(value.collect {["name": it]})
    }

    void setDefaultDestinations(String... destinations) {
        defaultDestinations.set(destinations.collect {["name": it]})
    }

    void defaultDestination(String name) {
        defaultDestinations.add(["name": name])
    }

    void defaultDestination(Iterable<String> destinations) {
        defaultDestinations.addAll(destinations.collect {["name": it]})
    }

    void defaultDestination(String... destinations) {
        defaultDestinations.addAll(destinations.collect {["name": it]})
    }

    void defaultDestinationId(String id) {
        defaultDestinations.add(["id": id])
    }

    private final Property<Boolean> publishEnabled = objects.property(Boolean)

    Property<Boolean> getPublishEnabled() {
        publishEnabled
    }

    Property<Boolean> isPublishEnabled() {
        return publishEnabled
    }

    void setPublishEnabled(boolean enabled) {
        this.publishEnabled.set(enabled)
    }
}

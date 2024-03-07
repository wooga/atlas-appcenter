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

package wooga.gradle.appcenter.tasks

import org.gradle.api.Action
import org.gradle.api.file.RegularFile
import org.gradle.api.file.RegularFileProperty
import org.gradle.api.provider.ListProperty
import org.gradle.api.provider.Property
import org.gradle.api.provider.Provider
import org.gradle.api.tasks.Input
import org.gradle.api.tasks.InputFile
import org.gradle.api.tasks.Nested
import org.gradle.api.tasks.Optional
import wooga.gradle.appcenter.AppCenterSpec
import wooga.gradle.appcenter.api.AppCenterBuildInfo


import static org.gradle.util.ConfigureUtil.configureUsing

trait AppCenterTaskSpec extends AppCenterSpec {

    private final Property<String> buildVersion = objects.property(String)

    @Input
    Property<String> getBuildVersion() {
        buildVersion
    }

    void setBuildVersion(Provider<String> value) {
        buildVersion.set(value)
    }

    private final Property<String> buildNumber = objects.property(String)

    @Optional
    @Input
    Property<String> getBuildNumber() {
        buildNumber
    }

    void setBuildNumber(Provider<String> value) {
        buildNumber.set(value)
    }

    private final Property<String> releaseNotes = objects.property(String)

    @Optional
    @Input
    Property<String> getReleaseNotes() {
        releaseNotes
    }

    void setReleaseNotes(Provider<String> value) {
        releaseNotes.set(value)
    }

    private final RegularFileProperty binary = objects.fileProperty()

    @InputFile
    RegularFileProperty getBinary() {
        binary
    }

    void setBinary(Provider<RegularFile> value) {
        binary.set(value)
    }

    private AppCenterBuildInfo buildInfo = new AppCenterBuildInfo()

    @Nested
    AppCenterBuildInfo getBuildInfo() {
        buildInfo
    }

    void buildInfo(Closure closure) {
        buildInfo(configureUsing(closure))
    }

    void buildInfo(Action<? super AppCenterBuildInfo> action) {
        action.execute(buildInfo)
    }

    private final ListProperty<Map<String, String>> destinations = objects.listProperty(Map)

    @Optional
    @Input
    ListProperty<Map<String, String>> getDestinations() {
        destinations
    }

    void setDestinations(Iterable<String> value) {
        destinations.set(value.collect { ["name": it] })
    }

    void setDestinations(String... value) {
        destinations.set(value.collect { ["name": it] })
    }

    void destination(String name) {
        destinations.add(["name": name])
    }

    void destination(Iterable<String> destinations) {
        this.destinations.addAll(destinations.collect { ["name": it] })
    }

    void destination(String... destinations) {
        this.destinations.addAll(destinations.collect { ["name": it] })
    }

    void destinationId(String id) {
        destinations.add(["id": id])
    }
}

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

import com.wooga.gradle.BaseSpec
import org.gradle.api.provider.ListProperty
import org.gradle.api.provider.Property
import org.gradle.api.provider.Provider
import org.gradle.api.tasks.Input
import org.gradle.api.tasks.Internal
import org.gradle.api.tasks.Optional

trait AppCenterSpec extends BaseSpec {

    @Input
    private final Property<String> apiToken = objects.property(String)

    Property<String> getApiToken() {
        apiToken
    }

    void setApiToken(Provider<String> value) {
        apiToken.set(value)
    }

    void setApiToken(String value) {
        apiToken.set(value)
    }

    @Input
    private final Property<String> owner = objects.property(String)

    Property<String> getOwner() {
        owner
    }

    void setOwner(Provider<String> value) {
        owner.set(value)
    }

    void setOwner(String value) {
        owner.set(value)
    }

    @Input
    private final Property<String> applicationIdentifier = objects.property(String)

    Property<String> getApplicationIdentifier() {
        applicationIdentifier
    }

    void setApplicationIdentifier(Provider<String> value) {
        applicationIdentifier.set(value)
    }

    void setApplicationIdentifier(String value) {
        applicationIdentifier.set(value)
    }

    private final Property<Long> retryTimeout = objects.property(Long)

    @Internal
    Property<Long> getRetryTimeout() {
        retryTimeout
    }

    void setRetryTimeout(Long value) {
        this.retryTimeout.set(value)
    }

    private final Property<Integer> retryCount = objects.property(Integer)

    @Internal
    Property<Integer> getRetryCount() {
        retryCount
    }

    void setRetryCount(Integer value) {
        retryCount.set(value)
    }
}

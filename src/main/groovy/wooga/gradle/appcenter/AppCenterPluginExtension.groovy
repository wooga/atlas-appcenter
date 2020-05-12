/*
 * Copyright 2019 Wooga GmbH
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

import org.gradle.api.provider.ListProperty
import org.gradle.api.provider.Property

interface AppCenterPluginExtension {
    Property<String> getApiToken()
    void setApiToken(String value)
    void apiToken(String value)

    Property<String> getOwner()
    void setOwner(String value)
    void owner(String value)
    Property<String> getApplicationIdentifier()

    void setApplicationIdentifier(String value)
    void applicationIdentifier(String value)

    ListProperty<Map<String, String>> getDefaultDestinations()

    void setDefaultDestinations(Iterable<String> value)
    void defaultDestination(String name)
    void defaultDestination(Iterable<String> destinations)
    void defaultDestination(String... destinations)
    void defaultDestinationId(String id)

    Property<Boolean> getPublishEnabled()
    Property<Boolean> isPublishEnabled()
    void setPublishEnabled(final boolean enabled)
}

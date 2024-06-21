/*
 * Copyright 2019-2022 Wooga GmbH
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

import com.wooga.gradle.PropertyLookup

class AppCenterConsts {

    static PropertyLookup apiToken = new PropertyLookup("APP_CENTER_API_TOKEN", "appCenter.apiToken", null)
    static PropertyLookup owner = new PropertyLookup("APP_CENTER_OWNER", "appCenter.owner", null)
    static PropertyLookup applicationIdentifier = new PropertyLookup("APP_CENTER_APPLICATION_IDENTIFIER", "appCenter.applicationIdentifier", null)

    static PropertyLookup defaultDestinations = new PropertyLookup("APP_CENTER_DEFAULT_DESTINATIONS", "appCenter.defaultDestinations", "Collaborators")

    static PropertyLookup publishEnabled = new PropertyLookup("APP_CENTER_PUBLISH_ENABLED", "appCenter.publishEnabled", true)
    static PropertyLookup retryTimeout = new PropertyLookup("APP_CENTER_RETRY_TIMEOUT", "appCenter.retryTimeout", 1000 * 60)
    static PropertyLookup retryCount = new PropertyLookup("APP_CENTER_RETRY_COUNT", "appCenter.retryCount", 30)
    static PropertyLookup binary = new PropertyLookup("APP_CENTER_BINARY", "appCenter.binary", null)
    static PropertyLookup releaseNotes = new PropertyLookup("APP_CENTER_RELEASE_NOTES", "appCenter.releaseNotes", null)
    static PropertyLookup uploadResultMetadata = new PropertyLookup("APP_CENTER_UPLOAD_RESULT_METADATA", "appCenter.uploadResultMetadata", null)
}

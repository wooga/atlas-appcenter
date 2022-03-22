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

import com.sun.org.apache.xpath.internal.operations.Bool
import com.wooga.gradle.PropertyLookup

class AppCenterConventions {

//    static String API_TOKEN_OPTION = "appCenter.apiToken"
//    static String API_TOKEN_ENV_VAR = "APP_CENTER_API_TOKEN"
    static PropertyLookup apiToken = new PropertyLookup("APP_CENTER_API_TOKEN", "appCenter.apiToken", null)

//    static String OWNER_OPTION = "appCenter.owner"
//    static String OWNER_ENV_VAR = "APP_CENTER_OWNER"
    static PropertyLookup owner = new PropertyLookup("APP_CENTER_OWNER", "appCenter.owner", null)

//    static String APPLICATION_IDENTIFIER_OPTION = "appCenter.applicationIdentifier"
//    static String APPLICATION_IDENTIFIER_ENV_VAR = "APP_CENTER_APPLICATION_IDENTIFIER"
    static PropertyLookup applicationIdentifier = new PropertyLookup("APP_CENTER_APPLICATION_IDENTIFIER", "appCenter.applicationIdentifier", null)

    // TODO: Change when a supported provider for this use case is implemented
    static List<Map<String, String>> defaultDestinations = [["name": "Collaborators"]]
    static String DEFAULT_DESTINATIONS_OPTION = "appCenter.defaultDestinations"
    static String DEFAULT_DESTINATIONS_ENV_VAR = "APP_CENTER_DEFAULT_DESTINATIONS"
    //static PropertyLookup defaultDestinations = new PropertyLookup("APP_CENTER_DEFAULT_DESTINATIONS", "appCenter.defaultDestinations",  [["name": "Collaborators"]])

//    static Boolean defaultPublishEnabled = true
//    static String PUBLISH_ENABLED_OPTION = "appCenter.publishEnabled"
//    static String PUBLISH_ENABLED_ENV_VAR = "APP_CENTER_PUBLISH_ENABLED"
    static PropertyLookup publishEnabled = new PropertyLookup("APP_CENTER_PUBLISH_ENABLED", "appCenter.publishEnabled", true)

//    static Long defaultRetryTimeout = 1000 * 60
//    static String RETRY_TIMEOUT_OPTION = "appCenter.retryTimeout"
//    static String RETRY_TIMEOUT_ENV_VAR = "APP_CENTER_RETRY_TIMEOUT"
    static PropertyLookup retryTimeout = new PropertyLookup("APP_CENTER_RETRY_TIMEOUT", "appCenter.retryTimeout", 1000 * 60)

//    static Integer defaultRetryCount = 30
//    static String RETRY_COUNT_OPTION = "appCenter.retryCount"
//    static String RETRY_COUNT_ENV_VAR = "APP_CENTER_RETRY_COUNT"
    static PropertyLookup retryCount = new PropertyLookup("APP_CENTER_RETRY_COUNT", "appCenter.retryCount", 30)
}

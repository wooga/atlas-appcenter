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

package wooga.gradle.hockey.api.internal

import groovy.json.JsonSlurper
import wooga.gradle.hockey.api.AppVersion

/**
 * "title": "HockeyTest",
 * "appsize": 1598428,
 * "timestamp": 1308930206,
 * "device_family": "iPhone/iPod",
 * "minimum_os_version": "4.0",
 * "notes": "<p>Some new features and fixed bugs.</p>",
 * "version": "8",
 * "shortversion": "1.0",
 * "status": 2,
 * "config_url": "https://rink.hockeyapp.net/manage/apps/123/app_versions/8",
 * "public_url": "https://rink.hockeyapp.net/apps/1234567890abcdef1234567890abcdef"
 */
class DefaultAppVersion implements AppVersion{
    String id
    String title
    String appId

    int appSize
    int timespamp
    String deviceFamily
    String notes
    String version
    String shortVersion
    int status
    String configUrl
    String publicUrl
    String buildUrl

    protected DefaultAppVersion() {}

    DefaultAppVersion(File file) {
        this(file.newInputStream())
    }

    DefaultAppVersion(InputStream inputStream) {
        this(new JsonSlurper().parse(inputStream))
    }

    protected DefaultAppVersion(Object json) {
        id = json["id"] as String
        title = json["title"] as String
        appId = json["app_id"] as String
        appSize = json["appsize"] as int
        timespamp = json["timestamp"] as int
        deviceFamily = json["device_family"] as String
        notes = json["notes"] as String
        version = json["version"] as String
        shortVersion = json["shortversion"] as String
        status = json["status"] as int
        configUrl = json["config_url"] as String
        publicUrl = json["public_url"] as String
        buildUrl = json["build_url"] as String
    }
}

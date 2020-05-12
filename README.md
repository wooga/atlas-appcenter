atlas-appcenter
=================

[![Gradle Plugin ID](https://img.shields.io/badge/gradle-net.wooga.build--unity-brightgreen.svg?style=flat-square)](https://plugins.gradle.org/plugin/net.wooga.build-unity)
[![Build Status](https://wooga-shields.herokuapp.com/jenkins/s/https/atlas-jenkins.wooga.com/job/atlas-plugins/job/atlas-appcenter/job/master.svg?style=flat-square)]()
[![Build Status](https://img.shields.io/travis/wooga/atlas-appcenter/master.svg?style=flat-square)](https://travis-ci.org/wooga/atlas-appcenter)
[![Coveralls Status](https://img.shields.io/coveralls/wooga/atlas-appcenter/master.svg?style=flat-square)](https://coveralls.io/github/wooga/atlas-appcenter?branch=master)
[![Apache 2.0](https://img.shields.io/badge/license-Apache%202-blue.svg?style=flat-square)](https://raw.githubusercontent.com/wooga/atlas-appcenter/master/LICENSE)
[![GitHub tag](https://img.shields.io/github/tag/wooga/atlas-appcenter.svg?style=flat-square)]()
[![GitHub release](https://img.shields.io/github/release/wooga/atlas-appcenter.svg?style=flat-square)]()

This plugin is work in progress.

# Applying the plugin

**build.gradle**
```groovy
plugins {
    id 'net.wooga.appcenter' version '0.1.0'
}
```


Development
===========

[Code of Conduct](docs/Code-of-conduct.md)

Gradle and Java Compatibility
=============================

Built with Oracle JDK7
Tested with Oracle JDK8

| Gradle Version  | Works    |
| :-------------: | :------: |
| 4.0             | `v0.6.0` |
| 4.1             | `v0.6.0` |
| 4.2             | `v0.6.0` |
| 4.3             | `v0.6.0` |
| 4.4             | ![yes]   |
| 4.5             | ![yes]   |
| 4.6             | ![yes]   |
| 4.6             | ![yes]   |
| 4.7             | ![yes]   |
| 4.8             | ![yes]   |
| 4.9             | ![yes]   |
| 4.10.2          | ![yes]   |
| 5.0             | ![yes]   |
| 5.1             | ![yes]   |
| 5.2             | ![yes]   |
| 5.3             | ![yes]   |
| 5.4             | ![yes]   |
| 5.5             | ![yes]   |
| 5.6.4           | ![yes]   |
| 6.0             | ![yes]   |
| 6.1             | ![yes]   |
| 6.2             | ![yes]   |
| 6.3             | ![yes]   |
| 6.4             | ![yes]   |
 

LICENSE
=======

Copyright 2017 Wooga GmbH

Licensed under the Apache License, Version 2.0 (the "License");
you may not use this file except in compliance with the License.
You may obtain a copy of the License at

<http://www.apache.org/licenses/LICENSE-2.0>

Unless required by applicable law or agreed to in writing, software
distributed under the License is distributed on an "AS IS" BASIS,
WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
See the License for the specific language governing permissions and
limitations under the License.

<!-- Links -->
[unity]:                https://unity3d.com/ "Unity 3D"
[unity_cmd]:            https://docs.unity3d.com/Manual/CommandLineArguments.html
[gradle]:               https://gradle.org/ "Gradle"
[gradle_finalizedBy]:   https://docs.gradle.org/3.5/dsl/org.gradle.api.Task.html#org.gradle.api.Task:finalizedBy
[gradle_dependsOn]:     https://docs.gradle.org/3.5/dsl/org.gradle.api.Task.html#org.gradle.api.Task:dependsOn

[yes]:                  https://atlas-resources.wooga.com/icons/icon_check.svg "yes"
[no]:                   https://atlas-resources.wooga.com/icons/icon_uncheck.svg "no"


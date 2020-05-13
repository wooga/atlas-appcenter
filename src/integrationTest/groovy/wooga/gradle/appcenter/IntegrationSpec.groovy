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

package wooga.gradle.appcenter

import groovy.json.StringEscapeUtils
import nebula.test.functional.ExecutionResult

import org.junit.Rule
import org.junit.contrib.java.lang.system.EnvironmentVariables
import org.junit.contrib.java.lang.system.ProvideSystemProperty

class IntegrationSpec extends nebula.test.IntegrationSpec {

    @Rule
    ProvideSystemProperty properties = new ProvideSystemProperty("ignoreDeprecations", "true")

    @Rule
    public final EnvironmentVariables environmentVariables = new EnvironmentVariables()

    def escapedPath(String path) {
        String osName = System.getProperty("os.name").toLowerCase()
        if (osName.contains("windows")) {
            path = StringEscapeUtils.escapeJava(path)
            return path.replace('\\', '\\\\')
        }
        path
    }

    def setup() {
        def gradleVersion = System.getenv("GRADLE_VERSION")
        if (gradleVersion) {
            this.gradleVersion = gradleVersion
            fork = true
        }
    }

    Boolean outputContains(ExecutionResult result, String message) {
        result.standardOutput.contains(message) || result.standardError.contains(message)
    }

    String wrapValueBasedOnType(Object rawValue, String type) {
        def value
        def rawValueEscaped = String.isInstance(rawValue) ? "'${rawValue}'" : rawValue
        def subtypeMatches = type =~ /(?<mainType>\w+)<(?<subType>[\w<>]+)>/
        def subType = (subtypeMatches.matches()) ? subtypeMatches.group("subType") : null
        type = (subtypeMatches.matches()) ? subtypeMatches.group("mainType") : type
        switch (type) {
            case "Closure":
                if (subType) {
                    value = "{${wrapValueBasedOnType(rawValue, subType)}}"
                } else {
                    value = "{$rawValueEscaped}"
                }
                break
            case "Callable":
                value = "new java.util.concurrent.Callable<${rawValue.class.typeName}>() {@Override ${rawValue.class.typeName} call() throws Exception { $rawValueEscaped }}"
                break
            case "Object":
                value = "new Object() {@Override String toString() { ${rawValueEscaped}.toString() }}"
                break
            case "Provider":
                switch (subType) {
                    case "RegularFile":
                        value = "project.layout.file(${wrapValueBasedOnType(rawValue, "Provider<File>")})"
                        break
                    default:
                        value = "project.provider(${wrapValueBasedOnType(rawValue, "Closure<${subType}>")})"
                        break
                }
                break
            case "String":
                value = "$rawValueEscaped"
                break
            case "String[]":
                value = "'{${rawValue.collect { '"' + it + '"' }.join(",")}}'.split(',')"
                break
            case "File":
                value = "new File('${escapedPath(rawValue.toString())}')"
                break
            case "String...":
                value = "${rawValue.collect { '"' + it + '"' }.join(", ")}"
                break
            case "List":
                value = "[${rawValue.collect { '"' + it + '"' }.join(", ")}]"
                break
            default:
                value = rawValue
        }
        value
    }
}

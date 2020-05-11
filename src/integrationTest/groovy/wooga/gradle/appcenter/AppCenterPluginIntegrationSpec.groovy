package wooga.gradle.appcenter

import spock.lang.Unroll

class AppCenterPluginIntegrationSpec extends IntegrationSpec {

    def setup() {
        buildFile << """
            ${applyPlugin(AppCenterPlugin)}
        """.stripIndent()
    }

    enum PropertyLocation {
        none, script, property, env

        String reason() {
            switch (this) {
                case script:
                    return "value is provided in script"
                case property:
                    return "value is provided in props"
                case env:
                    return "value is set in env"
                default:
                    return "no value was configured"
            }
        }
    }

    String envNameFromProperty(String property) {
        "APP_CENTER_${property.replaceAll(/([A-Z])/, "_\$1").toUpperCase()}"
    }

    @Unroll()
    def "extension property :#property returns '#testValue' if #reason"() {
        given:
        buildFile << """
            task(custom) {
                doLast {
                    def value = appCenter.${property}.getOrNull()
                    println("appCenter.${property}: " + value)
                }
            }
        """

        and: "a gradle.properties"
        def propertiesFile = createFile("gradle.properties")

        def escapedValue = (value instanceof String) ? escapedPath(value) : value

        switch (location) {
            case PropertyLocation.script:
                buildFile << "appCenter.${property} = ${escapedValue}"
                break
            case PropertyLocation.property:
                propertiesFile << "appCenter.${property} = ${escapedValue}"
                break
            case PropertyLocation.env:
                environmentVariables.set(envNameFromProperty(property), "${value}")
                break
            default:
                break
        }

        and: "the test value with replace placeholders"
        if (testValue instanceof String) {
            testValue = testValue.replaceAll("#projectDir#", escapedPath(projectDir.path))
        }

        when: ""
        def result = runTasksSuccessfully("custom")

        then:
        result.standardOutput.contains("appCenter.${property}: ${testValue}")

        where:
        property                | value                 | expectedValue                            | providedValue     | location
        "apiToken"              | "xxx"                 | _                                        | "xxx"             | PropertyLocation.env
        "apiToken"              | "yyy"                 | _                                        | "yyy"             | PropertyLocation.property
        "apiToken"              | "'zzz'"               | "zzz"                                    | "zzz"             | PropertyLocation.script
        "apiToken"              | null                  | _                                        | "null"            | PropertyLocation.none

        "owner"                 | "xxx"                 | _                                        | "xxx"             | PropertyLocation.env
        "owner"                 | "yyy"                 | _                                        | "yyy"             | PropertyLocation.property
        "owner"                 | "'zzz'"               | "zzz"                                    | "zzz"             | PropertyLocation.script
        "owner"                 | null                  | _                                        | "null"            | PropertyLocation.none

        "applicationIdentifier" | "xxx"                 | _                                        | "xxx"             | PropertyLocation.env
        "applicationIdentifier" | "yyy"                 | _                                        | "yyy"             | PropertyLocation.property
        "applicationIdentifier" | "'zzz'"               | "zzz"                                    | "zzz"             | PropertyLocation.script
        "applicationIdentifier" | null                  | _                                        | "null"            | PropertyLocation.none

        "defaultDestinations"   | "group1"              | [["name": "group1"]]                     | "group1"          | PropertyLocation.env
        "defaultDestinations"   | "group1,group2"       | [["name": "group1"], ["name": "group2"]] | "group1,group2"   | PropertyLocation.env
        "defaultDestinations"   | "group1, group2"      | [["name": "group1"], ["name": "group2"]] | "group1, group2"  | PropertyLocation.env
        "defaultDestinations"   | "group2"              | [["name": "group2"]]                     | "group2"          | PropertyLocation.property
        "defaultDestinations"   | "group2,group3"       | [["name": "group2"], ["name": "group3"]] | "group2,group3"   | PropertyLocation.property
        "defaultDestinations"   | "group2, group3"      | [["name": "group2"], ["name": "group3"]] | "group2, group3"  | PropertyLocation.property
        "defaultDestinations"   | "['group3']"          | [["name": "group3"]]                     | "[group3]"        | PropertyLocation.script
        "defaultDestinations"   | "['group3','group4']" | [["name": "group3"], ["name": "group4"]] | "[group3,group4]" | PropertyLocation.script
        "defaultDestinations"   | null                  | [["name": "Collaborators"]]              | "null"            | PropertyLocation.none


        testValue = (expectedValue == _) ? value : expectedValue
        reason = location.reason() + ((location == PropertyLocation.none) ? "" : " with '$providedValue'")
    }

    @Unroll()
    def "can add publish destinations with :#method and type '#type'"() {
        given: "a custom task to print extension property"
        buildFile << """
            task(custom) {
                doLast {
                    def value = appCenter.${property}.getOrNull()
                    println("appCenter.${property}: " + value)
                }
            }
        """

        and: "some configured property"
        buildFile << "appCenter.${method}(${value})"

        when: ""
        def result = runTasksSuccessfully("custom")

        then:
        result.standardOutput.contains("appCenter.${property}: ${expectedValue}")

        where:
        property              | method                   | rawValue             | type           | expectedValue
        "defaultDestinations" | "defaultDestination"     | ["group"]            | "List<String>" | [[name: "Collaborators"], [name: "group"]]
        "defaultDestinations" | "defaultDestination"     | "group"              | "String"       | [[name: "Collaborators"], [name: "group"]]
        "defaultDestinations" | "defaultDestination"     | ["group1", "group2"] | "List<String>" | [[name: "Collaborators"], [name: "group1"], [name: "group2"]]
        "defaultDestinations" | "defaultDestination"     | ["group1", "group2"] | "String..."    | [[name: "Collaborators"], [name: "group1"], [name: "group2"]]
        "defaultDestinations" | "defaultDestinationId"   | "groupId"            | "String"       | [[name: "Collaborators"], [id: "groupId"]]
        "defaultDestinations" | "setDefaultDestinations" | ["group1", "group2"] | "List<String>" | [[name: "group1"], [name: "group2"]]
        value = wrapValueBasedOnType(rawValue, type)
    }
}

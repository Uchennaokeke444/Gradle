/*
 * Copyright 2024 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.gradle.integtests.tooling.r87

import org.gradle.integtests.tooling.TestLauncherSpec
import org.gradle.integtests.tooling.fixture.TargetGradleVersion
import org.gradle.integtests.tooling.fixture.ToolingApiVersion
import org.gradle.test.precondition.Requires
import org.gradle.test.preconditions.UnitTestPreconditions
import org.gradle.tooling.TestLauncher
import spock.lang.Timeout

@Timeout(120)
@ToolingApiVersion('>=6.1')
@TargetGradleVersion(">8.6")
@Requires(UnitTestPreconditions.Jdk17OrEarlier)
/**
 * @see org.gradle.integtests.tooling.r70.TestDisplayNameJUnit5CrossVersionSpec and
 * @see org.gradle.integtests.tooling.r87.TestDisplayNameJUnit4CrossVersionSpec
 */
class TestDisplayNameSpockCrossVersionSpec extends TestLauncherSpec {
    @Override
    void addDefaultTests() {
    }

    @Override
    String simpleJavaProject() {
        """
        allprojects{
            apply plugin: 'groovy'
            ${mavenCentralRepository()}
            dependencies {
                implementation 'org.codehaus.groovy:groovy-all:3.0.0'
                testImplementation platform("org.spockframework:spock-bom:2.1-groovy-3.0")
                testImplementation "org.spockframework:spock-core:2.1-groovy-3.0"
            }

            test {
                useJUnitPlatform()
            }
        }
        """
    }

    def "reports display names of class and method"() {
        file("src/test/groovy/org/example/SimpleTests.groovy") << """package org.example

import spock.lang.Specification

class SimpleTests extends Specification {

    def "success test"() {
        expect:
        true
    }
}

"""
        when:
        launchTests { TestLauncher launcher ->
            launcher.withTaskAndTestClasses(':test', ['org.example.SimpleTests'])
        }

        then:
        jvmTestEvents {
            task(":test") {
                suite("Gradle Test Run :test") {
                    suite("Gradle Test Executor") {
                        testClass("org.example.SimpleTests") {
                            displayName "SimpleTests"
                            test("success test") {
                                displayName "success test"
                            }
                        }
                    }
                }
            }
        }
    }

    def "reports display names of parameterized tests"() {
        file("src/test/groovy/org/example/ParameterizedTests.groovy") << """package org.example

import spock.lang.Specification

class ParameterizedTests extends Specification {

    def "length of #name is #length"() {
        expect:
        name.size() == length

        where:
        name    | length
        "Spock" | 5
        "junit5" | 6
    }
}

"""

        when:
        launchTests { TestLauncher launcher ->
            launcher.withTaskAndTestClasses(':test', ['org.example.ParameterizedTest*'])
        }

        then:
        jvmTestEvents {
            task(":test") {
                suite("Gradle Test Run :test") {
                    suite("Gradle Test Executor") {
                        testClass("org.example.ParameterizedTests") {
                            displayName "ParameterizedTests"
                            suite("length of #name is #length") {
                                displayName "length of #name is #length"
                                generatedTest("length of Spock is 5") {
                                    displayName "length of Spock is 5"
                                }
                                generatedTest("length of junit5 is 6") {
                                    displayName "length of junit5 is 6"
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

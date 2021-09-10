plugins {
    java
}

repositories {
    mavenCentral()
}

testing {
    suites {
        named<JvmTestSuite>("test") {
            useJUnit()

            dependencies {
                implementation("junit:junit:4.13")
            }

            targets {
                all {
                    // tag::test-categories[]
                    testTask.configure {
                        useJUnit {
                            includeCategories("org.gradle.junit.CategoryA")
                            excludeCategories("org.gradle.junit.CategoryB")
                        }
                    }
                    // end::test-categories[]
                }
            }
        }
    }
}

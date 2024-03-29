plugins {
    id("gradlebuild.distribution.api-java")
}

description = "Implementation for interacting with Google Cloud Storage (GCS) repositories"

errorprone {
    disabledChecks.addAll(
        "StringCaseLocaleUsage", // 1 occurrences
        "UnusedMethod", // 1 occurrences
    )
}

dependencies {
    implementation(project(":base-services"))
    implementation(project(":logging"))
    implementation(project(":resources"))
    implementation(project(":resources-http"))
    implementation(project(":core"))

    implementation(libs.slf4jApi)
    implementation(libs.guava)
    implementation(libs.commonsLang)
    implementation(libs.gcs)

    testImplementation(libs.groovy)
    testImplementation(testFixtures(project(":core")))
    testImplementation(testFixtures(project(":dependency-management")))
    testImplementation(testFixtures(project(":ivy")))
    testImplementation(testFixtures(project(":maven")))

    integTestImplementation(project(":core-api"))
    integTestImplementation(project(":model-core"))
    integTestImplementation(libs.commonsIo)
    integTestImplementation(libs.jetty)
    integTestImplementation(libs.joda)

    integTestDistributionRuntimeOnly(project(":distributions-basics"))
}

strictCompile {
    ignoreDeprecations()
}

plugins {
    id("gradlebuild.distribution.api-java")
    id("gradlebuild.jmh")
}

description = "Implementation of build cache controller and factories"

dependencies {
    api(project(":base-annotations"))
    api(project(":build-cache-base"))
    api(project(":build-cache-packaging"))
    api(project(":build-operations"))
    api(project(":enterprise-operations"))
    api(project(":files"))
    api(project(":hashing"))
    api(project(":snapshots"))

    api(libs.jsr305)

    implementation(libs.commonsIo)
    api(libs.guava)
    implementation(libs.h2Database) {
        because("Used in BuildCacheNG")
    }
    implementation(libs.hikariCP) {
        because("Used in BuildCacheNG")
    }
    api(libs.inject)
    implementation(libs.slf4jApi)

    jmhImplementation(project(":native"))
    jmhImplementation(platform(project(":distributions-dependencies")))
    jmhImplementation(libs.aircompressor)
    jmhImplementation(libs.commonsCompress)
    jmhImplementation(libs.commonsIo)
    jmhImplementation(libs.jtar)
    jmhImplementation(libs.snappy)

    testImplementation(testFixtures(project(":base-services")))
    testImplementation(testFixtures(project(":core")))
    testImplementation(testFixtures(project(":snapshots")))

    testFixturesImplementation(testFixtures(project(":hashing")))

    integTestDistributionRuntimeOnly(project(":distributions-core"))
}

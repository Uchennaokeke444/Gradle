/*
 * Copyright 2018 the original author or authors.
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

package org.gradle.kotlin.dsl.support

import org.gradle.api.Project

import org.gradle.api.internal.project.ProjectInternal

import java.io.File


inline fun <reified T : Any> Project.serviceOf(): T =
    (this as ProjectInternal).services.get()


internal
fun serviceRegistryOf(project: Project) =
    (project as ProjectInternal).services


fun isGradleKotlinDslJar(file: File) =
    isGradleKotlinDslJarName(file.name)


internal
fun isGradleKotlinDslJarName(jarName: String) =
    jarName.startsWith("gradle-kotlin-dsl-")

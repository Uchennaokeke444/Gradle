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

package org.gradle.buildinit.plugins.internal;

import org.gradle.buildinit.plugins.internal.modifiers.BuildInitDsl;
import org.gradle.buildinit.plugins.internal.modifiers.BuildInitTestFramework;
import org.gradle.buildinit.plugins.internal.modifiers.ComponentType;
import org.gradle.buildinit.plugins.internal.modifiers.Language;
import org.gradle.buildinit.plugins.internal.modifiers.ModularizationOption;

import java.util.Optional;
import java.util.Set;

/**
 * Creates a new Gradle project
 */
public interface ProjectGenerator extends BuildContentGenerator {
    String getId();

    ComponentType getComponentType();

    Language getLanguage();

    boolean isJvmLanguage();

    Set<ModularizationOption> getModularizationOptions();

    Optional<String> getFurtherReading(InitSettings settings);

    /**
     * Does a source package name make sense for this type of project?
     */
    boolean supportsPackage();

    BuildInitDsl getDefaultDsl();

    /**
     * Returns the set of test frameworks supported for this type of project.
     */
    Set<BuildInitTestFramework> getTestFrameworks(ModularizationOption modularizationOption);

    /**
     * Returns {@link BuildInitTestFramework#NONE} when no tests generated by default.
     */
    BuildInitTestFramework getDefaultTestFramework(ModularizationOption modularizationOption);
}

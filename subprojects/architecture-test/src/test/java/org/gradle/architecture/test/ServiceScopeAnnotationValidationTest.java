/*
 * Copyright 2023 the original author or authors.
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

package org.gradle.architecture.test;

import com.tngtech.archunit.base.DescribedPredicate;
import com.tngtech.archunit.core.domain.JavaClass;
import com.tngtech.archunit.junit.AnalyzeClasses;
import com.tngtech.archunit.junit.ArchTest;
import com.tngtech.archunit.lang.ArchRule;
import org.gradle.internal.Factory;
import org.gradle.internal.service.ServiceRegistry;
import org.gradle.internal.service.scopes.ServiceScope;

import javax.inject.Inject;

import static com.tngtech.archunit.base.DescribedPredicate.not;
import static com.tngtech.archunit.lang.conditions.ArchPredicates.are;
import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.classes;
import static org.gradle.architecture.test.ArchUnitFixture.freeze;

@AnalyzeClasses(packages = "org.gradle")
public class ServiceScopeAnnotationValidationTest {

    private static final DescribedPredicate<JavaClass> special_classes = new DescribedPredicate<JavaClass>("special class") {
        @Override
        public boolean test(JavaClass javaClass) {
            return javaClass.isEquivalentTo(ServiceRegistry.class) || javaClass.isEquivalentTo(Factory.class);
        }
    };

    private static final DescribedPredicate<JavaClass> injected_by_getter = new DescribedPredicate<JavaClass>("injected into getters via @Inject") {
        @Override
        public boolean test(JavaClass javaClass) {
            return javaClass
                .getMethodsWithReturnTypeOfSelf()
                .stream()
                .anyMatch(method -> method.isAnnotatedWith(Inject.class));
        }
    };

    private static final DescribedPredicate<JavaClass> injected_by_constructor = new DescribedPredicate<JavaClass>("injected into constructors via @Inject") {
        @Override
        public boolean test(JavaClass javaClass) {
            return javaClass
                .getMethodsWithParameterTypeOfSelf()
                .stream()
                .anyMatch(method -> method.isAnnotatedWith(Inject.class));
        }
    };

    @ArchTest
    public static final ArchRule all_injected_classes_should_be_annotated_with_service_scope = freeze(classes()
        .that(are(not(special_classes))
            .and(are(injected_by_getter))
            .or(are(injected_by_constructor))
        )
        .should().beAnnotatedWith(ServiceScope.class));

}

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

package org.gradle.internal.restricteddsl.evaluator

import com.h0tk3y.kotlin.staticObjectNotation.analysis.AnalysisSchema
import com.h0tk3y.kotlin.staticObjectNotation.analysis.ResolutionError
import com.h0tk3y.kotlin.staticObjectNotation.analysis.ResolutionResult
import com.h0tk3y.kotlin.staticObjectNotation.analysis.SchemaTypeRefContext
import com.h0tk3y.kotlin.staticObjectNotation.analysis.defaultCodeResolver
import com.h0tk3y.kotlin.staticObjectNotation.astToLanguageTree.DefaultLanguageTreeBuilder
import com.h0tk3y.kotlin.staticObjectNotation.astToLanguageTree.Element
import com.h0tk3y.kotlin.staticObjectNotation.astToLanguageTree.FailingResult
import com.h0tk3y.kotlin.staticObjectNotation.astToLanguageTree.LanguageTreeBuilderWithTopLevelBlock
import com.h0tk3y.kotlin.staticObjectNotation.astToLanguageTree.LanguageTreeResult
import com.h0tk3y.kotlin.staticObjectNotation.astToLanguageTree.parseToAst
import com.h0tk3y.kotlin.staticObjectNotation.language.AstSourceIdentifier
import com.h0tk3y.kotlin.staticObjectNotation.mappingToJvm.RestrictedReflectionToObjectConverter
import com.h0tk3y.kotlin.staticObjectNotation.objectGraph.AssignmentResolver
import com.h0tk3y.kotlin.staticObjectNotation.objectGraph.AssignmentTraceElement
import com.h0tk3y.kotlin.staticObjectNotation.objectGraph.AssignmentTracer
import com.h0tk3y.kotlin.staticObjectNotation.objectGraph.ReflectionContext
import com.h0tk3y.kotlin.staticObjectNotation.objectGraph.reflect
import com.h0tk3y.kotlin.staticObjectNotation.schemaBuilder.kotlinFunctionAsConfigureLambda
import kotlinx.ast.common.ast.Ast
import org.antlr.v4.kotlinruntime.misc.ParseCancellationException
import org.gradle.api.initialization.Settings
import org.gradle.groovy.scripts.ScriptSource
import org.gradle.internal.restricteddsl.plugins.RuntimeTopLevelPluginsReceiver
import org.gradle.internal.restricteddsl.evaluator.RestrictedKotlinScriptEvaluator.EvaluationResult.NotEvaluated
import org.gradle.internal.restricteddsl.evaluator.RestrictedKotlinScriptEvaluator.EvaluationResult.NotEvaluated.StageFailure.FailuresInLanguageTree
import org.gradle.internal.restricteddsl.evaluator.RestrictedKotlinScriptEvaluator.EvaluationResult.NotEvaluated.StageFailure.FailuresInResolution
import org.gradle.internal.restricteddsl.evaluator.RestrictedKotlinScriptEvaluator.EvaluationResult.NotEvaluated.StageFailure.NoParseResult
import org.gradle.internal.restricteddsl.evaluator.RestrictedKotlinScriptEvaluator.EvaluationResult.NotEvaluated.StageFailure.NoSchemaAvailable
import org.gradle.internal.restricteddsl.evaluator.RestrictedKotlinScriptEvaluator.EvaluationResult.NotEvaluated.StageFailure.UnassignedValuesUsed


interface RestrictedKotlinScriptEvaluator {
    fun evaluate(
        target: Any,
        scriptSource: ScriptSource,
    ): EvaluationResult

    sealed interface EvaluationResult {
        object Evaluated : EvaluationResult
        class NotEvaluated(val stageFailures: List<StageFailure>) : EvaluationResult {
            sealed interface StageFailure {
                data class NoSchemaAvailable(val target: Any) : StageFailure
                object NoParseResult : StageFailure
                data class FailuresInLanguageTree(val failures: List<FailingResult>) : StageFailure
                data class FailuresInResolution(val errors: List<ResolutionError>) : StageFailure
                data class UnassignedValuesUsed(val usages: List<AssignmentTraceElement.UnassignedValueUsed>) : StageFailure
            }
        } // TODO: make reason more structured
    }
}


/**
 * A default implementation of a restricted DSL script evaluator, for use when no additional information needs to be provided at the use site.
 * TODO: The consumers should get an instance properly injected instead.
 */
val defaultRestrictedKotlinScriptEvaluator: RestrictedKotlinScriptEvaluator by lazy {
    DefaultRestrictedKotlinScriptEvaluator(DefaultRestrictedScriptSchemaBuilder())
}


internal
class DefaultRestrictedKotlinScriptEvaluator(
    private val schemaBuilder: RestrictedScriptSchemaBuilder
) : RestrictedKotlinScriptEvaluator {
    override fun evaluate(target: Any, scriptSource: ScriptSource): RestrictedKotlinScriptEvaluator.EvaluationResult {
        return when (val schema = schemaBuilder.getAnalysisSchemaForScript(target, scriptContextFor(target))) {
            ScriptSchemaBuildingResult.SchemaNotBuilt -> NotEvaluated(listOf(NoSchemaAvailable(target)))

            is ScriptSchemaBuildingResult.SchemaAvailable -> {
                evaluateWithSchema(schema.schema, scriptSource, target)
            }
        }
    }

    private
    fun evaluateWithSchema(schema: AnalysisSchema, scriptSource: ScriptSource, target: Any): RestrictedKotlinScriptEvaluator.EvaluationResult {
        val failureReasons = mutableListOf<NotEvaluated.StageFailure>()

        val resolver = defaultCodeResolver()
        val ast = astFromScript(scriptSource).singleOrNull()
            ?: run {
                failureReasons += (NoParseResult)
                return NotEvaluated(failureReasons)
            }
        val languageModel = languageModelFromAst(ast, scriptSource)
        val failures = languageModel.results.filterIsInstance<FailingResult>()
        if (failures.isNotEmpty()) {
            failureReasons += FailuresInLanguageTree(failures)
        }
        val elements = languageModel.results.filterIsInstance<Element<*>>().map { it.element }
        val resolution = resolver.resolve(schema, elements)
        if (resolution.errors.isNotEmpty()) {
            failureReasons += FailuresInResolution(resolution.errors)
        }

        val trace = assignmentTrace(resolution)
        val unassignedValueUsages = trace.elements.filterIsInstance<AssignmentTraceElement.UnassignedValueUsed>()
        if (unassignedValueUsages.isNotEmpty()) {
            failureReasons += UnassignedValuesUsed(unassignedValueUsages)
        }
        if (failureReasons.isNotEmpty()) {
            return NotEvaluated(failureReasons)
        }
        val context = ReflectionContext(SchemaTypeRefContext(schema), resolution, trace)
        val topLevel = reflect(resolution.topLevelReceiver, context)

        RestrictedReflectionToObjectConverter(emptyMap(), target, kotlinFunctionAsConfigureLambda).apply(topLevel)
        return RestrictedKotlinScriptEvaluator.EvaluationResult.Evaluated
    }

    private
    fun assignmentTrace(result: ResolutionResult) =
        AssignmentTracer { AssignmentResolver() }.produceAssignmentTrace(result)

    private
    fun astFromScript(scriptSource: ScriptSource): List<Ast> =
        try {
            parseToAst(scriptSource.resource.text)
        } catch (e: ParseCancellationException) {
            emptyList()
        }

    private
    fun languageModelFromAst(ast: Ast, scriptSource: ScriptSource): LanguageTreeResult =
        languageTreeBuilder.build(ast, AstSourceIdentifier(ast, scriptSource.fileName))

    private
    val languageTreeBuilder = LanguageTreeBuilderWithTopLevelBlock(DefaultLanguageTreeBuilder())

    private
    fun scriptContextFor(target: Any) = when (target) {
        is Settings -> RestrictedScriptContext.SettingsScript
        is RuntimeTopLevelPluginsReceiver -> RestrictedScriptContext.PluginsBlock
        else -> RestrictedScriptContext.UnknownScript
    }
}

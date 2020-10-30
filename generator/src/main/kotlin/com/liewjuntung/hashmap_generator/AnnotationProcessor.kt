package com.liewjuntung.hashmap_generator

import com.google.auto.service.AutoService
import com.squareup.kotlinpoet.*
import com.squareup.kotlinpoet.ParameterizedTypeName.Companion.parameterizedBy
import net.ltgt.gradle.incap.IncrementalAnnotationProcessor
import net.ltgt.gradle.incap.IncrementalAnnotationProcessorType
import org.jetbrains.annotations.Nullable
import java.util.*
import javax.annotation.processing.*
import javax.lang.model.SourceVersion
import javax.lang.model.element.Element
import javax.lang.model.element.ElementKind
import javax.lang.model.element.TypeElement
import javax.lang.model.util.Elements
import javax.lang.model.util.Types
import javax.tools.Diagnostic

@AutoService(Processor::class)
@IncrementalAnnotationProcessor(IncrementalAnnotationProcessorType.AGGREGATING)
class AnnotationProcessor : AbstractProcessor() {
    private lateinit var typeUtils: Types
    private lateinit var elementUtils: Elements
    private lateinit var filer: Filer
    private lateinit var messager: Messager

    override fun getSupportedAnnotationTypes(): MutableSet<String> {
        return mutableSetOf(GenerateHashMap::class.java.name)
    }

    override fun getSupportedSourceVersion(): SourceVersion = SourceVersion.latest()

    override fun init(processingEnv: ProcessingEnvironment) {
        super.init(processingEnv)
        typeUtils = processingEnv.typeUtils
        elementUtils = processingEnv.elementUtils
        filer = processingEnv.filer
        messager = processingEnv.messager
    }

    override fun process(annotations: MutableSet<out TypeElement>, roundEnv: RoundEnvironment): Boolean {


        roundEnv.getElementsAnnotatedWith(GenerateHashMap::class.java).forEach { annotatedElement ->
            if (!annotatedElement.kind.isClass) {
                messager.printMessage(
                    Diagnostic.Kind.ERROR,
                    "Only classes can be annotated with ${GenerateHashMap::class.java.simpleName}"
                )
                return true
            }
            try {

                val packageName = processingEnv.elementUtils.getPackageOf(annotatedElement).toString()
                val fileSpecBuilder = FileSpec.builder(
                    "com.netvirta.generated.hashMapUtils",
                    "Generated${annotatedElement.simpleName}Utils"
                )
                fileSpecBuilder
                    .addFunction(hashMapBuilder(annotatedElement, packageName))
                    .addFunction(hashMapListBuilder(annotatedElement, packageName))
                val fileSpec = fileSpecBuilder.build()
                fileSpec.writeTo(filer)
            } catch (e: Exception) {
                messager.printMessage(Diagnostic.Kind.ERROR, e.toString())
            }
        }
        return false
    }

    private fun hashMapListBuilder(annotatedElement: Element, packageName: String): FunSpec {
        val className = ClassName(packageName, annotatedElement.simpleName.toString())

        val funSpecBuilder = FunSpec.builder("toHashMapList")
            .addAnnotation(
                AnnotationSpec.builder(JvmName::class)
                    .addMember("\"${annotatedElement.simpleName}ToHashMapList\"")
                    .build()
            )
            .receiver(List::class.asClassName().parameterizedBy(className))
            .addStatement("return map { it.toHashMap() }")
            .returns(
                List::class.asClassName().parameterizedBy(
                    HashMap::class.asClassName().parameterizedBy(
                        String::class.asClassName(),
                        Any::class.asClassName().copy(nullable = true)
                    )
                )
            )

        return funSpecBuilder.build()
    }

    private fun hashMapBuilder(annotatedElement: Element, packageName: String): FunSpec {
        val className = ClassName(packageName, annotatedElement.simpleName.toString())

        val funSpecBuilder = FunSpec.builder("toHashMap")
            .receiver(className)
            .returns(
                HashMap::class.asClassName().parameterizedBy(
                    String::class.asClassName(),
                    Any::class.asClassName().copy(nullable = true)
                )
            )

        val mapStrList: ArrayList<String> = arrayListOf()

        annotatedElement.enclosedElements.filter { element -> element.kind == ElementKind.FIELD }.forEach { element ->
            val annotation: GenerateHashMapName? = element.getAnnotation(GenerateHashMapName::class.java)
            val toStringAnnotation: GenerateToString? = element.getAnnotation(GenerateToString::class.java)
            val nullable: Nullable? = element.getAnnotation(Nullable::class.java)
            val nullableText = if (nullable != null) {
                "?"
            } else {
                ""
            }
            val addToMapStr = if (element.asType().asTypeName().toString().contains("java.util.ArrayList") ||
                element.asType().asTypeName().toString().contains("java.util.List")
            ) {
                "${nullableText}.toHashMapList()"
            } else if (element.asType().kind.isPrimitive ||
                element.asType().asTypeName().toString() == "java.lang.String" ||
                element.asType().asTypeName().toString().contains("java.util") ||
                element.asType().asTypeName().toString().contains("java.lang")
            ) {
                ""
            } else {
                "${nullableText}.toHashMap()"
            }

            if (element.asType().asTypeName().toString() != "${packageName}.${annotatedElement.simpleName}.Companion") {
                mapStrList += when {
                    annotation != null -> {
                        "\"${annotation.name}\" to ${element.simpleName}$addToMapStr"
                    }
                    toStringAnnotation != null -> {
                        "\"${element.simpleName}\" to ${element.simpleName}.toString()"
                    }
                    else -> {
                        "\"${element.simpleName}\" to ${element.simpleName}$addToMapStr"
                    }
                }
            }
        }
        funSpecBuilder.addStatement("return hashMapOf(${mapStrList.joinToString(", ")})")
        return funSpecBuilder.build()
    }
}

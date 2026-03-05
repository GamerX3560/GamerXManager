package com.gamerx.manager.feature.script.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import java.util.UUID

/**
 * Complete Script Applet Manifest
 * Based on "Script Architecture for Advanced Android App" specification
 */
@Serializable
data class ScriptManifest(
    val id: String = UUID.randomUUID().toString(),
    val identity: ScriptIdentity,
    val capabilities: ScriptCapabilities = ScriptCapabilities(),
    val ui: UiDefinition = UiDefinition(),
    val execution: ExecutionConfig = ExecutionConfig()
)

@Serializable
data class ScriptIdentity(
    var name: String,
    val author: String = "Unknown",
    val version: String = "1.0.0",
    val description: String = "",
    val url: String = "",
    @SerialName("icon_path") val iconPath: String? = null
)

@Serializable
data class ScriptCapabilities(
    @SerialName("root_required") val rootRequired: Boolean = true,
    @SerialName("chroot_required") val chrootRequired: Boolean = false,
    @SerialName("network_access") val networkAccess: Boolean = false,
    @SerialName("qs_compatible") val qsCompatible: Boolean = true,
    @SerialName("headless_capable") val headlessCapable: Boolean = false,
    @SerialName("tile_mode") val tileMode: String = "execute"  // execute, toggle, popup, quick_select
)

@Serializable
data class UiDefinition(
    val presentation: String = "sheet", // sheet, dialog, fullscreen, headless
    val inputs: List<ScriptInput> = emptyList()
)

/**
 * Sealed class for polymorphic input types
 * Each type maps to a specific UI component
 */
@Serializable
sealed class ScriptInput {
    abstract val id: String
    abstract val label: String
    abstract val description: String?
    abstract val required: Boolean
    abstract val dependency: InputDependency?

    @Serializable
    @SerialName("text")
    data class TextInput(
        override val id: String,
        override val label: String,
        override val description: String? = null,
        override val required: Boolean = false,
        override val dependency: InputDependency? = null,
        val default: String = "",
        val placeholder: String = "",
        val regex: String? = null,
        @SerialName("regex_error") val regexError: String? = null,
        @SerialName("max_length") val maxLength: Int? = null
    ) : ScriptInput()

    @Serializable
    @SerialName("number")
    data class NumberInput(
        override val id: String,
        override val label: String,
        override val description: String? = null,
        override val required: Boolean = false,
        override val dependency: InputDependency? = null,
        val default: Double = 0.0,
        val min: Double? = null,
        val max: Double? = null,
        val step: Double = 1.0
    ) : ScriptInput()

    @Serializable
    @SerialName("toggle")
    data class ToggleInput(
        override val id: String,
        override val label: String,
        override val description: String? = null,
        override val required: Boolean = false,
        override val dependency: InputDependency? = null,
        val default: Boolean = false
    ) : ScriptInput()

    @Serializable
    @SerialName("select")
    data class SelectInput(
        override val id: String,
        override val label: String,
        override val description: String? = null,
        override val required: Boolean = false,
        override val dependency: InputDependency? = null,
        val options: List<SelectOption> = emptyList(),
        val default: String? = null
    ) : ScriptInput()

    @Serializable
    @SerialName("multi_select")
    data class MultiSelectInput(
        override val id: String,
        override val label: String,
        override val description: String? = null,
        override val required: Boolean = false,
        override val dependency: InputDependency? = null,
        val options: List<SelectOption> = emptyList(),
        val default: List<String> = emptyList()
    ) : ScriptInput()

    @Serializable
    @SerialName("slider")
    data class SliderInput(
        override val id: String,
        override val label: String,
        override val description: String? = null,
        override val required: Boolean = false,
        override val dependency: InputDependency? = null,
        val default: Float = 0f,
        val min: Float = 0f,
        val max: Float = 100f,
        val step: Float = 1f,
        @SerialName("show_value") val showValue: Boolean = true,
        val unit: String = ""
    ) : ScriptInput()

    @Serializable
    @SerialName("info")
    data class InfoInput(
        override val id: String,
        override val label: String,
        override val description: String? = null,
        override val required: Boolean = false,
        override val dependency: InputDependency? = null,
        val style: String = "default" // default, warning, error, success
    ) : ScriptInput()

    @Serializable
    @SerialName("file")
    data class FileInput(
        override val id: String,
        override val label: String,
        override val description: String? = null,
        override val required: Boolean = false,
        override val dependency: InputDependency? = null,
        @SerialName("mime_types") val mimeTypes: List<String> = listOf("*/*")
    ) : ScriptInput()
}

@Serializable
data class SelectOption(
    val value: String,
    val label: String,
    val description: String? = null
)

@Serializable
data class InputDependency(
    val target: String,
    val value: String,
    val condition: String = "equals" // equals, not_equals, contains, greater_than, less_than
)

@Serializable
data class ExecutionConfig(
    val engine: String = "sh", // sh, python3, node
    @SerialName("entry_point") val entryPoint: String = "logic.sh",
    val timeout: Int = 30,
    @SerialName("working_dir") val workingDir: String? = null
)

/**
 * Execution result with parsed output
 */
data class ScriptExecutionResult(
    val exitCode: Int,
    val stdout: String,
    val stderr: String,
    val parsedOutput: ScriptOutput? = null,
    val error: String? = null
)

/**
 * Standardized script output format
 */
@Serializable
data class ScriptOutput(
    val status: String = "success", // success, error, warning
    val message: String = "",
    val actions: List<String> = emptyList(),
    @SerialName("ui_updates") val uiUpdates: Map<String, String> = emptyMap()
)

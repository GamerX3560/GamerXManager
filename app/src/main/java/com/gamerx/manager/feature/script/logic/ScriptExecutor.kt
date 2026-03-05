package com.gamerx.manager.feature.script.logic

import com.gamerx.manager.feature.script.model.ExecutionConfig
import com.gamerx.manager.feature.script.model.ScriptManifest
import com.gamerx.manager.feature.script.model.ScriptOutput
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeoutOrNull
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.jsonPrimitive
import java.io.BufferedReader
import java.io.File
import java.io.InputStreamReader
import java.util.concurrent.TimeUnit

/**
 * Execution result with raw and parsed output
 */
data class ExecutionResult(
    val exitCode: Int,
    val stdout: String,
    val stderr: String,
    val timedOut: Boolean = false,
    val error: String? = null
)

/**
 * Script Executor - Handles script execution in Android Shell (root)
 * 
 * Features:
 * - Configurable timeout
 * - Proper stream separation (stdout/stderr)
 * - JSON input/output protocol
 * - Action parsing and execution
 */
object ScriptExecutor {

    private const val TMP_PATH = "/data/local/tmp"
    
    private val json = Json { 
        ignoreUnknownKeys = true
        encodeDefaults = true
    }

    /**
     * Execute a script with the given inputs
     */
    suspend fun executeScript(
        manifest: ScriptManifest,
        scriptFile: File,
        inputs: JsonObject
    ): ExecutionResult = withContext(Dispatchers.IO) {
        
        val uuid = manifest.id
        val extension = scriptFile.extension.ifEmpty { "sh" }
        val tmpScriptPath = "$TMP_PATH/gx_script_$uuid.$extension"
        val tmpInputPath = "$TMP_PATH/gx_input_$uuid.json"
        val timeoutSeconds = manifest.execution.timeout.toLong().coerceIn(5, 300)
        
        try {
            // 1. Prepare Input JSON (kept for advanced scripts)
            val inputPayload = buildInputPayload(inputs)
            
            // 2. Write input file via root (available as $GX_INPUT_FILE)
            val writeResult = runRootCommand(
                "echo '${inputPayload.replace("'", "'\\''")}' > $tmpInputPath"
            )
            if (writeResult.exitCode != 0) {
                return@withContext ExecutionResult(
                    exitCode = -1,
                    stdout = "",
                    stderr = "Failed to write input file: ${writeResult.stderr}",
                    error = "Input file creation failed"
                )
            }
            
            // 3. Copy and prepare script
            val copyResult = runRootCommand("cp -f '${scriptFile.absolutePath}' '$tmpScriptPath' && chmod 755 '$tmpScriptPath'")
            if (copyResult.exitCode != 0) {
                cleanup(tmpScriptPath, tmpInputPath)
                return@withContext ExecutionResult(
                    exitCode = -1,
                    stdout = "",
                    stderr = "Failed to prepare script: ${copyResult.stderr}",
                    error = "Script preparation failed"
                )
            }
            
            // 4. Determine execution engine
            val engine = getEngine(manifest.execution, extension)
            
            // 5. Build environment variable exports from inputs
            //    This is the PRIMARY way scripts receive their inputs
            val envExports = buildEnvExports(inputs, tmpInputPath)
            
            // 6. Execute with timeout - inputs as env vars
            val execCommand = "$envExports $engine '$tmpScriptPath'"
            
            val result = withTimeoutOrNull(timeoutSeconds * 1000) {
                runRootCommand(execCommand)
            }
            
            // 6. Cleanup
            cleanup(tmpScriptPath, tmpInputPath)
            
            // 7. Handle timeout
            if (result == null) {
                // Kill any lingering processes
                runRootCommand("pkill -f 'gx_script_$uuid' 2>/dev/null")
                return@withContext ExecutionResult(
                    exitCode = -1,
                    stdout = "",
                    stderr = "Script execution timed out after ${timeoutSeconds}s",
                    timedOut = true,
                    error = "Timeout"
                )
            }
            
            return@withContext result
            
        } catch (e: Exception) {
            cleanup(tmpScriptPath, tmpInputPath)
            return@withContext ExecutionResult(
                exitCode = -1,
                stdout = "",
                stderr = e.message ?: "Unknown error",
                error = e.javaClass.simpleName
            )
        }
    }

    /**
     * Execute a script in headless mode (for QS tiles)
     */
    suspend fun executeHeadless(
        manifest: ScriptManifest,
        scriptFile: File,
        savedInputs: Map<String, String>
    ): ExecutionResult = withContext(Dispatchers.IO) {
        val jsonInputs = kotlinx.serialization.json.buildJsonObject {
            savedInputs.forEach { (key, value) ->
                put(key, kotlinx.serialization.json.JsonPrimitive(value))
            }
        }
        executeScript(manifest, scriptFile, jsonInputs)
    }

    /**
     * Parse script output and extract actions
     */
    fun parseOutput(stdout: String): ScriptOutput? {
        return try {
            if (stdout.isBlank()) return null
            
            // Try to find JSON in output (may have other text around it)
            val jsonStart = stdout.indexOf('{')
            val jsonEnd = stdout.lastIndexOf('}')
            
            if (jsonStart >= 0 && jsonEnd > jsonStart) {
                val jsonStr = stdout.substring(jsonStart, jsonEnd + 1)
                json.decodeFromString<ScriptOutput>(jsonStr)
            } else {
                null
            }
        } catch (e: Exception) {
            null
        }
    }

    // ==================== PRIVATE HELPERS ====================

    /**
     * Build shell-safe environment variable export string from inputs.
     * Each input key becomes an exported env var accessible in the script.
     * Example: export action='set_level'; export level='100'; export GX_INPUT_FILE='/path/to/json'
     */
    private fun buildEnvExports(inputs: JsonObject, inputFilePath: String): String {
        val exports = mutableListOf<String>()
        
        // Export each input as an environment variable
        inputs.forEach { (key, value) ->
            // Sanitize key: only allow alphanumeric and underscore
            val safeKey = key.replace(Regex("[^a-zA-Z0-9_]"), "_")
            // Get the raw string value and escape single quotes for shell safety
            val rawValue = try {
                value.jsonPrimitive.content
            } catch (e: Exception) {
                value.toString().trim('"')
            }
            val safeValue = rawValue.replace("'", "'\\''")
            exports.add("export $safeKey='$safeValue'")
        }
        
        // Also export the JSON input file path for advanced scripts
        exports.add("export GX_INPUT_FILE='$inputFilePath'")
        
        return exports.joinToString("; ") + ";"
    }

    private fun buildInputPayload(inputs: JsonObject): String {
        val payload = kotlinx.serialization.json.buildJsonObject {
            put("context", kotlinx.serialization.json.buildJsonObject {
                put("android_version", kotlinx.serialization.json.JsonPrimitive(android.os.Build.VERSION.SDK_INT))
                put("device_model", kotlinx.serialization.json.JsonPrimitive(android.os.Build.MODEL))
                put("device_brand", kotlinx.serialization.json.JsonPrimitive(android.os.Build.BRAND))
                put("timestamp", kotlinx.serialization.json.JsonPrimitive(System.currentTimeMillis()))
            })
            put("inputs", inputs)
        }
        return payload.toString()
    }

    private fun getEngine(config: ExecutionConfig, extension: String): String {
        // Explicit engine takes priority
        if (config.engine.isNotBlank() && config.engine != "auto") {
            return when (config.engine) {
                "sh" -> "/system/bin/sh"
                "bash" -> "bash"
                "python3", "python" -> "python3"
                "node" -> "node"
                else -> config.engine
            }
        }
        
        // Auto-detect from extension
        return when (extension.lowercase()) {
            "sh" -> "/system/bin/sh"
            "bash" -> "bash"
            "py" -> "python3"
            "js" -> "node"
            else -> "/system/bin/sh"
        }
    }

    private fun cleanup(vararg paths: String) {
        try {
            val rmCmd = paths.joinToString(" ") { "'$it'" }
            runRootCommand("rm -f $rmCmd 2>/dev/null")
        } catch (e: Exception) {
            // Ignore cleanup errors
        }
    }

    private fun runRootCommand(command: String): ExecutionResult {
        return try {
            val process = ProcessBuilder("su", "-c", command)
                .redirectErrorStream(false)
                .start()
            
            val stdoutBuilder = StringBuilder()
            val stderrBuilder = StringBuilder()
            
            // Read streams in parallel
            val stdoutThread = Thread {
                BufferedReader(InputStreamReader(process.inputStream)).use { reader ->
                    reader.forEachLine { line ->
                        stdoutBuilder.append(line).append("\n")
                    }
                }
            }
            
            val stderrThread = Thread {
                BufferedReader(InputStreamReader(process.errorStream)).use { reader ->
                    reader.forEachLine { line ->
                        stderrBuilder.append(line).append("\n")
                    }
                }
            }
            
            stdoutThread.start()
            stderrThread.start()
            
            // Wait for process with overall timeout
            val finished = process.waitFor(60, TimeUnit.SECONDS)
            
            if (!finished) {
                process.destroyForcibly()
                return ExecutionResult(
                    exitCode = -1,
                    stdout = stdoutBuilder.toString().trim(),
                    stderr = "Process forcibly terminated",
                    timedOut = true
                )
            }
            
            stdoutThread.join(1000)
            stderrThread.join(1000)
            
            ExecutionResult(
                exitCode = process.exitValue(),
                stdout = stdoutBuilder.toString().trim(),
                stderr = stderrBuilder.toString().trim()
            )
            
        } catch (e: Exception) {
            ExecutionResult(
                exitCode = -1,
                stdout = "",
                stderr = e.message ?: "Unknown error",
                error = e.javaClass.simpleName
            )
        }
    }
}

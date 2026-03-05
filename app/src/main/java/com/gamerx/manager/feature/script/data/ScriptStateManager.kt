package com.gamerx.manager.feature.script.data

import android.content.Context
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import java.io.File

object ScriptStateManager {

    // Load state map from script's state/store.json
    suspend fun loadState(context: Context, scriptId: String): Map<String, Any> = withContext(Dispatchers.IO) {
        val scriptDir = File(context.filesDir, "scripts/$scriptId")
        val stateDir = File(scriptDir, "state")
        val storeFile = File(stateDir, "store.json")

        if (!storeFile.exists()) return@withContext emptyMap()

        try {
            val jsonString = storeFile.readText()
            val json = Json { ignoreUnknownKeys = true }
             // We use a Map<String, String> for simplicity in JSON and convert on usage, 
             // but if we want type safety, we can stick to String storage and VM parses it.
             // Or serialized Map<String, String> is easiest. 
            return@withContext json.decodeFromString<Map<String, String>>(jsonString)
        } catch (e: Exception) {
            e.printStackTrace()
            return@withContext emptyMap()
        }
    }

    suspend fun saveState(context: Context, scriptId: String, inputs: Map<String, Any>) = withContext(Dispatchers.IO) {
        val scriptDir = File(context.filesDir, "scripts/$scriptId")
        val stateDir = File(scriptDir, "state")
        if (!stateDir.exists()) stateDir.mkdirs()
        val storeFile = File(stateDir, "store.json")

        try {
            // Convert values to Strings for heterogeneous storage
            val stringMap = inputs.mapValues { it.value.toString() }
            val jsonString = Json.encodeToString(stringMap)
            storeFile.writeText(jsonString)
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }
}

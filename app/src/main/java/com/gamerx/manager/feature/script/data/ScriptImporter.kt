package com.gamerx.manager.feature.script.data

import android.content.Context
import android.net.Uri
import com.gamerx.manager.feature.script.model.ScriptManifest
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import java.io.BufferedOutputStream
import java.io.File
import java.io.FileOutputStream
import java.util.UUID
import java.util.zip.ZipInputStream

object ScriptImporter {

    suspend fun importScript(context: Context, zipUri: Uri): String = withContext(Dispatchers.IO) {
        val contentResolver = context.contentResolver
        val stagingDir = File(context.cacheDir, "script_staging_${UUID.randomUUID()}")
        stagingDir.mkdirs()

        try {
            // 1. Extract Zip to Staging
            val inputStream = contentResolver.openInputStream(zipUri) 
                ?: throw Exception("Cannot open script package")
            
            ZipInputStream(inputStream).use { zip ->
                var entry = zip.nextEntry
                while (entry != null) {
                    val file = File(stagingDir, entry.name)
                    if (entry.isDirectory) {
                        file.mkdirs()
                    } else {
                        file.parentFile?.mkdirs()
                        BufferedOutputStream(FileOutputStream(file)).use { output ->
                            zip.copyTo(output)
                        }
                    }
                    entry = zip.nextEntry
                }
            }

            // 2. Validate Manifest
            val manifestFile = File(stagingDir, "manifest.json")
            if (!manifestFile.exists()) {
                throw Exception("Invalid Script Package: manifest.json missing")
            }

            val json = Json { ignoreUnknownKeys = true }
            val originalManifest = json.decodeFromString<ScriptManifest>(manifestFile.readText())

            // 3. Generate New Identity
            val newUuid = UUID.randomUUID().toString()
            val newManifest = originalManifest.copy(id = newUuid)

            // 4. Update Manifest File
            val newJsonString = json.encodeToString(newManifest)
            manifestFile.writeText(newJsonString)

            // 5. Move to Internal Storage
            val scriptsDir = File(context.filesDir, "scripts")
            if (!scriptsDir.exists()) scriptsDir.mkdirs()
            
            val targetDir = File(scriptsDir, newUuid)
            if (targetDir.exists()) targetDir.deleteRecursively()
            
            stagingDir.renameTo(targetDir)
            
            return@withContext newUuid

        } catch (e: Exception) {
            e.printStackTrace()
            throw e
        } finally {
            // Cleanup staging if it still exists (renameTo might have moved it)
            if (stagingDir.exists()) {
                stagingDir.deleteRecursively()
            }
        }
    }
}

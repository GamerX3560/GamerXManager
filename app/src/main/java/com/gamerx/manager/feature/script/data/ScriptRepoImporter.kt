package com.gamerx.manager.feature.script.data

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import java.io.File
import java.io.FileOutputStream
import java.net.URL
import java.util.zip.ZipInputStream

/**
 * Script Repository Importer
 * Fetches and imports scripts from GitHub repositories
 */
object ScriptRepoImporter {

    private val json = Json { ignoreUnknownKeys = true }

    /**
     * Represents a script found in a git repo
     */
    @Serializable
    data class RepoScript(
        val name: String,
        val path: String,
        val downloadUrl: String,
        val description: String = ""
    )

    /**
     * GitHub API response for tree contents
     */
    @Serializable
    data class GitHubTreeResponse(
        val tree: List<GitHubTreeItem> = emptyList()
    )

    @Serializable
    data class GitHubTreeItem(
        val path: String,
        val mode: String,
        val type: String,  // "blob" or "tree"
        val sha: String
    )

    @Serializable
    data class GitHubContentItem(
        val name: String,
        val path: String,
        val type: String,  // "file" or "dir"
        val download_url: String? = null
    )

    /**
     * Parse a GitHub URL into owner/repo
     */
    fun parseGitHubUrl(url: String): Pair<String, String>? {
        // Supports: https://github.com/owner/repo, github.com/owner/repo
        val regex = Regex("""(?:https?://)?github\.com/([^/]+)/([^/]+?)(?:\.git)?/?$""")
        val match = regex.find(url.trim())
        return match?.let {
            Pair(it.groupValues[1], it.groupValues[2])
        }
    }

    /**
     * Fetch available scripts from a GitHub repository
     * Looks for directories containing manifest.json
     */
    suspend fun fetchScriptsFromRepo(repoUrl: String): Result<List<RepoScript>> = withContext(Dispatchers.IO) {
        try {
            val (owner, repo) = parseGitHubUrl(repoUrl)
                ?: return@withContext Result.failure(Exception("Invalid GitHub URL"))

            // Use GitHub API to get repo contents
            val apiUrl = "https://api.github.com/repos/$owner/$repo/contents"
            val response = URL(apiUrl).readText()
            
            val contents = json.decodeFromString<List<GitHubContentItem>>(response)
            val scripts = mutableListOf<RepoScript>()

            // Check each directory for manifest.json
            for (item in contents.filter { it.type == "dir" }) {
                val dirApiUrl = "https://api.github.com/repos/$owner/$repo/contents/${item.path}"
                try {
                    val dirResponse = URL(dirApiUrl).readText()
                    val dirContents = json.decodeFromString<List<GitHubContentItem>>(dirResponse)
                    
                    // If directory contains manifest.json, it's a script
                    val hasManifest = dirContents.any { it.name == "manifest.json" }
                    if (hasManifest) {
                        // Try to read script name from manifest
                        val manifestItem = dirContents.first { it.name == "manifest.json" }
                        val scriptName = try {
                            val manifestContent = URL(manifestItem.download_url ?: "").readText()
                            val manifestJson = json.decodeFromString<ManifestPreview>(manifestContent)
                            manifestJson.identity?.name ?: item.name
                        } catch (e: Exception) {
                            item.name
                        }
                        
                        scripts.add(
                            RepoScript(
                                name = scriptName,
                                path = item.path,
                                downloadUrl = "https://github.com/$owner/$repo/archive/refs/heads/main.zip",
                                description = "From $owner/$repo"
                            )
                        )
                    }
                } catch (e: Exception) {
                    // Skip directories that fail
                }
            }
            
            // Also check root for manifest.json (single script repo)
            val hasRootManifest = contents.any { it.name == "manifest.json" }
            if (hasRootManifest) {
                scripts.add(
                    RepoScript(
                        name = repo,
                        path = "",
                        downloadUrl = "https://github.com/$owner/$repo/archive/refs/heads/main.zip",
                        description = "Root script from $owner/$repo"
                    )
                )
            }

            Result.success(scripts)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * Download and import a specific script from a repo
     */
    suspend fun importScriptFromRepo(
        repoUrl: String, 
        scriptPath: String,
        scriptsDir: File
    ): Result<String> = withContext(Dispatchers.IO) {
        try {
            val (owner, repo) = parseGitHubUrl(repoUrl)
                ?: return@withContext Result.failure(Exception("Invalid GitHub URL"))
            
            // Download repo as zip
            val zipUrl = "https://github.com/$owner/$repo/archive/refs/heads/main.zip"
            val tempZip = File.createTempFile("gx_repo_", ".zip")
            
            URL(zipUrl).openStream().use { input ->
                FileOutputStream(tempZip).use { output ->
                    input.copyTo(output)
                }
            }
            
            // Extract specific script folder
            val repoPrefix = "$repo-main/"
            val scriptPrefix = if (scriptPath.isEmpty()) repoPrefix else "$repoPrefix$scriptPath/"
            
            val scriptId = java.util.UUID.randomUUID().toString()
            val targetDir = File(scriptsDir, scriptId).apply { mkdirs() }
            
            ZipInputStream(tempZip.inputStream()).use { zip ->
                var entry = zip.nextEntry
                while (entry != null) {
                    if (entry.name.startsWith(scriptPrefix) && !entry.isDirectory) {
                        val relativePath = entry.name.removePrefix(scriptPrefix)
                        if (relativePath.isNotEmpty()) {
                            val targetFile = File(targetDir, relativePath)
                            targetFile.parentFile?.mkdirs()
                            FileOutputStream(targetFile).use { out ->
                                zip.copyTo(out)
                            }
                        }
                    }
                    entry = zip.nextEntry
                }
            }
            
            tempZip.delete()
            
            // Verify manifest exists
            if (!File(targetDir, "manifest.json").exists()) {
                targetDir.deleteRecursively()
                return@withContext Result.failure(Exception("No manifest.json found"))
            }
            
            Result.success(scriptId)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    @Serializable
    private data class ManifestPreview(
        val identity: IdentityPreview? = null
    )

    @Serializable
    private data class IdentityPreview(
        val name: String? = null
    )
}

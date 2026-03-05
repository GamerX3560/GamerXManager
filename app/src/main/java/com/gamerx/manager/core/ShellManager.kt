package com.gamerx.manager.core

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.BufferedReader
import java.io.InputStreamReader

object ShellManager {

    /**
     * Executes a shell command (defaulting to root) and returns the output.
     * returns a Pair(exitCode, output)
     */
    suspend fun runCommand(command: String, asRoot: Boolean = true): Pair<Int, String> {
        return withContext(Dispatchers.IO) {
            try {
                val prefix = if (asRoot) "su -c " else ""
                // For su -c, we need to wrap the actual command in quotes if it's complex,
                // but for simple commands usually raw execution works.
                // A safer way is to use ProcessBuilder(["su", "-c", command])
                
                val process = if (asRoot) {
                    ProcessBuilder("su", "-c", command).start()
                } else {
                    ProcessBuilder("sh", "-c", command).start()
                }

                val output = StringBuilder()
                val reader = BufferedReader(InputStreamReader(process.inputStream))
                var line: String?
                while (reader.readLine().also { line = it } != null) {
                    output.append(line).append("\n")
                }
                
                val errorReader = BufferedReader(InputStreamReader(process.errorStream))
                while (errorReader.readLine().also { line = it } != null) {
                    output.append(line).append("\n")
                }

                process.waitFor()
                Pair(process.exitValue(), output.toString().trim())
            } catch (e: Exception) {
                e.printStackTrace()
                Pair(-1, e.message ?: "Unknown Error")
            }
        }
    }

    suspend fun checkRootAccess(): Boolean {
        val (exitCode, output) = runCommand("id")
        return exitCode == 0 && output.contains("uid=0")
    }
}

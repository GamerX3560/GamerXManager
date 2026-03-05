package com.gamerx.manager.worker

import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.content.pm.ServiceInfo
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.work.CoroutineWorker
import androidx.work.ForegroundInfo
import androidx.work.WorkerParameters
import androidx.work.workDataOf
import com.gamerx.manager.R
import com.gamerx.manager.feature.linux.LinuxConstants
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.io.BufferedInputStream
import java.io.File
import java.io.FileOutputStream
import java.io.RandomAccessFile
import java.net.HttpURLConnection
import java.net.URL

class LinuxInstallerWorker(
    context: Context,
    workerParams: WorkerParameters
) : CoroutineWorker(context, workerParams) {

    private val notificationManager =
        context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

    override suspend fun doWork(): Result = withContext(Dispatchers.IO) {
        try {
            // Create notification channel
            createNotificationChannel()

            setForegroundCompat(createForegroundInfo(0, "Checking for latest release..."))

            // ── Step 1: Fetch download URL from GitHub Releases API ──
            val (downloadUrl, fileSize) = fetchReleaseInfo()
            if (downloadUrl.isNullOrEmpty()) {
                updateNotification(-1, "Error: Could not find release")
                return@withContext Result.failure(workDataOf(LinuxConstants.KEY_ERROR to "Release not found"))
            }

            // ── Step 2: Download rootfs tar.gz to cache ──
            val cacheFile = File(applicationContext.cacheDir, LinuxConstants.ASSET_NAME)

            setProgressCompat(0, "Starting download...")

            downloadFileWithResume(downloadUrl, cacheFile, fileSize)

            // ── Step 3: Create target directories via root ──
            setProgressCompat(90, "Preparing install directory...")
            updateNotification(90, "Preparing install directory...")

            val mkdirResult = execRoot("mkdir -p ${LinuxConstants.ROOTFS_PATH} && mkdir -p ${LinuxConstants.BIN_PATH}")
            if (mkdirResult.first != 0) {
                throw Exception("Failed to create directories: ${mkdirResult.second}")
            }

            // ── Step 4: Extract tar.gz to rootfs via root ──
            setProgressCompat(92, "Extracting Linux image (this takes a while)...")
            updateNotification(92, "Extracting Linux image...")

            val extractResult = execRoot(
                "tar -xzf ${cacheFile.absolutePath} -C ${LinuxConstants.LINUX_ROOT}"
            )
            if (extractResult.first != 0) {
                throw Exception("Extraction failed: ${extractResult.second}")
            }

            // ── Step 5: Post-install setup ──
            setProgressCompat(96, "Setting up environment...")
            updateNotification(96, "Setting up chroot environment...")

            setupChrootEnvironment()

            // ── Step 6: Cleanup ──
            cacheFile.delete()

            setProgressCompat(100, "Installation complete!")
            updateNotification(100, "Installation complete ✓")

            return@withContext Result.success(
                workDataOf(
                    LinuxConstants.KEY_PROGRESS to 100,
                    LinuxConstants.KEY_STATUS to "Installation complete!"
                )
            )

        } catch (e: Exception) {
            e.printStackTrace()
            updateNotification(-1, "Error: ${e.message?.take(100)}")
            return@withContext Result.failure(
                workDataOf(LinuxConstants.KEY_ERROR to (e.message ?: "Unknown error"))
            )
        }
    }

    // ────────────────────────────────────────────────────────────
    //  GitHub Releases API
    // ────────────────────────────────────────────────────────────

    private fun fetchReleaseInfo(): Pair<String?, Long> {
        val url = URL(LinuxConstants.RELEASES_API)
        val conn = url.openConnection() as HttpURLConnection
        conn.setRequestProperty("Accept", "application/vnd.github+json")
        conn.connectTimeout = 15000
        conn.readTimeout = 15000

        try {
            val response = conn.inputStream.bufferedReader().readText()
            val json = JSONObject(response)
            val assets = json.getJSONArray("assets")

            for (i in 0 until assets.length()) {
                val asset = assets.getJSONObject(i)
                if (asset.getString("name") == LinuxConstants.ASSET_NAME) {
                    return Pair(
                        asset.getString("browser_download_url"),
                        asset.getLong("size")
                    )
                }
            }
        } finally {
            conn.disconnect()
        }
        return Pair(null, 0L)
    }

    // ────────────────────────────────────────────────────────────
    //  Download with Resume Support
    // ────────────────────────────────────────────────────────────

    private suspend fun downloadFileWithResume(urlStr: String, destination: File, expectedSize: Long) {
        var downloaded = if (destination.exists()) destination.length() else 0L
        val totalSize = if (expectedSize > 0) expectedSize else getContentLength(urlStr)

        // If already fully downloaded, skip
        if (downloaded >= totalSize && totalSize > 0) {
            setProgressCompat(89, "Download complete, preparing extraction...")
            return
        }

        val url = URL(urlStr)
        val conn = url.openConnection() as HttpURLConnection
        
        if (downloaded > 0) {
            conn.setRequestProperty("Range", "bytes=$downloaded-")
        }
        
        conn.connectTimeout = 30000
        conn.readTimeout = 30000
        conn.instanceFollowRedirects = true
        conn.connect()

        // Handle redirect (GitHub redirects to CDN)
        val responseCode = conn.responseCode
        if (responseCode != 200 && responseCode != 206) {
            conn.disconnect()
            // Follow redirect manually if needed
            val redirectUrl = conn.getHeaderField("Location")
            if (redirectUrl != null) {
                conn.disconnect()
                downloadFileWithResume(redirectUrl, destination, expectedSize)
                return
            }
            throw Exception("Download failed: HTTP $responseCode")
        }

        val isResume = responseCode == 206
        if (!isResume) {
            downloaded = 0L // Server doesn't support range, restart
        }

        val serverSize = if (isResume) {
            downloaded + conn.contentLength.toLong()
        } else {
            conn.contentLength.toLong().let { if (it > 0) it else totalSize }
        }

        val input = BufferedInputStream(conn.inputStream, 8192)
        val output = if (isResume) {
            RandomAccessFile(destination, "rw").apply { seek(downloaded) }
        } else {
            RandomAccessFile(destination, "rw").apply { setLength(0) } // Truncate for fresh start
        }

        try {
            val buffer = ByteArray(8192)
            var count: Int
            var lastNotifyTime = System.currentTimeMillis()
            var lastNotifyBytes = downloaded
            var lastProgressPercent = -1

            while (input.read(buffer).also { count = it } != -1) {
                if (isStopped) {
                    throw Exception("Installation cancelled by user")
                }

                output.write(buffer, 0, count)
                downloaded += count

                val now = System.currentTimeMillis()
                val elapsed = now - lastNotifyTime

                // Update every 500ms or at least every 1%
                if (elapsed >= 500) {
                    val bytesSinceLastUpdate = downloaded - lastNotifyBytes
                    val speedBps = if (elapsed > 0) (bytesSinceLastUpdate * 1000L / elapsed) else 0L
                    val speedStr = formatSpeed(speedBps)

                    val progress = if (serverSize > 0) {
                        ((downloaded.toFloat() / serverSize) * 89).toInt()  // 0-89% for download
                    } else {
                        -1 // Indeterminate
                    }

                    val downloadedMB = downloaded / (1024 * 1024)
                    val totalMB = serverSize / (1024 * 1024)

                    if (progress != lastProgressPercent || elapsed >= 1000) {
                        val statusText = "Downloading: ${downloadedMB}MB / ${totalMB}MB ($speedStr)"
                        setProgressCompat(progress, statusText)
                        updateNotification(progress, statusText)

                        lastProgressPercent = progress
                        lastNotifyTime = now
                        lastNotifyBytes = downloaded
                    }
                }
            }
        } finally {
            output.close()
            input.close()
            conn.disconnect()
        }
    }

    private fun getContentLength(urlStr: String): Long {
        val url = URL(urlStr)
        val conn = url.openConnection() as HttpURLConnection
        conn.requestMethod = "HEAD"
        conn.instanceFollowRedirects = true
        conn.connect()
        val len = conn.contentLengthLong
        conn.disconnect()
        return if (len > 0) len else 0L
    }

    // ────────────────────────────────────────────────────────────
    //  Chroot Environment Setup
    // ────────────────────────────────────────────────────────────

    private fun setupChrootEnvironment() {
        val rootfs = LinuxConstants.ROOTFS_PATH
        val binDir = LinuxConstants.BIN_PATH

        // Create essential directories if missing
        execRoot("mkdir -p $rootfs/proc $rootfs/sys $rootfs/dev $rootfs/dev/pts $rootfs/sdcard $rootfs/tmp")

        // Setup DNS
        execRoot("""
            echo "nameserver 8.8.8.8" > $rootfs/etc/resolv.conf
            echo "nameserver 8.8.4.4" >> $rootfs/etc/resolv.conf
        """.trimIndent())

        // Setup hostname
        execRoot("echo 'gamerx-linux' > $rootfs/etc/hostname")

        // Create the chroot scripts in bin/
        createChrootScripts(binDir, rootfs)

        // Fix permissions
        execRoot("chmod -R 755 $binDir")
        execRoot("chmod 755 $rootfs")
    }

    private fun createChrootScripts(binDir: String, rootfs: String) {
        val linuxRoot = LinuxConstants.LINUX_ROOT

        // ── enter_arch.sh ──
        val enterScript = """
#!/system/bin/sh
# GamerX Linux - Enter Script (Chroot)
LINUX_DIR="$linuxRoot"
ROOTFS="${'$'}LINUX_DIR/rootfs"

if [ ! -d "${'$'}ROOTFS/etc" ]; then
    echo "[-] Error: Rootfs not found at ${'$'}ROOTFS"
    exit 1
fi

echo "[*] Mounting filesystems..."

mountpoint -q "${'$'}ROOTFS/proc" || mount -t proc proc "${'$'}ROOTFS/proc"
mountpoint -q "${'$'}ROOTFS/sys" || mount -t sysfs sysfs "${'$'}ROOTFS/sys"
mountpoint -q "${'$'}ROOTFS/dev" || mount -o bind /dev "${'$'}ROOTFS/dev"
mountpoint -q "${'$'}ROOTFS/dev/pts" || mount -t devpts devpts "${'$'}ROOTFS/dev/pts"

mkdir -p "${'$'}ROOTFS/sdcard"
mountpoint -q "${'$'}ROOTFS/sdcard" || mount -o bind /sdcard "${'$'}ROOTFS/sdcard"

# DNS
cat /system/etc/resolv.conf > "${'$'}ROOTFS/etc/resolv.conf" 2>/dev/null
echo "nameserver 8.8.8.8" >> "${'$'}ROOTFS/etc/resolv.conf"

echo "[*] Entering GamerX Linux..."
export HOME="/root"
export PATH="/bin:/usr/bin:/sbin:/usr/sbin"
export TERM="xterm-256color"

if [ -z "${'$'}1" ]; then
    chroot "${'$'}ROOTFS" /bin/bash -l
else
    chroot "${'$'}ROOTFS" /bin/bash -c "${'$'}@"
fi
""".trim()

        // ── start_gui.sh ──
        val startGuiScript = """
#!/system/bin/sh
# GamerX Linux - Start GUI (VNC)
LINUX_DIR="$linuxRoot"
ScriptDir="${'$'}LINUX_DIR/bin"

"${'$'}ScriptDir/enter_arch.sh" "echo '[*] Mounts verified'"

echo "[*] Starting VNC Server..."
"${'$'}ScriptDir/enter_arch.sh" "su - gamerx -c 'vncserver -kill :1 2>/dev/null; vncserver :1 -geometry 1920x1080 -depth 24'"

echo "[+] GUI Started at :1 (Port 5901)"
""".trim()

        // ── stop_arch.sh ──
        val stopScript = """
#!/system/bin/sh
# GamerX Linux - Stop Script
LINUX_DIR="$linuxRoot"
ROOTFS="${'$'}LINUX_DIR/rootfs"

echo "[*] Stopping GamerX Linux..."

echo "[*] Killing Services..."
pkill -f "vncserver"
pkill -f "Xvnc"
pkill -f "novnc"
pkill -f "cloudflared"
pkill -f "xfce4"
pkill -f "dbus-daemon"
pkill -f "ssh-agent"
pkill -f "pulseaudio"

echo "[*] Unmounting..."
umount -l "${'$'}ROOTFS/sdcard" 2>/dev/null
umount -l "${'$'}ROOTFS/dev/pts" 2>/dev/null
umount -l "${'$'}ROOTFS/dev" 2>/dev/null
umount -l "${'$'}ROOTFS/sys" 2>/dev/null
umount -l "${'$'}ROOTFS/proc" 2>/dev/null

echo "[+] Stopped."
""".trim()

        // ── start_web.sh ──
        val startWebScript = """
#!/system/bin/sh
# GamerX Linux - Start Web / Tunnel
LINUX_DIR="$linuxRoot"
ScriptDir="${'$'}LINUX_DIR/bin"
LOG_FILE="${'$'}LINUX_DIR/tunnel.log"
MODE="${'$'}1"

"${'$'}ScriptDir/start_gui.sh"

echo "[*] Starting noVNC..."
"${'$'}ScriptDir/enter_arch.sh" "su - gamerx -c 'nohup /usr/bin/novnc_server --vnc localhost:5901 --listen 0.0.0.0:6080 > /dev/null 2>&1 &'"

if [ "${'$'}MODE" == "local" ]; then
    echo "[+] Local Web Started at http://127.0.0.1:6080"
    exit 0
fi

if [ "${'$'}MODE" == "world" ]; then
    echo "[*] Starting Cloudflare Tunnel..."
    echo "" > "${'$'}LOG_FILE"
    "${'$'}ScriptDir/enter_arch.sh" "su - gamerx -c 'nohup cloudflared tunnel --url http://localhost:6080 > /sdcard/gamerx_tunnel.log 2>&1 &'"
    echo "[+] Cloudflare Tunnel Started. Check Logs."
fi
""".trim()

        // Write scripts via root
        writeFileAsRoot("$binDir/enter_arch.sh", enterScript)
        writeFileAsRoot("$binDir/start_gui.sh", startGuiScript)
        writeFileAsRoot("$binDir/stop_arch.sh", stopScript)
        writeFileAsRoot("$binDir/start_web.sh", startWebScript)
    }

    private fun writeFileAsRoot(path: String, content: String) {
        // Write to a temp file in app cache, then copy via root
        val tempFile = File(applicationContext.cacheDir, "script_temp_${System.currentTimeMillis()}.sh")
        tempFile.writeText(content)
        execRoot("cp '${tempFile.absolutePath}' '$path' && chmod 755 '$path'")
        tempFile.delete()
    }

    // ────────────────────────────────────────────────────────────
    //  Root Command Execution
    // ────────────────────────────────────────────────────────────

    private fun execRoot(command: String): Pair<Int, String> {
        return try {
            val process = ProcessBuilder("su", "-c", command).start()
            val output = process.inputStream.bufferedReader().readText()
            val error = process.errorStream.bufferedReader().readText()
            process.waitFor()
            Pair(process.exitValue(), output + error)
        } catch (e: Exception) {
            Pair(-1, e.message ?: "Unknown error")
        }
    }

    // ────────────────────────────────────────────────────────────
    //  Notification Helpers
    // ────────────────────────────────────────────────────────────

    private fun createNotificationChannel() {
        val channel = NotificationChannel(
            LinuxConstants.CHANNEL_ID,
            "Linux Installer",
            NotificationManager.IMPORTANCE_LOW
        ).apply {
            description = "Download and installation progress for GamerX Linux"
        }
        notificationManager.createNotificationChannel(channel)
    }

    private suspend fun setForegroundCompat(info: ForegroundInfo) {
        setForeground(info)
    }

    private suspend fun setProgressCompat(progress: Int, status: String) {
        setProgress(
            workDataOf(
                LinuxConstants.KEY_PROGRESS to progress,
                LinuxConstants.KEY_STATUS to status
            )
        )
    }

    private fun updateNotification(progress: Int, status: String) {
        val builder = NotificationCompat.Builder(applicationContext, LinuxConstants.CHANNEL_ID)
            .setContentTitle("GamerX Linux")
            .setContentText(status)
            .setSmallIcon(android.R.drawable.stat_sys_download)
            .setOngoing(progress in 0..99)
            .setSilent(true)

        if (progress in 0..100) {
            builder.setProgress(100, progress, false)
        } else if (progress < 0) {
            // Error state
            builder.setSmallIcon(android.R.drawable.stat_notify_error)
            builder.setOngoing(false)
        }

        if (progress == 100) {
            builder.setSmallIcon(android.R.drawable.stat_sys_download_done)
            builder.setOngoing(false)
            builder.setAutoCancel(true)
        }

        notificationManager.notify(LinuxConstants.NOTIFICATION_ID, builder.build())
    }

    private fun createForegroundInfo(progress: Int, status: String): ForegroundInfo {
        val notification = NotificationCompat.Builder(applicationContext, LinuxConstants.CHANNEL_ID)
            .setContentTitle("GamerX Linux")
            .setContentText(status)
            .setSmallIcon(android.R.drawable.stat_sys_download)
            .setProgress(100, progress, progress < 0)
            .setOngoing(true)
            .setSilent(true)
            .build()

        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            ForegroundInfo(LinuxConstants.NOTIFICATION_ID, notification, ServiceInfo.FOREGROUND_SERVICE_TYPE_DATA_SYNC)
        } else {
            ForegroundInfo(LinuxConstants.NOTIFICATION_ID, notification)
        }
    }

    // ────────────────────────────────────────────────────────────
    //  Utility
    // ────────────────────────────────────────────────────────────

    private fun formatSpeed(bytesPerSecond: Long): String {
        return when {
            bytesPerSecond >= 1_000_000 -> String.format("%.1f MB/s", bytesPerSecond / 1_000_000.0)
            bytesPerSecond >= 1_000 -> String.format("%.0f KB/s", bytesPerSecond / 1_000.0)
            else -> "$bytesPerSecond B/s"
        }
    }
}

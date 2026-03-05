package com.gamerx.manager.feature.linux

/**
 * Shared constants for the GamerX Linux feature.
 * Single source of truth for all paths and URLs.
 */
object LinuxConstants {
    // Install location (root-owned, persistent across module updates)
    const val LINUX_ROOT = "/data/data/GamerX-Linux"
    const val ROOTFS_PATH = "$LINUX_ROOT/rootfs"
    const val BIN_PATH = "$LINUX_ROOT/bin"

    // GitHub Releases API
    const val RELEASES_API = "https://api.github.com/repos/GamerX3560/GamerX-Linux-Android/releases/latest"
    const val ASSET_NAME = "GamerX_Linux_ARM64.tar.gz"

    // Notification
    const val CHANNEL_ID = "linux_installer"
    const val NOTIFICATION_ID = 2001

    // WorkManager tag
    const val WORK_TAG = "linux_install"

    // Progress keys (WorkManager Data)
    const val KEY_PROGRESS = "progress"
    const val KEY_STATUS = "status"
    const val KEY_SPEED = "speed"
    const val KEY_ERROR = "error"
}

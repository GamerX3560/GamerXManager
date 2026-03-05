# GamerX Manager

GamerX Manager is a custom Android companion application designed to provide advanced control and customization for rooted devices (particularly through KernelSU and Magisk modules).

## Features
- **Script Management**: Install and run custom shell scripts with interactive UIs (dynamically generated from manifest.json).
- **Icon Packs & Boot Animations**: Apply systemless customizations dynamically.
- **Linux Environment**: Installs and manages a chroot Linux environment (Arch) directly from the app with VNC/Web integration.
- **Quick Settings Tiles**: Convenient quick access to custom scripts via QS tiles.

## Architecture
Built with Kotlin and Jetpack Compose. Utilizes background WorkManager for heavy downloads and rooted shell commands for system-level operations.

## Setup
To build the app, simply import the project into Android Studio and run the `app` configuration.

## License
MIT License

# Hot Folder Plugin

[![Build](https://github.com/aamurad/webstorm-hot-folder-plugin/workflows/Build/badge.svg)](https://github.com/aamurad/webstorm-hot-folder-plugin/actions)

<!-- Plugin description -->
**Hot Folder** plugin allows you to add shared folders from anywhere on your file system to your IDE workspace.

This plugin is perfect for sharing common libraries or resources between multiple projects, providing quick access to frequently used external directories, and keeping reference documentation or templates visible while working.

<!-- Plugin description end -->

## Features

- 📁 **Add Multiple Hot Folders** - Add as many shared folders as you need
- 🪟 **Dedicated Tool Window** - Movable tool window that can be positioned anywhere in your IDE
- 🔄 **Automatic Refresh** - File system watching ensures your view is always up-to-date
- 📂 **Full File Operations** - Create, copy, paste, delete files and folders
- 🔀 **Drag-and-Drop Support** - Drag files between project view and hot folders seamlessly
- ⚙️ **Settings Integration** - Manage hot folders via Settings → Tools → Hot Folders
- 🎨 **Custom Icon** - Distinctive hot folder icon for easy identification
- 🧹 **Clean View** - Automatically hides system files like .DS_Store

## Installation

### From JetBrains Marketplace (Recommended)

1. Open your IDE (WebStorm, IntelliJ IDEA, PhpStorm, etc.)
2. Go to **Settings/Preferences** → **Plugins**
3. Search for "**Hot Folder**"
4. Click **Install** and restart your IDE

### Manual Installation

1. Download the latest release from the [Releases](https://github.com/aamurad/webstorm-hot-folder-plugin/releases) page
2. Open your IDE
3. Go to **Settings/Preferences** → **Plugins**
4. Click the gear icon ⚙️ → **Install Plugin from Disk...**
5. Select the downloaded ZIP file
6. Restart your IDE

## Usage

### Adding Hot Folders

**Method 1: Via Tool Window**
1. Open the **Hot Folders** tool window (usually on the right side)
2. Click the **+** button in the toolbar
3. Browse and select the folder you want to add
4. The folder will appear immediately with all its contents

**Method 2: Via Settings**
1. Go to **Settings/Preferences** → **Tools** → **Hot Folders**
2. Click the **+** button
3. Select the folder you want to add
4. Click **OK** to apply changes

**Method 3: Via Context Menu**
1. Right-click in the **Project** view
2. Select **Add Hot Folder**
3. Choose the folder from the file chooser

### Managing Hot Folders

- **Remove folders**: Click the **-** button in the tool window toolbar or settings page
- **Refresh**: Click the refresh button to manually update the view
- **Navigate**: Double-click any file to open it in the editor
- **File operations**: Right-click on files/folders for context menu options
- **Drag to hot folders**: Drag files from project view to hot folders to copy or move them
- **Drag from hot folders**: Drag files from hot folders to project view (always creates a copy)

### Configuration

Hot folder settings are saved per project in `.idea/hotfolder.xml`. By default, this file is not committed to version control (`.idea` is typically in `.gitignore`).

If you want to share hot folder configuration with your team:
1. Modify `.gitignore` to include `hotfolder.xml`:
   ```gitignore
   .idea/*
   !.idea/hotfolder.xml
   ```

## Use Cases

- **Shared Libraries**: Access common code libraries across multiple projects
- **Templates**: Keep project templates or boilerplate code readily available
- **Documentation**: Reference documentation or specifications while coding
- **Team Resources**: Share team-wide resources without duplicating them
- **External Dependencies**: Quick access to external dependencies or SDKs

## Compatibility

This plugin is compatible with all IntelliJ Platform-based IDEs:
- IntelliJ IDEA
- WebStorm
- PhpStorm
- PyCharm
- RubyMine
- GoLand
- CLion
- Rider
- Android Studio

**Minimum version**: 2025.2 (Build 252)

## Building from Source

```bash
# Clone the repository
git clone https://github.com/aamurad/webstorm-hot-folder-plugin.git
cd webstorm-hot-folder-plugin

# Build the plugin
./gradlew buildPlugin

# The plugin ZIP will be in build/distributions/
```

## Development

```bash
# Run tests
./gradlew test

# Run the plugin in a sandbox IDE
./gradlew runIde

# Verify plugin compatibility
./gradlew verifyPlugin
```

## Contributing

Contributions are welcome! Please feel free to submit a Pull Request.

## License

This is a **commercial plugin** available through the JetBrains Marketplace on a subscription basis.

By using this plugin, you agree to the [End User License Agreement (EULA)](LICENSE).

### Subscription

- The plugin requires an active subscription purchased through the JetBrains Marketplace
- Subscription options include monthly and yearly plans
- A free trial period may be available for new users

## Support

If you encounter any issues or have suggestions:
- Open an issue on [GitHub Issues](https://github.com/aamurad/webstorm-hot-folder-plugin/issues)
- Contact: aamurad@hotmail.com

## Changelog

See [CHANGELOG.md](CHANGELOG.md) for a list of changes in each version.

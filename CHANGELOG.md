# Changelog

All notable changes to the Hot Folder plugin will be documented in this file.

The format is based on [Keep a Changelog](https://keepachangelog.com/en/1.0.0/),
and this project adheres to [Semantic Versioning](https://semver.org/spec/v2.0.0.html).

## [Unreleased]

### Added
- Drag-and-drop support: drag files from project view to hot folders
- Drag-and-drop support: drag files from hot folders to project (creates a copy)
- Automatic filtering of .DS_Store files from hot folder tree view

### Changed
- Improved file visibility by hiding system files (.DS_Store) automatically

## [1.0.0] - 2026-01-09

### Added
- Initial release of Hot Folder plugin
- Add multiple hot folders to your IDE workspace
- Dedicated tool window with tree view for browsing hot folder contents
- Toolbar with Add, Remove, and Refresh actions
- Context menu with full file operations (New, Copy, Paste, Delete, Rename, etc.)
- Double-click to open files in the editor
- Settings page for managing hot folders (Settings → Tools → Hot Folders)
- Automatic file system watching and refresh when files change
- Custom hot folder icon for both light and dark themes
- Support for all IntelliJ Platform-based IDEs (IntelliJ IDEA, WebStorm, PhpStorm, PyCharm, etc.)
- Per-project configuration stored in `.idea/hotfolder.xml`

### Features
- **Multiple Hot Folders**: Add as many shared folders as needed
- **Movable Tool Window**: Position the Hot Folders tool window anywhere in your IDE
- **Real-time Updates**: Automatic refresh when files are added, modified, or deleted
- **Full Navigation**: Browse folder structure and open files directly
- **File Operations**: Complete support for creating, copying, pasting, and deleting files
- **Persistent Configuration**: Hot folder settings are saved per project

### Use Cases
- Share common libraries or resources between multiple projects
- Quick access to frequently used external directories
- Keep reference documentation or templates visible while working
- Access shared team resources without copying them into each project

[Unreleased]: https://github.com/aamurad/webstorm-hot-folder-plugin/compare/v1.0.0...HEAD
[1.0.0]: https://github.com/aamurad/webstorm-hot-folder-plugin/releases/tag/v1.0.0

package com.github.abdulaziz.hotfolder.toolWindow

import com.github.abdulaziz.hotfolder.services.HotFolderService
import com.intellij.openapi.actionSystem.*
import com.intellij.openapi.fileChooser.FileChooser
import com.intellij.openapi.fileChooser.FileChooserDescriptorFactory
import com.intellij.openapi.fileEditor.FileEditorManager
import com.intellij.openapi.project.DumbAware
import com.intellij.openapi.project.Project
import com.intellij.openapi.vfs.LocalFileSystem
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.openapi.vfs.VirtualFileManager
import com.intellij.openapi.vfs.newvfs.BulkFileListener
import com.intellij.openapi.vfs.newvfs.events.VFileEvent
import com.intellij.openapi.wm.ToolWindow
import com.intellij.openapi.wm.ToolWindowFactory
import com.intellij.ui.ColoredTreeCellRenderer
import com.intellij.ui.PopupHandler
import com.intellij.ui.SimpleTextAttributes
import com.intellij.ui.content.ContentFactory
import com.intellij.ui.treeStructure.Tree
import java.awt.BorderLayout
import java.awt.event.MouseAdapter
import java.awt.event.MouseEvent
import javax.swing.JPanel
import javax.swing.JTree
import javax.swing.tree.DefaultMutableTreeNode
import javax.swing.tree.DefaultTreeModel
import javax.swing.tree.TreePath
import com.intellij.icons.AllIcons
import com.intellij.openapi.util.IconLoader
import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.application.ModalityState
import com.intellij.openapi.application.ReadAction
import com.intellij.openapi.vfs.VfsUtil
import com.intellij.psi.PsiManager
import com.intellij.refactoring.copy.CopyFilesOrDirectoriesHandler
import com.intellij.refactoring.move.moveFilesOrDirectories.MoveFilesOrDirectoriesUtil
import java.awt.datatransfer.DataFlavor
import java.io.File
import java.awt.datatransfer.Transferable
import javax.swing.DropMode
import javax.swing.TransferHandler
import javax.swing.JComponent

class HotFolderToolWindowFactory : ToolWindowFactory, DumbAware {

    override fun createToolWindowContent(project: Project, toolWindow: ToolWindow) {
        val panel = HotFolderPanel(project)
        val content = ContentFactory.getInstance().createContent(panel, "", false)
        toolWindow.contentManager.addContent(content)
    }
}

class HotFolderPanel(private val project: Project) : JPanel(BorderLayout()) {
    private val rootNode = DefaultMutableTreeNode("Hot Folders")
    private val treeModel = DefaultTreeModel(rootNode)
    private val tree = Tree(treeModel)
    private val service = HotFolderService.getInstance(project)

    init {
        tree.isRootVisible = false
        tree.showsRootHandles = true
        tree.cellRenderer = HotFolderTreeCellRenderer()
        
        // Enable drag and drop using TransferHandler
        tree.dragEnabled = true
        tree.dropMode = DropMode.ON_OR_INSERT
        tree.transferHandler = HotFolderTransferHandler(project, service, this)
        
        // Double-click to open files
        tree.addMouseListener(object : MouseAdapter() {
            override fun mouseClicked(e: MouseEvent) {
                if (e.clickCount == 2) {
                    val path = tree.getPathForLocation(e.x, e.y) ?: return
                    val node = path.lastPathComponent as? DefaultMutableTreeNode ?: return
                    val file = node.userObject as? VirtualFile ?: return
                    if (!file.isDirectory) {
                        FileEditorManager.getInstance(project).openFile(file, true)
                    }
                }
            }
        })
        
        // Context menu
        tree.addMouseListener(object : PopupHandler() {
            override fun invokePopup(comp: java.awt.Component, x: Int, y: Int) {
                val path = tree.getPathForLocation(x, y)
                if (path != null) {
                    tree.selectionPath = path
                }
                showContextMenu(x, y)
            }
        })
        
        // Toolbar with add/remove actions
        val toolbar = createToolbar()
        
        add(toolbar, BorderLayout.NORTH)
        add(com.intellij.ui.components.JBScrollPane(tree), BorderLayout.CENTER)
        
        // Initial load
        refreshTree()
        
        // Listen for VFS changes
        project.messageBus.connect().subscribe(VirtualFileManager.VFS_CHANGES, object : BulkFileListener {
            override fun after(events: List<VFileEvent>) {
                val hotFolderPaths = service.getHotFolders()
                val shouldRefresh = events.any { event ->
                    val eventPath = event.path
                    hotFolderPaths.any { hotFolderPath ->
                        eventPath.startsWith(hotFolderPath)
                    }
                }
                if (shouldRefresh) {
                    refreshTree()
                }
            }
        })
    }
    
    private fun createToolbar(): javax.swing.JComponent {
        val actionGroup = DefaultActionGroup().apply {
            add(object : AnAction("Add Hot Folder", "Add a new hot folder", AllIcons.General.Add) {
                override fun actionPerformed(e: AnActionEvent) {
                    addHotFolder()
                }
            })
            add(object : AnAction("Remove Hot Folder", "Remove selected hot folder", AllIcons.General.Remove) {
                override fun actionPerformed(e: AnActionEvent) {
                    removeSelectedHotFolder()
                }
                
                override fun update(e: AnActionEvent) {
                    val path = tree.selectionPath
                    val node = path?.lastPathComponent as? DefaultMutableTreeNode
                    val file = node?.userObject as? VirtualFile
                    e.presentation.isEnabled = file != null && service.getHotFolders().contains(file.path)
                }
                
                override fun getActionUpdateThread(): ActionUpdateThread = ActionUpdateThread.EDT
            })
            addSeparator()
            add(object : AnAction("Refresh", "Refresh hot folders", AllIcons.Actions.Refresh) {
                override fun actionPerformed(e: AnActionEvent) {
                    refreshTree()
                }
            })
        }
        
        val toolbar = ActionManager.getInstance().createActionToolbar("HotFolderToolbar", actionGroup, true)
        toolbar.targetComponent = this
        return toolbar.component
    }
    
    private fun showContextMenu(x: Int, y: Int) {
        val actionGroup = DefaultActionGroup().apply {
            val path = tree.selectionPath
            val node = path?.lastPathComponent as? DefaultMutableTreeNode
            val file = node?.userObject as? VirtualFile
            
            if (file != null) {
                if (!file.isDirectory) {
                    add(object : AnAction("Open", "Open file", AllIcons.Actions.MenuOpen) {
                        override fun actionPerformed(e: AnActionEvent) {
                            FileEditorManager.getInstance(project).openFile(file, true)
                        }
                    })
                    addSeparator()
                }
                
                if (service.getHotFolders().contains(file.path)) {
                    add(object : AnAction("Remove Hot Folder", "Remove this hot folder", AllIcons.General.Remove) {
                        override fun actionPerformed(e: AnActionEvent) {
                            service.removeHotFolder(file.path)
                            refreshTree()
                        }
                    })
                }
                
                add(object : AnAction("Open in File Manager", "Open in system file manager", AllIcons.Actions.MenuOpen) {
                    override fun actionPerformed(e: AnActionEvent) {
                        com.intellij.ide.actions.RevealFileAction.openFile(file.toNioPath().toFile())
                    }
                })
            } else {
                add(object : AnAction("Add Hot Folder", "Add a new hot folder", AllIcons.General.Add) {
                    override fun actionPerformed(e: AnActionEvent) {
                        addHotFolder()
                    }
                })
            }
        }
        
        val popupMenu = ActionManager.getInstance().createActionPopupMenu("HotFolderPopup", actionGroup)
        popupMenu.component.show(tree, x, y)
    }
    
    private fun addHotFolder() {
        val descriptor = FileChooserDescriptorFactory.createSingleFolderDescriptor()
        descriptor.title = "Select Hot Folder"
        descriptor.description = "This folder will be shown in the Hot Folders tool window."
        
        FileChooser.chooseFile(descriptor, project, null) { file ->
            service.addHotFolder(file.path)
            refreshTree()
        }
    }
    
    private fun removeSelectedHotFolder() {
        val path = tree.selectionPath ?: return
        val node = path.lastPathComponent as? DefaultMutableTreeNode ?: return
        val file = node.userObject as? VirtualFile ?: return
        
        if (service.getHotFolders().contains(file.path)) {
            service.removeHotFolder(file.path)
            refreshTree()
        }
    }
    
    fun refreshTree() {
        rootNode.removeAllChildren()
        
        val hotFolders = service.getHotFolders()
        val fs = LocalFileSystem.getInstance()
        
        for (folderPath in hotFolders) {
            val file = fs.refreshAndFindFileByPath(folderPath)
            if (file != null && file.isDirectory && file.isValid) {
                val folderNode = DefaultMutableTreeNode(file)
                addChildren(folderNode, file)
                rootNode.add(folderNode)
            }
        }
        
        treeModel.reload()
        
        // Expand first level
        for (i in 0 until rootNode.childCount) {
            val child = rootNode.getChildAt(i) as DefaultMutableTreeNode
            tree.expandPath(TreePath(arrayOf(rootNode, child)))
        }
    }
    
    private fun addChildren(parentNode: DefaultMutableTreeNode, parentFile: VirtualFile) {
        val children = parentFile.children
            .filter { it.name != ".DS_Store" } // Filter out .DS_Store files
            .sortedWith(compareBy({ !it.isDirectory }, { it.name.lowercase() }))
        for (child in children) {
            val childNode = DefaultMutableTreeNode(child)
            if (child.isDirectory) {
                addChildren(childNode, child)
            }
            parentNode.add(childNode)
        }
    }
    
    fun getTree(): Tree = tree
}

/**
 * Custom Transferable for VirtualFile drag operations
 */
class FileTransferable(private val files: List<File>) : Transferable {
    override fun getTransferDataFlavors(): Array<DataFlavor> {
        return arrayOf(DataFlavor.javaFileListFlavor)
    }

    override fun isDataFlavorSupported(flavor: DataFlavor): Boolean {
        return flavor == DataFlavor.javaFileListFlavor
    }

    override fun getTransferData(flavor: DataFlavor): Any {
        if (flavor == DataFlavor.javaFileListFlavor) {
            return files
        }
        throw java.awt.datatransfer.UnsupportedFlavorException(flavor)
    }
}

/**
 * TransferHandler for drag-and-drop operations in the Hot Folder tree
 */
class HotFolderTransferHandler(
    private val project: Project,
    private val service: HotFolderService,
    private val panel: HotFolderPanel
) : TransferHandler() {
    
    override fun getSourceActions(c: JComponent): Int {
        return COPY_OR_MOVE
    }
    
    override fun createTransferable(c: JComponent): Transferable? {
        val tree = c as? JTree ?: return null
        val path = tree.selectionPath ?: return null
        val node = path.lastPathComponent as? DefaultMutableTreeNode ?: return null
        val file = node.userObject as? VirtualFile ?: return null
        
        return FileTransferable(listOf(file.toNioPath().toFile()))
    }
    
    override fun canImport(support: TransferSupport): Boolean {
        if (!support.isDrop) return false
        if (!support.isDataFlavorSupported(DataFlavor.javaFileListFlavor)) return false
        
        val tree = support.component as? JTree ?: return false
        val dropLocation = support.dropLocation as? JTree.DropLocation ?: return false
        val path = dropLocation.path ?: return false
        val node = path.lastPathComponent as? DefaultMutableTreeNode ?: return false
        val targetFile = node.userObject as? VirtualFile ?: return false
        
        return targetFile.isDirectory
    }
    
    override fun importData(support: TransferSupport): Boolean {
        if (!canImport(support)) return false
        
        val tree = support.component as? JTree ?: return false
        val dropLocation = support.dropLocation as? JTree.DropLocation ?: return false
        val path = dropLocation.path ?: return false
        val node = path.lastPathComponent as? DefaultMutableTreeNode ?: return false
        val targetDir = node.userObject as? VirtualFile ?: return false
        
        if (!targetDir.isDirectory) return false
        
        try {
            @Suppress("UNCHECKED_CAST")
            val files = support.transferable.getTransferData(DataFlavor.javaFileListFlavor) as List<File>
            val isCopy = support.dropAction == COPY
            
            // Perform file operations asynchronously with proper modality state
            // This ensures proper threading context for PSI operations and dialogs
            ApplicationManager.getApplication().invokeLater({
                handleFileDrop(files, targetDir, isCopy)
            }, ModalityState.defaultModalityState())
            return true
        } catch (e: Exception) {
            return false
        }
    }
    
    override fun exportDone(source: JComponent?, data: Transferable?, action: Int) {
        // Always call super to properly clean up the drag-and-drop operation
        // This is critical to release input handling and restore touchpad gestures
        super.exportDone(source, data, action)
        
        // Refresh after move operation
        if (action == MOVE) {
            panel.refreshTree()
        }
    }
    
    private fun handleFileDrop(files: List<File>, targetDir: VirtualFile, isCopy: Boolean) {
        // Wrap PSI operations in ReadAction to ensure proper thread access
        val (targetPsiDir, sourceFiles, shouldCopy) = ReadAction.compute<Triple<com.intellij.psi.PsiDirectory?, Array<com.intellij.psi.PsiFileSystemItem>, Boolean>, Throwable> {
            val psiManager = PsiManager.getInstance(project)
            val targetDir = psiManager.findDirectory(targetDir)
            
            val sources = files.mapNotNull<File, com.intellij.psi.PsiFileSystemItem> { file ->
                val vFile = VfsUtil.findFileByIoFile(file, true)
                if (vFile != null) {
                    if (vFile.isDirectory) {
                        psiManager.findDirectory(vFile)
                    } else {
                        psiManager.findFile(vFile)
                    }
                } else {
                    null
                }
            }.toTypedArray()
            
            // When dragging from hot folders to project, always copy
            // When dragging from project to hot folders, respect the action (move or copy)
            val copy = isCopy || 
                       sources.any { element ->
                           val vFile = element.virtualFile
                           vFile != null && service.getHotFolders().any { hotFolder ->
                               vFile.path.startsWith(hotFolder)
                           }
                       }
            
            Triple(targetDir, sources, copy)
        }
        
        if (targetPsiDir == null || sourceFiles.isEmpty()) return
        
        if (shouldCopy) {
            // Copy files one by one
            for (sourceFile in sourceFiles) {
                CopyFilesOrDirectoriesHandler.copyToDirectory(sourceFile, null, targetPsiDir)
            }
        } else {
            // Move files
            MoveFilesOrDirectoriesUtil.doMove(project, sourceFiles, arrayOf(targetPsiDir), null)
        }
        
        panel.refreshTree()
    }
}

class HotFolderTreeCellRenderer : ColoredTreeCellRenderer() {
    companion object {
        val HOT_FOLDER_ICON = IconLoader.getIcon("/icons/hotFolder.svg", HotFolderTreeCellRenderer::class.java)
    }
    
    override fun customizeCellRenderer(
        tree: JTree,
        value: Any?,
        selected: Boolean,
        expanded: Boolean,
        leaf: Boolean,
        row: Int,
        hasFocus: Boolean
    ) {
        val node = value as? DefaultMutableTreeNode ?: return
        val file = node.userObject as? VirtualFile
        
        if (file != null) {
            append(file.name, SimpleTextAttributes.REGULAR_ATTRIBUTES)
            icon = if (file.isDirectory) {
                // Check if this is a root hot folder
                val parent = node.parent as? DefaultMutableTreeNode
                if (parent?.userObject == "Hot Folders") {
                    HOT_FOLDER_ICON
                } else {
                    AllIcons.Nodes.Folder
                }
            } else {
                file.fileType.icon ?: AllIcons.FileTypes.Any_type
            }
        } else if (node.userObject is String) {
            append(node.userObject as String, SimpleTextAttributes.REGULAR_BOLD_ATTRIBUTES)
            icon = HOT_FOLDER_ICON
        }
    }
}

package com.github.abdulaziz.hotfolder.toolWindow

import com.github.abdulaziz.hotfolder.services.HotFolderService
import com.intellij.ide.DataManager
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
import com.intellij.util.ui.JBUI
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
        val children = parentFile.children.sortedWith(compareBy({ !it.isDirectory }, { it.name.lowercase() }))
        for (child in children) {
            val childNode = DefaultMutableTreeNode(child)
            if (child.isDirectory) {
                addChildren(childNode, child)
            }
            parentNode.add(childNode)
        }
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

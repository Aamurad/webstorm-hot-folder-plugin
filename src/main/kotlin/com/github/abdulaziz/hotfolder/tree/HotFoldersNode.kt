package com.github.aamurad.hotfolder.tree

import com.github.aamurad.hotfolder.services.HotFolderService
import com.intellij.ide.projectView.PresentationData
import com.intellij.ide.projectView.ViewSettings
import com.intellij.ide.util.treeView.AbstractTreeNode
import com.intellij.openapi.project.Project
import com.intellij.openapi.vfs.LocalFileSystem
import com.intellij.psi.PsiManager
import com.intellij.ui.SimpleTextAttributes
import com.intellij.icons.AllIcons

class HotFoldersNode(
    project: Project,
    private val viewSettings: ViewSettings?
) : AbstractTreeNode<String>(project, "Hot Folders") {

    override fun getChildren(): Collection<AbstractTreeNode<*>> {
        val project = project ?: return emptyList()
        val hotFolders = HotFolderService.getInstance(project).getHotFolders()
        val psiManager = PsiManager.getInstance(project)
        val fs = LocalFileSystem.getInstance()
        
        val children = mutableListOf<AbstractTreeNode<*>>()
        for (path in hotFolders) {
            // Use refreshAndFindFileByPath to ensure the file and its children are loaded
            val file = fs.refreshAndFindFileByPath(path)
            if (file != null && file.isDirectory && file.isValid) {
                // Ensure children are loaded in VFS
                file.children
                val psiDir = psiManager.findDirectory(file)
                if (psiDir != null && psiDir.isValid) {
                    children.add(HotFolderDirectoryNode(project, psiDir, viewSettings))
                }
            }
        }
        return children
    }

    override fun update(presentation: PresentationData) {
        presentation.presentableText = "Hot Folders"
        presentation.setIcon(AllIcons.Nodes.Folder)
        presentation.addText("Hot Folders", SimpleTextAttributes.REGULAR_ATTRIBUTES)
    }
}

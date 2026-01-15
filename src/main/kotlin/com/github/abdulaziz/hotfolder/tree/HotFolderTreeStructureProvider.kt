package com.github.abdulaziz.hotfolder.tree

import com.github.abdulaziz.hotfolder.services.HotFolderService
import com.intellij.ide.projectView.TreeStructureProvider
import com.intellij.ide.projectView.ViewSettings
import com.intellij.ide.projectView.impl.nodes.PsiDirectoryNode
import com.intellij.ide.util.treeView.AbstractTreeNode
import com.intellij.openapi.project.Project
import com.intellij.openapi.vfs.LocalFileSystem

class HotFolderTreeStructureProvider : TreeStructureProvider {
    override fun modify(
        parent: AbstractTreeNode<*>,
        children: MutableCollection<AbstractTreeNode<*>>,
        settings: ViewSettings?
    ): MutableCollection<AbstractTreeNode<*>> {
        val project = parent.project ?: return children
        
        // Add the Hot Folders parent node at the project root level
        if (parent is PsiDirectoryNode && parent.virtualFile == project.guessProjectDir()) {
            val hotFolders = HotFolderService.getInstance(project).getHotFolders()
            
            // Only add the parent node if there are hot folders configured
            if (hotFolders.isNotEmpty()) {
                // Insert at the beginning to ensure it's visible and not pushed down by other nodes
                val newChildren = mutableListOf<AbstractTreeNode<*>>()
                newChildren.add(HotFoldersNode(project, settings))
                newChildren.addAll(children)
                return newChildren
            }
        }
        
        // Replace PsiDirectoryNode children with HotFolderDirectoryNode if parent is a hot folder
        // Keep all other nodes (like PsiFileNode) as-is to show files, but filter out .DS_Store
        if (parent is HotFoldersNode || parent is HotFolderDirectoryNode || isInsideHotFolder(parent, project)) {
            val newChildren = mutableListOf<AbstractTreeNode<*>>()
            for (child in children) {
                // Filter out .DS_Store files
                val virtualFile = when (child) {
                    is PsiDirectoryNode -> child.virtualFile
                    else -> (child as? AbstractTreeNode<*>)?.let { node ->
                        // Try to get virtual file from the node's value
                        when (val value = node.value) {
                            is com.intellij.psi.PsiFile -> value.virtualFile
                            else -> null
                        }
                    }
                }
                
                if (virtualFile?.name == ".DS_Store") {
                    continue // Skip .DS_Store files
                }
                
                if (child is PsiDirectoryNode && child !is HotFolderDirectoryNode) {
                    val psiDir = child.value
                    if (psiDir != null) {
                        newChildren.add(HotFolderDirectoryNode(project, psiDir, settings))
                    } else {
                        newChildren.add(child)
                    }
                } else {
                    // Keep all other node types (files, etc.) as-is
                    newChildren.add(child)
                }
            }
            return newChildren
        }
        
        return children
    }
    
    private fun isInsideHotFolder(node: AbstractTreeNode<*>, project: Project): Boolean {
        if (node !is PsiDirectoryNode) return false
        val virtualFile = node.virtualFile ?: return false
        val hotFolders = HotFolderService.getInstance(project).getHotFolders()
        
        for (hotFolderPath in hotFolders) {
            if (virtualFile.path.startsWith(hotFolderPath)) {
                return true
            }
        }
        return false
    }
}

// Extension function to guess project dir if not available in older versions, 
// though template uses a recent version
fun Project.guessProjectDir() = LocalFileSystem.getInstance().findFileByPath(basePath ?: "")

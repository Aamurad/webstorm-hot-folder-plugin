package com.github.abdulaziz.hotfolder.tree

import com.intellij.ide.projectView.ViewSettings
import com.intellij.ide.projectView.impl.nodes.PsiDirectoryNode
import com.intellij.ide.util.treeView.AbstractTreeNode
import com.intellij.openapi.project.Project
import com.intellij.psi.PsiDirectory
import com.intellij.psi.PsiElement

class HotFolderDirectoryNode(
    project: Project,
    value: PsiDirectory,
    viewSettings: ViewSettings?
) : PsiDirectoryNode(project, value, viewSettings) {

    override fun canNavigate(): Boolean = true

    override fun canNavigateToSource(): Boolean = true

    override fun canRepresent(element: Any?): Boolean {
        if (element is PsiElement) {
            return super.canRepresent(element)
        }
        return false
    }
}

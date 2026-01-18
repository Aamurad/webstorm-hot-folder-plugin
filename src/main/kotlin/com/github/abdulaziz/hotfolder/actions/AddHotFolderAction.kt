package com.github.aamurad.hotfolder.actions

import com.github.aamurad.hotfolder.services.HotFolderService
import com.intellij.ide.projectView.ProjectView
import com.intellij.openapi.actionSystem.AnAction
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.fileChooser.FileChooser
import com.intellij.openapi.fileChooser.FileChooserDescriptorFactory

class AddHotFolderAction : AnAction() {
    override fun actionPerformed(e: AnActionEvent) {
        val project = e.project ?: return
        val descriptor = FileChooserDescriptorFactory.createSingleFolderDescriptor()
        descriptor.title = "Select Hot Folder"
        descriptor.description = "This folder will be shown in the project view."

        FileChooser.chooseFile(descriptor, project, null) { file ->
            HotFolderService.getInstance(project).addHotFolder(file.path)
            ProjectView.getInstance(project).refresh()
        }
    }
}

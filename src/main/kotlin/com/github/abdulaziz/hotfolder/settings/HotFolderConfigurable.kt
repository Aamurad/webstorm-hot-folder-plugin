package com.github.aamurad.hotfolder.settings

import com.github.aamurad.hotfolder.services.HotFolderService
import com.intellij.openapi.fileChooser.FileChooserDescriptorFactory
import com.intellij.openapi.options.BoundConfigurable
import com.intellij.openapi.project.Project
import com.intellij.openapi.ui.DialogPanel
import com.intellij.ui.dsl.builder.panel
import com.intellij.ui.dsl.builder.RowLayout
import com.intellij.ide.projectView.ProjectView
import com.intellij.openapi.ui.TextFieldWithBrowseButton
import com.intellij.ui.dsl.builder.bindText
import com.intellij.ui.dsl.builder.columns
import com.intellij.ui.CollectionListModel
import com.intellij.ui.ToolbarDecorator
import com.intellij.ui.components.JBList
import java.awt.BorderLayout
import javax.swing.JPanel

class HotFolderConfigurable(private val project: Project) : BoundConfigurable("Hot Folders") {
    private val service = HotFolderService.getInstance(project)
    private val listModel = CollectionListModel(service.getHotFolders().toMutableList())
    private val list = JBList(listModel)

    override fun createPanel(): DialogPanel {
        return panel {
            row {
                val decorator = ToolbarDecorator.createDecorator(list)
                    .setAddAction {
                        val descriptor = FileChooserDescriptorFactory.createSingleFolderDescriptor()
                        com.intellij.openapi.fileChooser.FileChooser.chooseFile(descriptor, project, null) { file ->
                            if (!listModel.items.contains(file.path)) {
                                listModel.add(file.path)
                            }
                        }
                    }
                    .setRemoveAction {
                        val selectedValue = list.selectedValue
                        if (selectedValue != null) {
                            listModel.remove(selectedValue)
                        }
                    }
                    .disableUpDownActions()
                
                val panel = JPanel(BorderLayout())
                panel.add(decorator.createPanel(), BorderLayout.CENTER)
                cell(panel).align(com.intellij.ui.dsl.builder.Align.FILL)
            }.resizableRow()
        }
    }

    override fun isModified(): Boolean {
        return listModel.items != service.getHotFolders()
    }

    override fun apply() {
        val currentState = service.getState()
        currentState.hotFolders = listModel.items.toMutableList()
        ProjectView.getInstance(project).refresh()
    }

    override fun reset() {
        listModel.removeAll()
        listModel.add(service.getHotFolders())
    }
}

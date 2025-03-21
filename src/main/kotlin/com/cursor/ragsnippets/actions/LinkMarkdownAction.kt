package com.cursor.ragsnippets.actions

import com.cursor.ragsnippets.service.RagSnippetService
import com.intellij.openapi.actionSystem.AnAction
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.fileChooser.FileChooserFactory
import com.intellij.openapi.fileChooser.FileSaverDescriptor
import com.intellij.openapi.project.Project
import com.intellij.openapi.project.ProjectManager
import com.intellij.openapi.ui.Messages
import com.intellij.openapi.ui.popup.JBPopupFactory
import com.intellij.openapi.vfs.LocalFileSystem
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.ui.components.JBList
import java.nio.file.Paths
import javax.swing.DefaultListModel

class LinkMarkdownAction : AnAction() {
    override fun actionPerformed(e: AnActionEvent) {
        val currentProject = e.project
        
        if (currentProject == null) {
            Messages.showErrorDialog("No project is currently open", "Error")
            return
        }
        
        // If only one project is open, use it directly
        val projects = ProjectManager.getInstance().openProjects
        
        if (projects.isEmpty()) {
            Messages.showErrorDialog("No projects are currently open", "Error")
            return
        }
        
        // If multiple projects are open, ask which one to use
        if (projects.size > 1) {
            val projectPaths = projects.map { it.basePath.toString() }
            
            // Create a proper list model
            val listModel = DefaultListModel<String>()
            projectPaths.forEach { listModel.addElement(it) }
            val list = JBList(listModel)
            
            // Create a popup with the list
            val popup = JBPopupFactory.getInstance()
                .createListPopupBuilder(list)
                .setTitle("Select Project")
                .setItemChosenCallback(Runnable {
                    val selectedIndex = list.selectedIndex
                    if (selectedIndex >= 0) {
                        linkMarkdownToSelectedProject(projects[selectedIndex])
                    }
                })
                .createPopup()
            
            popup.showCenteredInCurrentWindow(currentProject)
        } else {
            linkMarkdownToSelectedProject(projects[0])
        }
    }
    
    private fun linkMarkdownToSelectedProject(project: Project) {
        val basePath = project.basePath ?: return
        val baseDir = LocalFileSystem.getInstance().findFileByPath(basePath) ?: return
        
        // Ask for the file name
        val defaultName = "rag-content.md"
        val fileName = Messages.showInputDialog(
            project,
            "Enter a name for the markdown file in your project",
            "Link RAG Markdown",
            null,
            defaultName,
            null
        ) ?: return
        
        // Create the file descriptor
        val descriptor = FileSaverDescriptor(
            "Link RAG Markdown",
            "Select where to save the markdown file",
            "md"
        )
        
        // Show the file chooser dialog
        val fileChooser = FileChooserFactory.getInstance().createSaveFileDialog(descriptor, project)
        val virtualFilePath = fileChooser.save(baseDir, fileName)?.file ?: return
        
        // Link the markdown file
        RagSnippetService.getInstance().linkMarkdownToProject(
            project,
            Paths.get(virtualFilePath.path)
        )
        
        // Open the file
        val linkedFile = LocalFileSystem.getInstance().refreshAndFindFileByPath(virtualFilePath.path)
        if (linkedFile != null) {
            linkedFile.refresh(false, false)
            openFile(project, linkedFile)
        }
    }
    
    private fun openFile(project: Project, file: VirtualFile) {
        com.intellij.openapi.fileEditor.FileEditorManager.getInstance(project).openFile(file, true)
    }
} 
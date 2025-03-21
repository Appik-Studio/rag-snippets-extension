package com.cursor.ragsnippets.actions

import com.cursor.ragsnippets.service.RagSnippetService
import com.intellij.openapi.actionSystem.AnAction
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.ui.Messages
import com.intellij.openapi.ui.popup.JBPopupFactory
import com.intellij.ui.components.JBList
import javax.swing.DefaultListModel

class ManageSnippetsAction : AnAction() {
    override fun actionPerformed(e: AnActionEvent) {
        val project = e.project ?: return
        val service = RagSnippetService.getInstance()
        
        val snippets = service.getAllSnippets()
        
        if (snippets.isEmpty()) {
            Messages.showInfoMessage(project, "No snippets found in the RAG folder", "RAG Snippets")
            return
        }
        
        // Create a proper list model
        val listModel = DefaultListModel<String>()
        snippets.forEach { listModel.addElement(it) }
        val list = JBList(listModel)
        
        // Create a simple popup with the list
        val popup = JBPopupFactory.getInstance()
            .createListPopupBuilder(list)
            .setTitle("RAG Snippets")
            .setItemChosenCallback(Runnable {
                val selectedValue = list.selectedValue
                if (selectedValue != null) {
                    // Ask for confirmation
                    val result = Messages.showYesNoDialog(
                        project,
                        "Do you want to remove '${selectedValue}' from RAG snippets?",
                        "Confirm Removal",
                        "Yes",
                        "No",
                        null
                    )
                    
                    if (result == Messages.YES) {
                        service.removeFromRag(project, selectedValue.toString())
                    }
                }
            })
            .createPopup()
        
        popup.showCenteredInCurrentWindow(project)
    }
} 
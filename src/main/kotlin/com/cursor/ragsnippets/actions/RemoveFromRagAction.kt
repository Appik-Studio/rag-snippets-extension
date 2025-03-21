package com.cursor.ragsnippets.actions

import com.cursor.ragsnippets.service.RagSnippetService
import com.intellij.ide.projectView.impl.nodes.PsiFileNode
import com.intellij.openapi.actionSystem.ActionUpdateThread
import com.intellij.openapi.actionSystem.AnAction
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.actionSystem.CommonDataKeys
import com.intellij.openapi.actionSystem.LangDataKeys
import com.intellij.openapi.actionSystem.PlatformCoreDataKeys
import com.intellij.openapi.actionSystem.PlatformDataKeys
import com.intellij.openapi.project.Project
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiFile

class RemoveFromRagAction : AnAction() {
    override fun actionPerformed(e: AnActionEvent) {
        val project = e.project ?: return
        
        // Try different ways to get the selected file
        var virtualFile = tryGetVirtualFile(e, project)
        
        // If we still couldn't find a virtual file, return
        if (virtualFile == null) return
        
        // Skip directories
        if (virtualFile.isDirectory) return
        
        // Process the file
        val service = RagSnippetService.getInstance()
        service.removeFromRag(project, virtualFile)
    }
    
    private fun tryGetVirtualFile(e: AnActionEvent, project: Project): VirtualFile? {
        // Method 1: VIRTUAL_FILE directly
        var virtualFile = e.getData(CommonDataKeys.VIRTUAL_FILE)
        if (virtualFile != null) {
            println("Found file via CommonDataKeys.VIRTUAL_FILE: ${virtualFile.name}")
            return virtualFile
        }
        
        // Method 2: SELECTED_ITEMS
        val selectedItems = e.getData(PlatformCoreDataKeys.SELECTED_ITEMS)
        if (selectedItems != null) {
            println("Selected items types: ${selectedItems.map { it?.javaClass?.name }}")
            for (item in selectedItems) {
                println("  Item type: ${item?.javaClass?.name}")
                
                // Handle PsiFileNode from project view
                if (item is PsiFileNode) {
                    println("  Found PsiFileNode")
                    try {
                        // Try to get the value directly from the PsiFileNode
                        val psiFile = item.getValue() as? PsiFile
                        if (psiFile != null) {
                            val vFile = psiFile.virtualFile
                            if (vFile != null) {
                                println("  Found VirtualFile from PsiFileNode.getValue(): ${vFile.name}")
                                return vFile
                            }
                        }
                        
                        // Alternatively, try to access the virtual file through reflection
                        val field = PsiFileNode::class.java.getDeclaredField("myVirtualFile")
                        field.isAccessible = true
                        val vf = field.get(item) as? VirtualFile
                        if (vf != null) {
                            println("  Found VirtualFile via reflection: ${vf.name}")
                            return vf
                        }
                    } catch (ex: Exception) {
                        println("  Error extracting file from PsiFileNode: ${ex.message}")
                        ex.printStackTrace()
                    }
                }
                
                // Try to extract VirtualFile directly
                if (item is VirtualFile) {
                    println("  Found VirtualFile directly: ${item.name}")
                    return item
                }
                
                // Try to extract from PSI element
                if (item is PsiElement) {
                    val vFile = item.containingFile?.virtualFile
                    if (vFile != null) {
                        println("  Found VirtualFile from PsiElement: ${vFile.name}")
                        return vFile
                    }
                }
            }
        }
        
        // Method 3: Try PSI_ELEMENT
        val psiElement = e.getData(LangDataKeys.PSI_ELEMENT)
        if (psiElement != null) {
            println("Found PSI_ELEMENT: ${psiElement.javaClass.name}")
            val vFile = psiElement.containingFile?.virtualFile
            if (vFile != null) {
                println("  Found VirtualFile from PSI_ELEMENT: ${vFile.name}")
                return vFile
            }
        }
        
        // Method 4: Try NAVIGATABLE
        val navigatable = e.getData(CommonDataKeys.NAVIGATABLE)
        if (navigatable != null) {
            println("Found NAVIGATABLE: ${navigatable.javaClass.name}")
            if (navigatable is PsiElement) {
                val vFile = (navigatable as PsiElement).containingFile?.virtualFile
                if (vFile != null) {
                    println("  Found VirtualFile from NAVIGATABLE: ${vFile.name}")
                    return vFile
                }
            }
        }
        
        // Method 5: Try PSI_FILE
        val psiFile = e.getData(CommonDataKeys.PSI_FILE)
        if (psiFile != null) {
            println("Found PSI_FILE: ${psiFile.name}")
            val vFile = psiFile.virtualFile
            if (vFile != null) {
                println("  Found VirtualFile from PSI_FILE: ${vFile.name}")
                return vFile
            }
        }
        
        return null
    }
    
    override fun getActionUpdateThread(): ActionUpdateThread {
        return ActionUpdateThread.EDT
    }
    
    override fun update(e: AnActionEvent) {
        // Initially make invisible
        e.presentation.isVisible = false
        e.presentation.isEnabled = false
        
        if (e.project == null) return
        
        // Try different ways to get the selected file
        val virtualFile = tryGetVirtualFile(e, e.project!!)
        
        // Only show and enable if we have a non-directory file that is already in the RAG folder
        if (virtualFile != null && !virtualFile.isDirectory) {
            val service = RagSnippetService.getInstance()
            val fileExistsInRag = service.fileExistsInRag(virtualFile.name)
            
            // Only show if the file exists in the RAG folder
            e.presentation.isVisible = fileExistsInRag
            e.presentation.isEnabled = fileExistsInRag
        }
        
        println("RemoveFromRagAction.update - Place: ${e.place}, File: ${virtualFile?.name}, Visible: ${e.presentation.isVisible}")
    }
} 
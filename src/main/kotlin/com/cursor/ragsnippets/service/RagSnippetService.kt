package com.cursor.ragsnippets.service

import com.intellij.notification.NotificationGroupManager
import com.intellij.notification.NotificationType
import com.intellij.openapi.components.Service
import com.intellij.openapi.components.service
import com.intellij.openapi.project.Project
import com.intellij.openapi.vfs.LocalFileSystem
import com.intellij.openapi.vfs.VirtualFile
import java.io.File
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.Paths
import kotlin.io.path.exists
import kotlin.io.path.isDirectory
import kotlin.io.path.isSymbolicLink

@Service
class RagSnippetService {
    companion object {
        private val RAG_FOLDER = Paths.get(System.getProperty("user.home"), "rag-snippet")
        private val MARKDOWN_OUTPUT = RAG_FOLDER.resolve("rag-content.md")
        
        fun getInstance(): RagSnippetService = service()
    }
    
    init {
        ensureRagFolder()
    }
    
    // Ensure the RAG folder exists
    private fun ensureRagFolder() {
        if (!RAG_FOLDER.exists()) {
            Files.createDirectories(RAG_FOLDER)
        }
    }
    
    // Generate markdown file from all snippets
    fun generateMarkdown() {
        ensureRagFolder()
        
        val sb = StringBuilder("# RAG Snippets\n\n")
        
        // Get all files in the RAG folder that aren't the markdown file
        Files.list(RAG_FOLDER).use { stream ->
            stream
                .filter { path -> !path.fileName.toString().equals(MARKDOWN_OUTPUT.fileName.toString()) }
                .filter { path -> !path.fileName.toString().equals(".DS_Store") }
                .sorted()
                .forEach { path ->
                    // Skip if it's a directory
                    if (path.isDirectory()) return@forEach
                    
                    // Try to get the real path if it's a symlink
                    var realPath = path
                    var isSymlink = false
                    
                    if (path.isSymbolicLink()) {
                        try {
                            realPath = Files.readSymbolicLink(path)
                            isSymlink = true
                            
                            // Skip if symlink target doesn't exist
                            if (!realPath.exists()) return@forEach
                        } catch (e: Exception) {
                            return@forEach
                        }
                    }
                    
                    // Get file content
                    val content = try {
                        Files.readString(realPath)
                    } catch (e: Exception) {
                        return@forEach
                    }
                    
                    // Add file info to markdown
                    sb.append("## ${path.fileName}\n\n")
                    
                    if (isSymlink) {
                        sb.append("- **Source**: $realPath\n\n")
                    }
                    
                    // Add file content with language detection from file extension
                    val language = path.fileName.toString().substringAfterLast('.', "text")
                    sb.append("```$language\n$content\n```\n\n")
                }
        }
        
        Files.writeString(MARKDOWN_OUTPUT, sb.toString())
    }
    
    // Check if a file exists in the RAG folder
    fun fileExistsInRag(fileName: String): Boolean {
        try {
            ensureRagFolder()
            println("Checking if file exists in RAG: $fileName")
            
            // Clean the file name to remove any path elements that might be present
            val cleanFileName = java.io.File(fileName).name
            println("Cleaned file name: $cleanFileName")
            
            val symlinkPath = RAG_FOLDER.resolve(cleanFileName)
            val exists = symlinkPath.exists()
            println("Path $symlinkPath exists: $exists")
            
            return exists
        } catch (e: Exception) {
            // Log the exception but don't throw it - return false as a safe default
            println("Error checking if file exists in RAG: ${e.message}")
            e.printStackTrace()
            return false
        }
    }
    
    // Check if a VirtualFile exists in the RAG folder by path
    fun pathExistsInRag(path: String): Boolean {
        try {
            ensureRagFolder()
            println("Checking if path exists in RAG: $path")
            
            // Get the base name from the path
            val baseName = java.nio.file.Paths.get(path).fileName.toString()
            println("Base name from path: $baseName")
            
            val symlinkPath = RAG_FOLDER.resolve(baseName)
            val exists = symlinkPath.exists()
            println("Path $symlinkPath exists: $exists")
            
            return exists
        } catch (e: Exception) {
            println("Error checking if path exists in RAG: ${e.message}")
            e.printStackTrace()
            return false
        }
    }
    
    // Add a file to the RAG folder
    fun addToRag(project: Project, virtualFile: VirtualFile): Boolean {
        try {
            ensureRagFolder()
            val filePath = Paths.get(virtualFile.path)
            
            if (!filePath.exists()) {
                return false // Silently fail if file doesn't exist
            }
            
            val fileName = filePath.fileName.toString()
            val symlinkPath = RAG_FOLDER.resolve(fileName)
            
            // Remove if exists
            if (symlinkPath.exists()) {
                Files.delete(symlinkPath)
            }
            
            // Create symbolic link
            Files.createSymbolicLink(symlinkPath, filePath)
            generateMarkdown() // Regenerate markdown
            
            // Show notification
            showNotification(project, "Added ${fileName} to RAG snippets", NotificationType.INFORMATION)
            return true
        } catch (e: Exception) {
            return false // Silently fail
        }
    }
    
    // Remove a file from the RAG folder
    fun removeFromRag(project: Project, virtualFile: VirtualFile): Boolean {
        try {
            ensureRagFolder()
            val fileName = virtualFile.name
            val symlinkPath = RAG_FOLDER.resolve(fileName)
            
            if (!symlinkPath.exists()) {
                return false // File doesn't exist in RAG folder
            }
            
            Files.delete(symlinkPath)
            generateMarkdown() // Regenerate markdown
            
            // Show notification
            showNotification(project, "Removed ${fileName} from RAG snippets", NotificationType.INFORMATION)
            return true
        } catch (e: Exception) {
            return false // Silently fail
        }
    }
    
    // For backward compatibility
    @Deprecated("Use the version with VirtualFile parameter instead")
    fun removeFromRag(project: Project, fileName: String): Boolean {
        try {
            ensureRagFolder()
            val symlinkPath = RAG_FOLDER.resolve(fileName)
            
            if (!symlinkPath.exists()) {
                return false // File doesn't exist in RAG folder
            }
            
            Files.delete(symlinkPath)
            generateMarkdown() // Regenerate markdown
            
            // Show notification
            showNotification(project, "Removed ${fileName} from RAG snippets", NotificationType.INFORMATION)
            return true
        } catch (e: Exception) {
            return false // Silently fail
        }
    }
    
    // Get all RAG snippets
    fun getAllSnippets(): List<String> {
        ensureRagFolder()
        
        val files = mutableListOf<String>()
        
        Files.list(RAG_FOLDER).use { stream ->
            stream
                .filter { path -> !path.fileName.toString().equals(MARKDOWN_OUTPUT.fileName.toString()) }
                .filter { path -> !path.fileName.toString().equals(".DS_Store") }
                .sorted()
                .forEach { path ->
                    // Skip if it's a directory
                    if (!path.isDirectory()) {
                        files.add(path.fileName.toString())
                    }
                }
        }
        
        return files
    }
    
    // Link markdown to project
    fun linkMarkdownToProject(project: Project, targetPath: Path): Boolean {
        try {
            ensureRagFolder()
            
            // Make sure the markdown file exists
            if (!MARKDOWN_OUTPUT.exists()) {
                generateMarkdown() // Create it if it doesn't exist
            }
            
            // Check if file already exists
            if (targetPath.exists()) {
                Files.delete(targetPath)
            }
            
            // Create symbolic link
            Files.createSymbolicLink(targetPath, MARKDOWN_OUTPUT)
            
            // Refresh the virtual file system
            LocalFileSystem.getInstance().refreshAndFindFileByPath(targetPath.toString())
            
            // Show notification
            showNotification(
                project,
                "Linked RAG markdown file to ${targetPath.fileName} in your project",
                NotificationType.INFORMATION
            )
            
            return true
        } catch (e: Exception) {
            showNotification(
                project,
                "Error linking markdown file: ${e.message}",
                NotificationType.ERROR
            )
            return false
        }
    }
    
    // Show a notification
    private fun showNotification(project: Project, content: String, type: NotificationType) {
        NotificationGroupManager.getInstance()
            .getNotificationGroup("RAG Snippets Notifications")
            .createNotification(content, type)
            .notify(project)
    }
} 
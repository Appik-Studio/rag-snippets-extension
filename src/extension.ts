import * as fs from 'fs'
import * as os from 'os'
import * as path from 'path'
import * as vscode from 'vscode'

const RAG_FOLDER = path.join(os.homedir(), 'rag-snippet')
const MARKDOWN_OUTPUT = path.join(RAG_FOLDER, 'rag-content.md')

const ensureRagFolder = (): void => {
  if (!fs.existsSync(RAG_FOLDER)) {
    fs.mkdirSync(RAG_FOLDER, { recursive: true })
  }
}

const generateMarkdown = (): void => {
  let markdown = '# RAG Snippets\n\n'
  
  // Get all files in the RAG folder that aren't the markdown file
  const files = fs.readdirSync(RAG_FOLDER)
    .filter(file => file !== path.basename(MARKDOWN_OUTPUT) && file !== '.DS_Store')
    .sort()
  
  for (const file of files) {
    const filePath = path.join(RAG_FOLDER, file)
    
    // Skip if it's a directory or symlink points to non-existent file
    if (fs.lstatSync(filePath).isDirectory()) {
      continue
    }
    
    // Try to get the real path if it's a symlink
    let realPath = filePath
    let isSymlink = false
    
    try {
      if (fs.lstatSync(filePath).isSymbolicLink()) {
        realPath = fs.readlinkSync(filePath)
        isSymlink = true
        
        // Skip if symlink target doesn't exist
        if (!fs.existsSync(realPath)) {
          continue
        }
      }
    } catch (error) {
      continue
    }
    
    // Get file content
    let content = ''
    try {
      content = fs.readFileSync(realPath, 'utf-8')
    } catch (error) {
      continue
    }
    
    // Add file info to markdown
    markdown += `## ${file}\n\n`
    
    if (isSymlink) {
      markdown += `- **Source**: ${realPath}\n\n`
    }
    
    // Add file content with language detection from file extension
    const language = path.extname(file).slice(1) || 'text'
    markdown += `\`\`\`${language}\n${content}\n\`\`\`\n\n`
  }
  
  fs.writeFileSync(MARKDOWN_OUTPUT, markdown, 'utf-8')
}

// Check if a file exists in the RAG folder
const fileExistsInRag = (fileName: string): boolean => {
  ensureRagFolder()
  const symlinkPath = path.join(RAG_FOLDER, fileName)
  return fs.existsSync(symlinkPath)
}

// Add a file to the RAG folder
const addToRag = async (uri: vscode.Uri): Promise<boolean> => {
  try {
    ensureRagFolder()
    const filePath = uri.fsPath
    
    if (!fs.existsSync(filePath)) {
      return false // Silently fail if file doesn't exist
    }
    
    const fileName = path.basename(filePath)
    const symlinkPath = path.join(RAG_FOLDER, fileName)
    
    // Check if file already exists in RAG folder
    if (fs.existsSync(symlinkPath)) {
      const overwrite = await vscode.window.showQuickPick(['Yes', 'No'], {
        placeHolder: `${fileName} already exists in RAG folder. Overwrite?`
      })
      
      if (overwrite !== 'Yes') return false
      
      try {
        // Remove existing file/symlink
        fs.unlinkSync(symlinkPath)
      } catch (error) {
        return false // Silently fail if we can't remove
      }
    }
    
    // Create symbolic link
    try {
      fs.symlinkSync(filePath, symlinkPath)
      generateMarkdown() // Regenerate markdown file
      vscode.window.showInformationMessage(`Added ${fileName} to RAG snippets`)
      return true
    } catch (error) {
      // Silently fail if symlink creation fails
      return false
    }
  } catch (error) {
    // Silently fail on any other errors
    return false
  }
}

// Remove a file from the RAG folder
const removeFromRag = async (uri: vscode.Uri): Promise<boolean> => {
  try {
    ensureRagFolder()
    const fileName = path.basename(uri.fsPath)
    const symlinkPath = path.join(RAG_FOLDER, fileName)
    
    if (!fs.existsSync(symlinkPath)) {
      return false // File doesn't exist in RAG folder
    }
    
    // Remove file/symlink
    try {
      fs.unlinkSync(symlinkPath)
      generateMarkdown() // Regenerate markdown file
      vscode.window.showInformationMessage(`Removed ${fileName} from RAG snippets`)
      return true
    } catch (error) {
      // Silently fail if removal fails
      return false
    }
  } catch (error) {
    // Silently fail on any other errors
    return false
  }
}

// Update the title of the toggle command based on whether the file exists in RAG folder
const updateToggleCommandTitle = (uri?: vscode.Uri): void => {
  if (!uri) return
  
  const fileName = path.basename(uri.fsPath)
  const exists = fileExistsInRag(fileName)
  
  // Update the command title
  vscode.commands.executeCommand(
    'setContext', 
    'ragSnippetExists', 
    exists
  )
}

// Toggle a file in the RAG folder (add if not present, remove if present)
const toggleRagSnippet = async (uri: vscode.Uri) => {
  if (!uri) return
  
  const fileName = path.basename(uri.fsPath)
  
  // First update the command title
  updateToggleCommandTitle(uri)
  
  if (fileExistsInRag(fileName)) {
    // If file exists in RAG folder, remove it
    await removeFromRag(uri)
  } else {
    // If file doesn't exist in RAG folder, add it
    await addToRag(uri)
  }
  
  // Update the command title again after toggle
  updateToggleCommandTitle(uri)
  
  // Force VS Code to refresh the context menu
  // This ensures the menu item updates from "Add" to "Remove" or vice versa
  vscode.commands.executeCommand('setContext', 'ragSnippetExists', !fileExistsInRag(fileName))
  setTimeout(() => {
    vscode.commands.executeCommand('setContext', 'ragSnippetExists', fileExistsInRag(fileName))
  }, 100)
}

// List all RAG snippets and allow user to select one to remove
const listRagSnippets = async () => {
  try {
    ensureRagFolder()
    
    // Get all files in the RAG folder that aren't the markdown file or .DS_Store
    const files = fs.readdirSync(RAG_FOLDER)
      .filter(file => file !== path.basename(MARKDOWN_OUTPUT) && file !== '.DS_Store')
    
    if (files.length === 0) {
      vscode.window.showInformationMessage('No snippets found in the RAG folder')
      return
    }
    
    // Show a quickpick to select which file to remove
    const quickPickItems = files.map(file => ({
      label: file
    }))
    
    const selectedItem = await vscode.window.showQuickPick(quickPickItems, {
      placeHolder: 'Select a snippet to remove'
    })
    
    if (!selectedItem) return // User cancelled
    
    const fileToRemove = selectedItem.label
    
    // Remove file/symlink
    try {
      const symlinkPath = path.join(RAG_FOLDER, fileToRemove)
      fs.unlinkSync(symlinkPath)
      generateMarkdown() // Regenerate markdown file
      vscode.window.showInformationMessage(`Removed ${fileToRemove} from RAG snippets`)
    } catch (error) {
      // Silently fail if removal fails
      return
    }
  } catch (error) {
    // Silently fail on any other errors
    return
  }
}

const setupFileWatcher = (context: vscode.ExtensionContext): void => {
  const watcher = fs.watch(RAG_FOLDER, (eventType, filename) => {
    if (!filename) return
    
    // Skip markdown file
    if (filename === path.basename(MARKDOWN_OUTPUT)) {
      return
    }
    
    // Regenerate markdown when files are added or removed
    generateMarkdown()
  })
  
  // Make sure to dispose the watcher when the extension is deactivated
  context.subscriptions.push({ dispose: () => watcher.close() })
}

// Listen for selection changes to update the command title
const setupSelectionListener = (context: vscode.ExtensionContext): void => {
  const selectionListener = vscode.window.onDidChangeWindowState(() => {
    const activeEditor = vscode.window.activeTextEditor
    if (activeEditor) {
      updateToggleCommandTitle(activeEditor.document.uri)
    }
  })
  
  context.subscriptions.push(selectionListener)
}

// Create a symbolic link to the markdown file in the current project
const linkMarkdownToProject = async () => {
  try {
    ensureRagFolder()
    
    // Make sure the markdown file exists
    if (!fs.existsSync(MARKDOWN_OUTPUT)) {
      generateMarkdown() // Create it if it doesn't exist
    }
    
    // Get workspace folders
    const workspaceFolders = vscode.workspace.workspaceFolders
    
    if (!workspaceFolders || workspaceFolders.length === 0) {
      vscode.window.showErrorMessage('No workspace folder found. Please open a folder first.')
      return
    }
    
    let targetFolder: vscode.WorkspaceFolder
    
    // If there are multiple workspace folders, ask the user which one to use
    if (workspaceFolders.length > 1) {
      const folderItems = workspaceFolders.map(folder => ({
        label: folder.name,
        description: folder.uri.fsPath,
        folder: folder
      }))
      
      const selectedFolder = await vscode.window.showQuickPick(folderItems, {
        placeHolder: 'Select a workspace folder to link the markdown file to'
      })
      
      if (!selectedFolder) return // User cancelled
      
      targetFolder = selectedFolder.folder
    } else {
      // Just use the only workspace folder
      targetFolder = workspaceFolders[0]
    }
    
    // Ask for the name of the symlink file
    const defaultName = 'rag-content.md'
    const fileName = await vscode.window.showInputBox({
      prompt: 'Enter a name for the markdown file in your project',
      placeHolder: 'E.g., rag-content.md',
      value: defaultName
    })
    
    if (!fileName) return // User cancelled
    
    // Create the target path
    const targetPath = path.join(targetFolder.uri.fsPath, fileName)
    
    // Check if file already exists
    if (fs.existsSync(targetPath)) {
      const overwrite = await vscode.window.showQuickPick(['Yes', 'No'], {
        placeHolder: `${fileName} already exists in the project. Overwrite?`
      })
      
      if (overwrite !== 'Yes') return
      
      try {
        // Remove existing file/symlink
        fs.unlinkSync(targetPath)
      } catch (error) {
        vscode.window.showErrorMessage(`Could not remove existing file: ${error}`)
        return
      }
    }
    
    // Create symbolic link
    try {
      fs.symlinkSync(MARKDOWN_OUTPUT, targetPath)
      vscode.window.showInformationMessage(`Linked RAG markdown file to ${fileName} in your project`)
      
      // Open the file in the editor
      const document = await vscode.workspace.openTextDocument(targetPath)
      await vscode.window.showTextDocument(document)
    } catch (error) {
      vscode.window.showErrorMessage(`Error creating symlink: ${error}`)
    }
  } catch (error) {
    vscode.window.showErrorMessage(`Error linking markdown file: ${error}`)
  }
}

export function activate(context: vscode.ExtensionContext) {
  console.log('RAG Snippets extension is now active')
  
  // Ensure the RAG folder exists
  ensureRagFolder()
  
  // Register dynamic command title provider
  context.subscriptions.push(
    vscode.commands.registerTextEditorCommand('extension.updateToggleTitle', (editor) => {
      updateToggleCommandTitle(editor.document.uri)
    })
  )
  
  // Register the "Toggle RAG Snippet" command
  const toggleCommand = vscode.commands.registerCommand('cursor-rules.toggleRagSnippet', (uri: vscode.Uri) => {
    if (!uri) return
    
    // Execute the toggle action
    toggleRagSnippet(uri)
  })
  
  // Register the "List RAG Snippets" command
  const listCommand = vscode.commands.registerCommand('cursor-rules.listRagSnippets', () => {
    listRagSnippets()
  })
  
  // Register the "Link Markdown to Project" command
  const linkCommand = vscode.commands.registerCommand('cursor-rules.linkMarkdownToProject', () => {
    linkMarkdownToProject()
  })
  
  // Setup file watcher
  setupFileWatcher(context)
  
  // Setup selection listener
  setupSelectionListener(context)
  
  // Generate markdown on startup
  generateMarkdown()
  
  // Setup title updater for when a file is selected in the explorer
  const fileExplorerListener = vscode.window.onDidChangeActiveTextEditor((editor) => {
    if (editor) {
      updateToggleCommandTitle(editor.document.uri)
    }
  })
  
  // Setup command for updating the command title when UI is interacted with
  const updateTitleCommand = vscode.commands.registerCommand('rag-snippets.updateTitle', (uri: vscode.Uri) => {
    updateToggleCommandTitle(uri)
  })
  
  context.subscriptions.push(toggleCommand, listCommand, linkCommand, fileExplorerListener, updateTitleCommand)
  
  // Register localized command titles
  context.subscriptions.push(
    vscode.commands.registerCommand('rag-snippets.getAddTitle', () => 'Add to RAG Snippets'),
    vscode.commands.registerCommand('rag-snippets.getRemoveTitle', () => 'Remove from RAG Snippets')
  )
}

export function deactivate() {} 
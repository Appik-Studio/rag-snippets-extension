# RAG Snippets Extension

A VS Code extension that creates symbolic links to code files in a dedicated folder (`~/rag-snippet`) and automatically generates a markdown document containing the content of these files for use with Retrieval-Augmented Generation (RAG) systems.

## Features

- Right-click on a code file to toggle it in your RAG snippets collection
  - If the file isn't in your collection, you'll see "Add to RAG Snippets"
  - If the file is already in your collection, you'll see "Remove from RAG Snippets"
- Automatically generates a markdown file (`rag-content.md`) containing all snippet content
- Link the generated markdown file to your current workspace with a single command
- File system watcher automatically updates the markdown when files are added or removed

## How to Use

1. Right-click on a code file in the VS Code explorer
2. Select "Add to RAG Snippets" to add the file, or "Remove from RAG Snippets" if it's already added
3. If a file with the same name already exists, you'll be asked if you want to overwrite it

To manage all your snippets:
1. Open the Command Palette (`Ctrl+Shift+P` or `Cmd+Shift+P`)
2. Run "Manage RAG Snippets" to see a list of all snippets and remove any if needed

To link the generated markdown file to your project:
1. Open the Command Palette (`Ctrl+Shift+P` or `Cmd+Shift+P`)
2. Run "Link RAG Markdown to Current Project"
3. If you have multiple workspace folders, select the target folder
4. Enter a name for the markdown file (default: `rag-content.md`)
5. The file will be linked and opened in the editor

## Implementation Details

This extension:

1. Creates a folder at `~/rag-snippet` to store:
   - Symbolic links to your code files
   - A markdown file (`rag-content.md`) with formatted content of all snippets

2. When you add a file:
   - Creates a symbolic link to the original file (keeping the original intact)
   - Regenerates the markdown content

3. When you remove a file:
   - Removes the symbolic link (original file remains untouched)
   - Regenerates the markdown content

4. When you link the markdown:
   - Creates a symbolic link from the generated markdown to your workspace
   - Automatically opens the linked file for viewing

5. A file system watcher monitors changes in the RAG folder and automatically regenerates the markdown 
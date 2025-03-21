# RAG Snippets Plugin for JetBrains IDEs

A JetBrains IDE plugin that creates symbolic links to code files in a dedicated folder (`~/rag-snippet`) and automatically generates a markdown document containing the content of these files for use with Retrieval-Augmented Generation (RAG) systems.

## Features

- Right-click on a code file to add it to your RAG snippets collection
- Right-click on a code file already in your collection to remove it
- Automatically generates a markdown file (`rag-content.md`) containing all snippet content
- Link the generated markdown file to your current project with a single command
- Automatic updates when files are added or removed

## How to Use

1. Right-click on a code file in the Project view
2. Select "Add to RAG Snippets" to add the file
3. If the file is already in your RAG collection, you'll see "Remove from RAG Snippets" instead

To manage all your snippets:
1. Go to Tools > Manage RAG Snippets
2. Select any snippet to remove it from your collection

To link the generated markdown file to your project:
1. Go to Tools > Link RAG Markdown to Current Project
2. If you have multiple projects open, select the target project
3. Enter a name for the markdown file (default: `rag-content.md`)
4. Choose a location in your project to save the linked file
5. The file will be linked and opened in the editor

## Implementation Details

This plugin:

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
   - Creates a symbolic link from the generated markdown to your project
   - Automatically opens the linked file for viewing

## Building the Plugin

1. Clone this repository
2. Build using Gradle:
   ```
   ./gradlew buildPlugin
   ```
3. The plugin will be available at: `build/distributions/rag-snippets-1.0.0.zip`

## Installation

1. In your JetBrains IDE, go to Settings/Preferences > Plugins
2. Click on the gear icon and select "Install Plugin from Disk..."
3. Navigate to the built plugin ZIP file and select it
4. Restart your IDE when prompted 
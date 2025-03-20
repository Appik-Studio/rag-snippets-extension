# Installing the RAG Snippets Extension

## Installation

There are two ways to install this extension:

### Method 1: Install from VSIX

1. Open VS Code
2. Press `Ctrl+Shift+P` (Windows/Linux) or `Cmd+Shift+P` (Mac) to open the command palette
3. Type "Install from VSIX" and select the command
4. Navigate to the location of the `rag-snippets-extension-1.0.0.vsix` file and select it
5. VS Code will install the extension and prompt you to restart

### Method 2: Install from VS Code Extensions Marketplace (if published)

1. Open VS Code
2. Click on the Extensions icon in the Activity Bar (or press `Ctrl+Shift+X`)
3. Search for "RAG Snippets Extension"
4. Click the "Install" button

## Usage

After installation, you can:

1. Right-click on any file in the Explorer view
2. The context menu will show:
   - "Add to RAG Snippets" if the file is not already in your collection
   - "Remove from RAG Snippets" if the file is already in your collection

The extension will:
- Create a symbolic link to your file in the `~/rag-snippet` directory
- Generate a markdown file containing all snippets' content

## Accessing the Generated Content

The extension creates and manages the following files:

- Symbolic links to your code files in `~/rag-snippet/`
- A generated markdown file at `~/rag-snippet/rag-content.md`

You can use the markdown file directly with your RAG system or copy its contents as needed.

You can also link the markdown file to your current project:
1. Open the Command Palette (`Ctrl+Shift+P` or `Cmd+Shift+P`)
2. Run "Link RAG Markdown to Current Project"
3. Follow the prompts to select a location and filename
4. The file will be linked and automatically opened in the editor

## Managing Snippets

To manage all your snippets at once:

1. Open the Command Palette (`Ctrl+Shift+P` or `Cmd+Shift+P`)
2. Run "Manage RAG Snippets" to see a list of all snippets and remove any if needed 
import re
import os
import subprocess
import tempfile
import collections
from flask import Flask, request, render_template_string

app = Flask(__name__)

# We keep everything in a single file by storing the UI template as a string.
# It uses Tailwind CSS for a sleek VS2022 dark-mode aesthetic and vanilla JS for interactivity.
HTML_TEMPLATE = """
<!DOCTYPE html>
<html lang="en" class="dark">
<head>
    <meta charset="UTF-8">
    <meta name="viewport" content="width=device-width, initial-scale=1.0">
    <title>Gradle Output Debugger</title>
    <script src="https://cdn.tailwindcss.com"></script>
    <script src="https://unpkg.com/lucide@latest"></script>
    <script>
        tailwind.config = {
            darkMode: 'class',
            theme: {
                extend: {
                    colors: {
                        vs: {
                            bg: '#1e1e1e',
                            sidebar: '#252526',
                            border: '#333333',
                            hover: '#2a2d2e',
                            active: '#37373d',
                            text: '#cccccc',
                            blue: '#007acc',
                            error: '#f48771',
                            warning: '#cca700'
                        }
                    }
                }
            }
        }
    </script>
    <style>
        /* Custom scrollbar for a native IDE feel */
        ::-webkit-scrollbar { width: 10px; height: 10px; }
        ::-webkit-scrollbar-track { background: #1e1e1e; }
        ::-webkit-scrollbar-thumb { background: #424242; border: 1px solid #1e1e1e; }
        ::-webkit-scrollbar-thumb:hover { background: #4f4f4f; }
        ::-webkit-scrollbar-corner { background: #1e1e1e; }
    </style>
</head>
<body class="bg-vs-bg text-vs-text h-screen flex flex-col font-sans overflow-hidden relative">

    <!-- Loading Overlay -->
    <div id="loading-overlay" class="fixed inset-0 bg-black/80 backdrop-blur-sm z-50 flex flex-col items-center justify-center hidden">
        <i data-lucide="loader-2" class="w-12 h-12 text-vs-blue animate-spin mb-4"></i>
        <h2 class="text-white text-xl font-semibold">Running Gradle...</h2>
        <p class="text-gray-400 mt-2 max-w-md text-center">Executing local build and capturing expanded errors. This might take a few minutes on large projects.</p>
    </div>

    <!-- Top Navigation Bar -->
    <header class="bg-vs-sidebar border-b border-vs-border flex items-center px-4 py-2 text-sm shadow-md z-10">
        <i data-lucide="terminal-square" class="w-5 h-5 text-vs-blue mr-2"></i>
        <span class="font-semibold text-gray-200">Gradle UI Debugger</span>
        
        {% if has_data %}
        <form method="GET" action="/" class="ml-auto">
            <button type="submit" class="bg-vs-active hover:bg-vs-hover text-vs-text px-3 py-1 rounded text-xs flex items-center transition-colors">
                <i data-lucide="rotate-ccw" class="w-3 h-3 mr-1"></i> Start Over
            </button>
        </form>
        {% endif %}
    </header>

    {% if error_msg %}
    <div class="bg-red-900/50 border-b border-red-500/50 text-red-200 px-4 py-4 text-sm flex flex-col items-center justify-center max-h-64 overflow-y-auto">
        <div class="flex items-start mb-2 w-full max-w-4xl">
            <i data-lucide="alert-circle" class="w-5 h-5 mr-3 flex-shrink-0 mt-0.5"></i>
            <span class="font-bold">Execution Error:</span>
        </div>
        <div class="font-mono whitespace-pre-wrap text-left break-all w-full max-w-4xl bg-black/30 p-4 rounded">{{ error_msg }}</div>
    </div>
    {% endif %}

    {% if not has_data %}
    <!-- Input Screen -->
    <main class="flex-1 flex flex-col items-center justify-center p-8 max-w-4xl mx-auto w-full overflow-y-auto">
        <div class="text-center mb-8">
            <h1 class="text-3xl font-light text-white mb-2">Analyze Gradle Compilation</h1>
            <p class="text-gray-400 text-sm">Run your project directly to bypass the 100-error limit, or paste an existing log.</p>
        </div>

        <!-- Run Local Project Section -->
        <form method="POST" action="/" class="w-full flex flex-col items-center bg-[#252526] p-6 rounded shadow-lg mb-8 border border-vs-border load-trigger">
            <h2 class="text-white text-lg font-medium mb-4 w-full text-left flex items-center">
                <i data-lucide="play-circle" class="w-5 h-5 mr-2 text-vs-blue"></i> Run Local Project
            </h2>
            <div class="flex w-full mb-3">
                <input type="text" name="project_path" value="." class="flex-1 bg-[#1e1e1e] border border-vs-border rounded-l px-4 py-3 text-sm text-gray-300 focus:outline-none focus:border-vs-blue" placeholder="Enter root path (Defaults to '.' current directory)">
                <button type="submit" class="bg-vs-blue hover:bg-[#005f9e] text-white px-8 py-3 rounded-r font-medium shadow-lg transition-colors flex items-center whitespace-nowrap">
                    <i data-lucide="terminal" class="w-4 h-4 mr-2"></i> Run Build
                </button>
            </div>
            <p class="text-xs text-gray-500 text-left w-full flex items-center">
                <i data-lucide="info" class="w-3 h-3 mr-1"></i> 
                Automatically runs `./gradlew build` and injects `-Xmaxerrs 10000` to get all errors.
            </p>
        </form>

        <div class="flex items-center w-full mb-8">
            <div class="flex-1 border-t border-vs-border"></div>
            <span class="px-4 text-gray-500 font-semibold text-xs tracking-widest uppercase">Or</span>
            <div class="flex-1 border-t border-vs-border"></div>
        </div>

        <!-- Paste Log Section -->
        <form method="POST" action="/" class="w-full flex flex-col items-end load-trigger">
            <textarea name="log_data" class="w-full h-48 bg-[#1e1e1e] border border-vs-border rounded shadow-inner p-4 text-sm font-mono text-gray-300 focus:outline-none focus:border-vs-blue resize-none mb-4" placeholder="Paste your raw terminal output here..."></textarea>
            <button type="submit" class="bg-vs-active hover:bg-vs-hover text-white px-6 py-2 rounded font-medium shadow transition-colors flex items-center border border-vs-border">
                <i data-lucide="file-text" class="w-4 h-4 mr-2"></i> Parse Pasted Log
            </button>
        </form>
    </main>
    {% else %}
    
    <!-- Debugger Workspace -->
    <main class="flex-1 flex overflow-hidden">
        
        <!-- Sidebar: File List -->
        <aside class="w-1/3 max-w-sm bg-vs-sidebar border-r border-vs-border flex flex-col overflow-hidden">
            <!-- Global "All Files" Button -->
            <button id="btn-all-files" onclick="selectAllFiles()" class="w-full px-4 py-3 text-xs font-semibold uppercase tracking-wider text-gray-400 border-b border-vs-border shadow-sm flex justify-between items-center hover:text-white hover:bg-vs-hover transition-colors cursor-pointer group">
                <span class="flex items-center">
                    <i data-lucide="files" class="w-4 h-4 mr-2 group-hover:text-vs-blue transition-colors"></i> Affected Files
                </span>
                <span id="total-badge" class="bg-vs-border text-gray-300 px-2 py-0.5 rounded-full group-hover:bg-vs-blue group-hover:text-white transition-colors">0</span>
            </button>
            <ul id="file-list" class="overflow-y-auto flex-1 py-1">
                <!-- Populated by JS -->
            </ul>
        </aside>

        <!-- Main Area: Error Details -->
        <section class="flex-1 flex flex-col overflow-hidden bg-vs-bg relative">
            
            <!-- Context Header -->
            <div id="file-header" class="px-6 py-4 border-b border-vs-border bg-[#1e1e1e] shadow-sm flex items-center justify-between hidden z-20">
                <div class="flex items-center overflow-hidden">
                    <div id="header-icon-container" class="mr-3 flex-shrink-0">
                        <i id="icon-single" data-lucide="file-code" class="w-5 h-5 text-gray-400"></i>
                        <i id="icon-all" data-lucide="files" class="w-5 h-5 text-gray-400 hidden"></i>
                    </div>
                    <div class="flex flex-col overflow-hidden">
                        <span id="active-filename" class="text-white font-medium text-sm truncate"></span>
                        <span id="active-filepath" class="text-gray-500 text-xs font-mono mt-0.5 truncate max-w-xl"></span>
                    </div>
                </div>
                <!-- Global Copy Button -->
                <button onclick="copyCurrentView()" class="flex-shrink-0 ml-4 flex items-center space-x-1 text-gray-400 hover:text-white transition-colors bg-[#2d2d2d] hover:bg-[#3d3d3d] px-3 py-1.5 rounded border border-[#3d3d3d] text-xs">
                    <i data-lucide="copy" class="w-3.5 h-3.5"></i>
                    <span id="global-copy-text">Copy Context</span>
                </button>
            </div>

            <!-- Placeholder for unselected state -->
            <div id="no-selection" class="flex-1 flex flex-col items-center justify-center text-gray-500">
                <i data-lucide="folder-search" class="w-16 h-16 mb-4 opacity-50"></i>
                <p>Select a file from the list to view its issues.</p>
            </div>

            <!-- Error List -->
            <div id="issue-container" class="flex-1 overflow-y-auto p-4 hidden">
                <!-- Populated by JS -->
            </div>

        </section>
    </main>
    
    <script>
        // Inject data from Python directly into JS
        const parsedData = {{ parsed_data | tojson | safe }};
        const fileListEl = document.getElementById('file-list');
        const issueContainerEl = document.getElementById('issue-container');
        const noSelectionEl = document.getElementById('no-selection');
        const fileHeaderEl = document.getElementById('file-header');
        
        let activeFile = null;

        // Sorting files alphabetically
        const sortedFiles = Object.keys(parsedData).sort((a, b) => {
            const nameA = a.split(/[\\\\/]/).pop().toLowerCase();
            const nameB = b.split(/[\\\\/]/).pop().toLowerCase();
            return nameA.localeCompare(nameB);
        });

        // Setup Total Count
        let totalIssues = 0;

        // --- COPY LOGIC ---
        function formatIssueText(issue) {
            let txt = "Line " + issue.line + " | " + issue.type.toUpperCase() + ": " + issue.message + "\\n";
            if (issue.details) txt += issue.details + "\\n";
            return txt;
        }

        function copyText(text, btnTextElementId) {
            const textArea = document.createElement("textarea");
            textArea.value = text;
            document.body.appendChild(textArea);
            textArea.select();
            try {
                document.execCommand('copy');
                const copyTextEl = document.getElementById(btnTextElementId);
                if(copyTextEl) {
                    const originalText = copyTextEl.innerText;
                    copyTextEl.innerText = 'Copied!';
                    setTimeout(() => copyTextEl.innerText = originalText, 2000);
                }
            } catch (err) {
                console.error('Copy failed', err);
            }
            document.body.removeChild(textArea);
        }

        // Copies the content currently visible in the main panel
        function copyCurrentView() {
            let textToCopy = "";
            if (activeFile === 'ALL') {
                sortedFiles.forEach(file => {
                    textToCopy += "--- " + file + " ---\\n";
                    parsedData[file].forEach(issue => textToCopy += formatIssueText(issue));
                    textToCopy += "\\n";
                });
            } else if (activeFile) {
                textToCopy += "--- " + activeFile + " ---\\n";
                parsedData[activeFile].forEach(issue => textToCopy += formatIssueText(issue));
            }
            copyText(textToCopy, 'global-copy-text');
        }

        // Copies only a specific file's content (used in the All Files view headers)
        function copyFileText(file, btnId) {
            let textToCopy = "--- " + file + " ---\\n";
            parsedData[file].forEach(issue => textToCopy += formatIssueText(issue));
            copyText(textToCopy, btnId);
        }
        
        function renderSidebar() {
            if (sortedFiles.length === 0) {
                fileListEl.innerHTML = '<li class="px-4 py-6 text-center text-gray-500 text-sm italic">No compilation errors or warnings found!</li>';
                return;
            }

            sortedFiles.forEach(file => {
                const issues = parsedData[file];
                totalIssues += issues.length;
                
                const filename = file.split(/[\\\\/]/).pop();
                const errorCount = issues.filter(i => i.type === 'error').length;
                const warnCount = issues.filter(i => i.type === 'warning').length;
                
                const li = document.createElement('li');
                li.className = `cursor-pointer px-4 py-2 border-l-2 border-transparent hover:bg-vs-hover flex items-center justify-between group transition-colors`;
                li.onclick = () => selectFile(file, li);
                li.title = file;

                li.innerHTML = `
                    <div class="flex items-center truncate mr-2">
                        <i data-lucide="${errorCount > 0 ? 'file-x-2' : 'file-warning'}" class="w-4 h-4 mr-2 flex-shrink-0 ${errorCount > 0 ? 'text-vs-error' : 'text-vs-warning'}"></i>
                        <span class="text-sm truncate group-hover:text-white transition-colors">${filename}</span>
                    </div>
                    <div class="flex items-center space-x-1 text-xs">
                        ${errorCount > 0 ? `<span class="bg-red-900/40 text-vs-error px-1.5 rounded">${errorCount}</span>` : ''}
                        ${warnCount > 0 ? `<span class="bg-yellow-900/40 text-vs-warning px-1.5 rounded">${warnCount}</span>` : ''}
                    </div>
                `;
                fileListEl.appendChild(li);
            });
            document.getElementById('total-badge').innerText = totalIssues;
            lucide.createIcons();
        }

        function renderIssues(file, container, includeHeader) {
            // Include a mini-header with a copy button for the file if requested
            if (includeHeader) {
                const safeId = "btn-copy-" + Math.random().toString(36).substr(2, 9);
                // CRITICAL FIX: Use split and join instead of regex to prevent Python escape sequence bugs breaking JS
                const safeFileStr = file.split('\\\\').join('\\\\\\\\').split("'").join("\\\\'");
                
                const headerDiv = document.createElement('div');
                headerDiv.className = 'flex justify-between items-center bg-[#252526] border border-vs-border p-3 mt-6 mb-3 rounded shadow-sm sticky top-0 z-10';
                headerDiv.innerHTML = `
                    <div class="flex items-center truncate mr-4">
                        <i data-lucide="file-code" class="w-4 h-4 text-vs-blue mr-2 flex-shrink-0"></i>
                        <span class="text-gray-200 font-medium text-sm truncate" title="${file}">${file}</span>
                    </div>
                    <button onclick="copyFileText('${safeFileStr}', '${safeId}')" class="flex-shrink-0 flex items-center space-x-1 text-gray-400 hover:text-white transition-colors bg-[#2d2d2d] hover:bg-[#3d3d3d] px-2 py-1 rounded border border-[#3d3d3d] text-xs">
                        <i data-lucide="copy" class="w-3 h-3"></i>
                        <span id="${safeId}">Copy File</span>
                    </button>
                `;
                container.appendChild(headerDiv);
            }

            const issues = parsedData[file];
            issues.forEach(issue => {
                const isError = issue.type === 'error';
                const div = document.createElement('div');
                div.className = `mb-4 bg-[#252526] border ${isError ? 'border-red-900/30' : 'border-yellow-900/30'} rounded-lg overflow-hidden shadow-sm`;
                
                // Construct the code snippet area if details exist
                let detailsHtml = '';
                if (issue.details && issue.details.trim().length > 0) {
                    // Escape HTML so code renders properly
                    const escapedDetails = issue.details.replace(/&/g, "&amp;").replace(/</g, "&lt;").replace(/>/g, "&gt;");
                    detailsHtml = `
                        <div class="bg-[#1a1a1a] p-3 text-xs font-mono text-[#d4d4d4] overflow-x-auto border-t border-[#333]">
                            <pre class="m-0 leading-relaxed">${escapedDetails}</pre>
                        </div>
                    `;
                }

                div.innerHTML = `
                    <div class="p-3 flex items-start">
                        <div class="mr-3 mt-0.5">
                            <i data-lucide="${isError ? 'x-circle' : 'alert-triangle'}" class="w-4 h-4 ${isError ? 'text-vs-error' : 'text-vs-warning'}"></i>
                        </div>
                        <div class="flex-1 min-w-0">
                            <div class="flex items-center space-x-2 text-sm mb-1">
                                <span class="text-gray-400 font-mono">Line ${issue.line}</span>
                                <span class="text-gray-600">•</span>
                                <span class="${isError ? 'text-red-400' : 'text-yellow-400'} font-medium capitalize">${issue.type}</span>
                            </div>
                            <div class="text-sm font-semibold text-gray-200 whitespace-pre-wrap break-words">${issue.message.replace(/</g, "&lt;").replace(/>/g, "&gt;")}</div>
                        </div>
                    </div>
                    ${detailsHtml}
                `;
                container.appendChild(div);
            });
        }

        function clearSelection() {
            // Deselect list items
            document.querySelectorAll('#file-list li').forEach(el => {
                el.classList.remove('bg-vs-active', 'border-vs-blue');
                el.classList.add('border-transparent');
            });
            // Deselect master button
            const allBtn = document.getElementById('btn-all-files');
            if(allBtn) {
                allBtn.classList.remove('bg-vs-active', 'text-white');
                allBtn.classList.add('text-gray-400');
            }
        }

        function selectAllFiles() {
            activeFile = 'ALL';
            clearSelection();
            
            const allBtn = document.getElementById('btn-all-files');
            allBtn.classList.add('bg-vs-active', 'text-white');
            allBtn.classList.remove('text-gray-400');

            noSelectionEl.classList.add('hidden');
            fileHeaderEl.classList.remove('hidden');
            issueContainerEl.classList.remove('hidden');

            document.getElementById('active-filename').innerText = "All Affected Files";
            document.getElementById('active-filepath').innerText = `Displaying ${totalIssues} issues across ${sortedFiles.length} files`;
            
            // Toggle Icon
            document.getElementById('icon-single').classList.add('hidden');
            document.getElementById('icon-all').classList.remove('hidden');

            issueContainerEl.innerHTML = '';
            
            // Render each file sequentially with headers
            sortedFiles.forEach(file => {
                renderIssues(file, issueContainerEl, true);
            });

            lucide.createIcons();
        }

        function selectFile(file, element) {
            activeFile = file;
            clearSelection();
            
            element.classList.add('bg-vs-active', 'border-vs-blue');
            element.classList.remove('border-transparent');

            noSelectionEl.classList.add('hidden');
            fileHeaderEl.classList.remove('hidden');
            issueContainerEl.classList.remove('hidden');

            document.getElementById('active-filename').innerText = file.split(/[\\\\/]/).pop();
            document.getElementById('active-filepath').innerText = file;
            
            // Toggle Icon
            document.getElementById('icon-single').classList.remove('hidden');
            document.getElementById('icon-all').classList.add('hidden');

            issueContainerEl.innerHTML = '';
            renderIssues(file, issueContainerEl, false);
            
            lucide.createIcons();
        }

        if (Object.keys(parsedData).length > 0) {
            renderSidebar();
        }
    </script>
    {% endif %}
    
    <script>
        // Initialize Icons on load
        lucide.createIcons();

        // Show loading spinner on form submit
        document.querySelectorAll('.load-trigger').forEach(form => {
            form.addEventListener('submit', function() {
                const projectPathInput = this.querySelector('input[name="project_path"]');
                const logDataInput = this.querySelector('textarea[name="log_data"]');
                
                // Only show loading if inputs actually have content
                if ((projectPathInput && projectPathInput.value.trim()) || 
                    (logDataInput && logDataInput.value.trim())) {
                    document.getElementById('loading-overlay').classList.remove('hidden');
                }
            });
        });
    </script>
</body>
</html>
"""

def execute_gradle_project(project_path):
    """
    Executes the Gradle project locally, injecting an init script
    that forces javac to output unlimited errors and warnings.
    """
    # Use afterEvaluate to ensure project-level build.gradle doesn't override these args
    init_script_content = """
    allprojects {
        afterEvaluate {
            tasks.withType(JavaCompile) {
                options.compilerArgs += ["-Xmaxerrs", "10000", "-Xmaxwarns", "10000", "-Xdiags:verbose"]
            }
        }
    }
    """
    
    # Create temporary init script
    fd, temp_path = tempfile.mkstemp(suffix=".gradle")
    with os.fdopen(fd, 'w') as f:
        f.write(init_script_content)
        
    try:
        # Determine OS constraints
        is_win = os.name == 'nt'
        
        # We must resolve the absolute path to gradlew to avoid CWD/PATH mapping errors
        # on Windows subprocess calls.
        cmd_exec = os.path.abspath(os.path.join(project_path, "gradlew.bat" if is_win else "gradlew"))
        
        if not os.path.exists(cmd_exec):
            # Fallback to system gradle if no gradlew is available locally
            cmd_exec = "gradle"
            
        # --console=plain prevents interactive formatting that might confuse the parser or cause hangs
        cmd = [cmd_exec, "build", "--console=plain", "--init-script", os.path.abspath(temp_path)]
        print(f"Executing Gradle: {' '.join(cmd)}")
        
        try:
            # Added stdin=subprocess.DEVNULL to prevent Gradle from hanging if it asks for user input
            # Added timeout=600 (10 minutes) so it doesn't spin forever
            result = subprocess.run(
                cmd, 
                cwd=os.path.abspath(project_path), 
                capture_output=True, 
                text=True, 
                shell=is_win,
                stdin=subprocess.DEVNULL,
                timeout=600
            )
            
            output = (result.stdout or '') + "\n" + (result.stderr or '')
            return True, output
            
        except subprocess.TimeoutExpired as e:
            return False, f"Gradle process timed out after 10 minutes.\nOutput captured before timeout:\n{(e.stdout or '')}\n{(e.stderr or '')}"
        except FileNotFoundError:
            return False, f"Could not find or execute: {cmd_exec}. Please ensure the path is correct."
    except Exception as e:
        return False, f"Unexpected error executing Gradle: {str(e)}"
    finally:
        # Clean up temporary init file
        if os.path.exists(temp_path):
            os.remove(temp_path)

def parse_gradle_log(log_text):
    """
    Parses the Gradle output, extracts java compilation errors/warnings,
    and deduplicates them to prevent the indented list at the bottom 
    from causing duplicate entries.
    """
    parsed = collections.defaultdict(list)
    current_issue = None
    seen_keys = set()
    
    # Clean ANSI escape sequences (terminal colors) that gradle might output
    ansi_escape = re.compile(r'\x1B(?:[@-Z\\-_]|\[[0-?]*[ -/]*[@-~])')
    clean_log = ansi_escape.sub('', log_text)
    
    # Regex to capture file path, line number, severity, and message
    issue_pattern = re.compile(r'^\s*(.+?\.java):(\d+):\s+(error|warning):\s+(.*)')
    
    # Regex to detect when a code block for an error ends
    stop_pattern = re.compile(r'^\s*(\d+ errors|\d+ warning|Note:|FAILURE:|> Task|> Compilation failed|\* What went wrong:|\* Try:)')
    
    for line in clean_log.splitlines():
        clean_line = line.rstrip()
        if not clean_line:
            continue
            
        match = issue_pattern.match(clean_line)
        if match:
            # Save the previous issue before starting a new one
            if current_issue and current_issue['key'] not in seen_keys:
                seen_keys.add(current_issue['key'])
                parsed[current_issue['file']].append(current_issue)
            
            filepath, lineno, severity, msg = match.groups()
            filepath = filepath.strip()
            
            # Create a unique key to deduplicate identical errors printed twice
            issue_key = f"{filepath}:{lineno}:{severity}:{msg}"
            
            current_issue = {
                'key': issue_key,
                'file': filepath,
                'line': lineno,
                'type': severity,
                'message': msg,
                'details': "" # Will capture the code snippet and pointer (^)
            }
        elif stop_pattern.match(clean_line):
            # Log parsing is leaving the Javac output scope
            if current_issue and current_issue['key'] not in seen_keys:
                seen_keys.add(current_issue['key'])
                parsed[current_issue['file']].append(current_issue)
            current_issue = None
        else:
            # If we are currently tracking an issue, this line is part of the code snippet context
            if current_issue:
                current_issue['details'] += clean_line + "\n"
                
    # Ensure the very last issue is saved
    if current_issue and current_issue['key'] not in seen_keys:
        seen_keys.add(current_issue['key'])
        parsed[current_issue['file']].append(current_issue)
        
    return dict(parsed)

@app.route('/', methods=['GET', 'POST'])
def index():
    parsed_data = {}
    has_data = False
    error_msg = None
    
    if request.method == 'POST':
        project_path = request.form.get('project_path', '').strip()
        log_data = request.form.get('log_data', '').strip()
        
        # 1. Execute Local Project Mode
        if project_path:
            if not os.path.exists(project_path) or not os.path.isdir(project_path):
                error_msg = f"The path '{project_path}' does not exist or is not a directory."
            else:
                success, output = execute_gradle_project(project_path)
                if success:
                    parsed_data = parse_gradle_log(output)
                    
                    if not parsed_data:
                        # Command successfully ran but NO java errors were parsed.
                        # Expose the raw output directly so the user can see what actually happened.
                        tail_output = "\n".join(output.splitlines()[-30:]) 
                        error_msg = f"Gradle execution finished, but NO Java compilation errors were found. Is there a Gradle script error?\n\n-- Raw End of Log --\n{tail_output}"
                        print("\n=== RAW GRADLE OUTPUT ===\n" + output + "\n=========================\n")
                    else:
                        has_data = True
                else:
                    error_msg = output
                    
        # 2. Paste Log Mode (Fallback)
        elif log_data:
            parsed_data = parse_gradle_log(log_data)
            has_data = True
            
    return render_template_string(HTML_TEMPLATE, parsed_data=parsed_data, has_data=has_data, error_msg=error_msg)

if __name__ == '__main__':
    print("Starting Gradle Debugger UI on http://localhost:5000")
    # debug=True allows hot-reloading if you edit the file
    app.run(host='0.0.0.0', port=5000, debug=True)
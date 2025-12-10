# Pre-Commit Hooks Configuration Documentation

## Overview

This document describes the pre-commit hooks setup for the StudyConnect project. Pre-commit hooks are automated scripts that run before a commit is made to the Git repository. They enforce code quality, linting rules, and formatting standards automatically, preventing problematic code from being committed.

**Date Configured:** December 10, 2025
**Pre-Commit Version:** 4.5.0
**Java Version:** 21
**Spring Boot Version:** 3.5.6

---

## Table of Contents

1. [What are Pre-Commit Hooks?](#what-are-pre-commit-hooks)
2. [Installation & Setup](#installation--setup)
3. [Configuration](#configuration)
4. [Hooks Overview](#hooks-overview)
5. [Running Pre-Commit Hooks](#running-pre-commit-hooks)
6. [Execution Examples](#execution-examples)
7. [Troubleshooting](#troubleshooting)
8. [Best Practices](#best-practices)

---

## What are Pre-Commit Hooks?

Pre-commit hooks are automated checks that run **before each commit** is saved to the Git repository. They help:

- **Enforce Code Quality**: Ensure code adheres to style guides and linting rules
- **Prevent Bad Commits**: Stop commits that violate project standards
- **Automate Repetitive Tasks**: Format code, validate syntax, fix common issues
- **Maintain Consistency**: Keep the codebase uniform across all developers

**Benefits:**
- ✅ Catches issues early (before code is committed)
- ✅ Reduces code reviews burden
- ✅ Ensures consistent formatting across the team
- ✅ Automates tedious formatting and validation tasks
- ✅ No need for separate CI/CD checks on the first level

---

## Installation & Setup

### Prerequisites

- Git repository initialized
- Python 3.7+ installed
- Maven 3.6+ (for Java checks)

### Step 1: Install Python 3.12

On Windows:
```powershell
winget install python.python.3.12
```

On macOS:
```bash
brew install python@3.12
```

On Linux (Ubuntu/Debian):
```bash
sudo apt-get install python3.12 python3-pip
```

### Step 2: Install Pre-Commit Framework

```bash
# Using pip
python -m pip install --upgrade pip
python -m pip install pre-commit

# Or on Windows with full path:
C:\Users\<username>\AppData\Local\Programs\Python\Python312\python.exe -m pip install pre-commit
```

Verify installation:
```bash
pre-commit --version
```

### Step 3: Clone/Navigate to StudyConnect Project

```bash
cd path/to/studyconnect
```

### Step 4: Install Git Hooks

```bash
pre-commit install
```

Output should indicate:
```
pre-commit installed at .git/hooks/pre-commit
```

---

## Configuration

### Configuration File: `.pre-commit-config.yaml`

**Location:** `/studyconnect/.pre-commit-config.yaml`

This YAML file defines which hooks to run and how they should behave.

### Hook Definition Structure

```yaml
repos:
  - repo: <repository-url-or-local>
    rev: <version-or-commit>
    hooks:
      - id: <hook-id>
        name: <human-readable-name>
        entry: <command-to-run>
        language: <language-type>
        files: <regex-pattern>
        pass_filenames: <boolean>
        exclude: <regex-pattern>
        stages: [commit, push, manual]
```

### Key Configuration Options

| Option | Description |
|--------|-------------|
| `repo` | Repository containing the hook (local, github URL, etc.) |
| `rev` | Version/tag/commit SHA of the hook |
| `id` | Unique identifier for the hook |
| `name` | Human-readable name (shown in output) |
| `entry` | Command/script to execute |
| `language` | Language/interpreter (`system`, `python`, `node`, etc.) |
| `files` | Regex pattern: which files to check |
| `pass_filenames` | Whether to pass filenames as arguments to the hook |
| `exclude` | Regex pattern: which files to exclude |
| `stages` | When to run: `commit`, `push`, or `manual` |

---

## Hooks Overview

### Java/Maven Hooks

#### 1. **Checkstyle** (Code Style Linting)

**Purpose:** Validates Java code against the project's Checkstyle configuration (see `checkstyle.xml`).

**Configuration:**
```yaml
- id: checkstyle
  name: Checkstyle
  entry: cmd /c 'cd backend && mvn checkstyle:check -DskipTests -q'
  language: system
  files: '\.(java|properties|xml)$'
  pass_filenames: false
  stages: [commit]
```

**What it checks:**
- Naming conventions (classes, methods, variables)
- Import statements (no wildcard imports, no unused imports)
- Whitespace and indentation
- Line length (max 120 characters)
- Code block structure (braces, indentation)

**Fails if:** Checkstyle ERROR-level violations are found

---

#### 2. **Spotless Format Check** (Code Formatting)

**Purpose:** Ensures Java code matches Google Java Format standard.

**Configuration:**
```yaml
- id: spotless-check
  name: Spotless Format Check
  entry: cmd /c 'cd backend && mvn spotless:check -DskipTests -q'
  language: system
  files: '\.java$'
  pass_filenames: false
  stages: [commit]
```

**What it checks:**
- Code formatting (indentation, spacing, line breaks)
- Google Java Format compliance
- Code style consistency

**Fails if:** Code is not properly formatted

**Fix:** Run `mvn spotless:apply` in the `backend/` directory to auto-fix formatting

---

#### 3. **Maven Compile Check** (Compilation)

**Purpose:** Ensures all Java code compiles without errors.

**Configuration:**
```yaml
- id: maven-compile
  name: Maven Compile
  entry: cmd /c 'cd backend && mvn clean compile -DskipTests -q'
  language: system
  files: '\.java$'
  pass_filenames: false
  exclude: '^docs/|^\.github/'
  stages: [commit]
```

**What it checks:**
- Java syntax errors
- Missing imports
- Type mismatches
- Compilation errors

**Fails if:** Code fails to compile

---

### Generic File Hooks

#### 4. **YAML Lint** (YAML Validation)

Validates YAML files for syntax errors and formatting issues.

---

#### 5. **Check JSON** (JSON Validation)

Validates all JSON files for correct syntax.

---

#### 6. **Check YAML** (YAML Syntax)

Generic YAML syntax validation.

---

#### 7. **Check for Merge Conflicts**

Detects unresolved merge conflict markers (`<<<<<<<`, `=======`, `>>>>>>>`).

---

#### 8. **End of File Fixer**

Ensures all files end with exactly one newline. **Auto-fixes** this issue.

---

#### 9. **Trim Trailing Whitespace**

Removes trailing whitespace from all lines. **Auto-fixes** this issue.

---

#### 10. **Check for Case Conflicts**

Detects filenames that would cause problems on case-insensitive filesystems.

---

## Running Pre-Commit Hooks

### Automatic Execution (on `git commit`)

Once installed, pre-commit hooks run **automatically** when you commit:

```bash
git add .
git commit -m "Your commit message"
# Pre-commit hooks run automatically here
```

### Manual Execution

Run hooks manually on all files:

```bash
pre-commit run --all-files
```

Run hooks on specific files:

```bash
pre-commit run --files "path/to/file.java"
```

Run only a specific hook:

```bash
pre-commit run checkstyle --all-files
```

### Skip Pre-Commit Hooks

To bypass hooks (not recommended, use sparingly):

```bash
git commit --no-verify -m "Your message"
```

---

## Execution Examples

### Example 1: Normal Commit with All Hooks Passing

**Scenario:** Committing a correctly formatted Java file.

**Command:**
```bash
git add backend/src/main/java/de/softwaretesting/studyconnect/services/TaskService.java
git commit -m "feat: Add task service validation"
```

**Output:**
```
[INFO] hook id `checkstyle` uses deprecated stage names (commit) which will be removed in a future version.
Checkstyle...............................................................Passed
Spotless Format Check....................................................Passed
Maven Compile............................................................Passed
YAML Lint............................................(no files to check)Skipped
Check JSON...........................................(no files to check)Skipped
Check YAML...........................(no files to check)Skipped
Check for merge conflicts................................................Passed
Fix end of file..........................................................Passed
Trim trailing whitespace.................................................Passed
Check for case conflicts.................................................Passed

[main abc1234] feat: Add task service validation
 1 file changed, 45 insertions(+)
```

**Result:** ✅ Commit succeeds

---

### Example 2: Commit Fails - Checkstyle Violation

**Scenario:** Committing Java code with a style violation (e.g., line too long).

**Command:**
```bash
git add backend/src/main/java/de/softwaretesting/studyconnect/models/Task.java
git commit -m "refactor: Update task model"
```

**Output:**
```
Checkstyle.................................................................Failed
- hook id: checkstyle
- exit code: 1

[ERROR] src/main/java/de/softwaretesting/studyconnect/models/Task.java:45:121: Line is longer than 120 characters (found 145).
[ERROR] CHECKSTYLE ERROR - Build Failure!

Spotless Format Check....................................................Skipped
Maven Compile............................................................Skipped
```

**Result:** ❌ Commit is blocked

**Solution:**
1. Fix the style violation manually or run:
   ```bash
   mvn spotless:apply
   ```
2. Re-add and commit:
   ```bash
   git add backend/src/main/java/de/softwaretesting/studyconnect/models/Task.java
   git commit -m "refactor: Update task model"
   ```

---

### Example 3: Commit Fails - Compilation Error

**Scenario:** Committing code with a compilation error (missing import).

**Command:**
```bash
git add backend/src/main/java/de/softwaretesting/studyconnect/controller/TaskController.java
git commit -m "feat: Add new endpoint"
```

**Output:**
```
Checkstyle...............................................................Passed
Spotless Format Check....................................................Passed
Maven Compile............................................................Failed
- hook id: maven-compile
- exit code: 1

[ERROR] /backend/src/main/java/.../TaskController.java:[12,0] error: cannot find symbol
[ERROR]   symbol:   class TaskRepository
[ERROR]   location: class TaskController
[ERROR] COMPILATION ERROR

```

**Result:** ❌ Commit is blocked

**Solution:**
1. Fix the compilation error (add missing imports, fix typos)
2. Re-add and commit

---

### Example 4: Auto-Fix Hooks

**Scenario:** Files have trailing whitespace and missing newlines.

**Command:**
```bash
git add docs/README.md
git commit -m "Update docs"
```

**Output:**
```
Checkstyle...............................................................Skipped
Spotless Format Check....................................................Skipped
Maven Compile.............................Skipped
YAML Lint........................................Skipped
Check JSON..............................Skipped
Check YAML..............................Skipped
Check for merge conflicts................................................Passed
Fix end of file..........................................................Failed
- hook id: end-of-file-fixer
- exit code: 1
- files were modified by this hook

Fixing docs/README.md

Trim trailing whitespace.................................................Failed
- hook id: trailing-whitespace
- exit code: 1
- files were modified by this hook

Fixing docs/README.md
```

**Result:** ⚠️ Files automatically fixed, commit blocked

**Solution:** The files were already fixed, just re-add and commit:
```bash
git add docs/README.md
git commit -m "Update docs"
```

**Second attempt output:**
```
Check for merge conflicts................................................Passed
Fix end of file..........................................................Passed
Trim trailing whitespace.................................................Passed

[main def5678] Update docs
 1 file changed, 2 insertions(+)
```

**Result:** ✅ Commit succeeds

---

## Troubleshooting

### Issue 1: "pre-commit: command not found"

**Cause:** Pre-commit not in system PATH.

**Solutions:**

Option A: Use full path on Windows:
```powershell
C:\Users\<username>\AppData\Local\Programs\Python\Python312\Scripts\pre-commit.exe run --all-files
```

Option B: Add Python Scripts to PATH:
- On Windows, add `C:\Users\<username>\AppData\Local\Programs\Python\Python312\Scripts` to system PATH
- Restart terminal
- Try `pre-commit run --all-files`

---

### Issue 2: "mvn: command not found"

**Cause:** Maven not in system PATH.

**Solution:**
- Ensure Maven is installed: `mvn --version`
- Add Maven's `bin/` directory to system PATH
- Or use full path: `C:\path\to\maven\bin\mvn`

---

### Issue 3: Hooks Run Very Slowly

**Cause:** Maven compilation and testing is slow.

**Solutions:**
- Add `-q` (quiet) flag to reduce output (already done)
- Add `-DskipTests` to skip tests during compilation (already done)
- Run only specific hooks: `pre-commit run checkstyle --files <file>`

---

### Issue 4: "bash: command not found" on Windows

**Cause:** Git Bash is required for bash commands on Windows.

**Solutions:**
- Install Git for Windows (includes bash)
- Or change `entry` from `bash -c '...'` to `cmd /c '...'` (already done in config)

---

### Issue 5: Staged Files Not Checked

**Cause:** Hook pattern doesn't match file paths.

**Solution:**
- Check `files` regex pattern in `.pre-commit-config.yaml`
- Ensure filenames match the pattern
- Test manually: `pre-commit run --files "your/file.java" --all-files`

---

## Best Practices

### 1. **Review Hook Output**
Always read the error messages carefully. They usually indicate exactly what's wrong.

### 2. **Fix Issues Locally First**
Don't bypass hooks with `--no-verify`. Fix the root cause instead:
- Run `mvn spotless:apply` for formatting
- Fix checkstyle violations manually
- Ensure code compiles

### 3. **Run Hooks Manually Before Committing**
To save time, run hooks before staging changes:
```bash
pre-commit run --all-files
```

### 4. **Keep Configuration Updated**
Periodically update hook versions:
```bash
pre-commit autoupdate
```

### 5. **Communicate with Team**
Ensure all team members have pre-commit installed and configured:
```bash
pre-commit install
```

### 6. **Custom Hooks**
For project-specific checks, add `repo: local` hooks (like we did for Maven checks).

### 7. **Exclude Generated Code**
Ensure generated files (from Lombok, MapStruct) are properly excluded in hook configurations.

---

## Appendix

### Quick Reference Commands

```bash
# Install pre-commit framework
python -m pip install pre-commit

# Install git hooks
pre-commit install

# Run all hooks on all files
pre-commit run --all-files

# Run specific hook
pre-commit run checkstyle --all-files

# Run on specific files
pre-commit run --files "backend/src/main/java/..."

# Update hook versions
pre-commit autoupdate

# Skip hooks (not recommended)
git commit --no-verify -m "message"

# Uninstall pre-commit hooks
pre-commit uninstall
```

### File Locations

- **Configuration:** `.pre-commit-config.yaml`
- **Git Hook:** `.git/hooks/pre-commit`
- **Checkstyle Config:** `backend/checkstyle.xml`
- **Spotless Config:** `backend/pom.xml` (spotless-maven-plugin)

### References

- [Pre-Commit Official Documentation](https://pre-commit.com/)
- [Pre-Commit Hooks Available](https://pre-commit.com/hooks.html)
- [Git Hooks Documentation](https://git-scm.com/docs/githooks)
- [Checkstyle Configuration](./checkstyle-configuration.md)

---

*Document Version: 1.0*
*Last Updated: December 10, 2025*

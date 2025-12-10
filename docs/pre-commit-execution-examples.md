# Pre-Commit Hooks Execution Examples

This document provides concrete execution examples showing pre-commit hooks in action.

---

## Example 1: Successful Commit ✅

### Scenario
Committing a Java file that passes all checks (properly formatted, valid code, correct style).

### Commands Executed

```bash
# Navigate to project
cd C:\Users\nilsr\Documents\H Esslingen\Semester 5\SW-Testing\Lab\studyconnect

# Create/modify a Java file
# (In this case, adding a new service method to TaskService.java)

# Stage the changes
git add backend/src/main/java/de/softwaretesting/studyconnect/services/TaskService.java

# Commit with message
git commit -m "feat: Add task completion validation logic"
```

### Output

```
[INFO] hook id `checkstyle` uses deprecated stage names (commit) which will be removed in a future version.
[INFO] hook id `spotless-check` uses deprecated stage names (commit) which will be removed in a future version.
[INFO] hook id `maven-compile` uses deprecated stage names (commit) which will be removed in a future version.

Checkstyle...............................................................Passed
Spotless Format Check....................................................Passed
Maven Compile............................................................Passed
YAML Lint............................................(no files to check)Skipped
Check JSON...........................................(no files to check)Skipped
Check YAML...........................................(no files to check)Skipped
Check for merge conflicts................................................Passed
Fix end of file..........................................................Passed
Trim trailing whitespace.................................................Passed
Check for case conflicts.................................................Passed

[main a1b2c3d4] feat: Add task completion validation logic
 1 file changed, 23 insertions(+)
```

### Result
✅ **All checks passed, commit successful!**

The commit is created and pushed to the local repository.

---

## Example 2: Checkstyle Violation ❌

### Scenario
Committing Java code with a line that exceeds the 120-character limit (Checkstyle violation).

### Code Violation

```java
// ❌ This line is 145 characters long (exceeds max 120)
private static final String VERY_LONG_VARIABLE_NAME_THAT_EXCEEDS_THE_MAXIMUM_LINE_LENGTH_AND_SHOULD_TRIGGER_CHECKSTYLE_ERROR = "value";
```

### Commands Executed

```bash
git add backend/src/main/java/de/softwaretesting/studyconnect/models/Task.java

git commit -m "refactor: Update Task model with new fields"
```

### Output

```
[WARNING] hook id `checkstyle` uses deprecated stage names (commit) which will be removed in a future version.
[WARNING] hook id `spotless-check` uses deprecated stage names (commit) which will be removed in a future version.
[WARNING] hook id `maven-compile` uses deprecated stage names (commit) which will be removed in a future version.

Checkstyle...................................................................Failed
- hook id: checkstyle
- exit code: 1

[ERROR] File: backend/src/main/java/de/softwaretesting/studyconnect/models/Task.java
[ERROR] Line 42: 'public' modifier out of order with the JLS suggestions.
[ERROR] Line 52: Line is longer than 120 characters (found 145).

CHECKSTYLE ERROR - Fix the above violations before committing.
```

### Result
❌ **Commit blocked! Checkstyle violations found.**

**Fix options:**

**Option 1: Manual Fix**
```java
// ✅ Fix 1: Break into multiple lines
private static final String VERY_LONG_VARIABLE_NAME = "value";
private static final String ADDITIONAL_INFO = "more data";

// ✅ Fix 2: Use shorter names
private static final String LONG_VAR_NAME = "value";
```

**Option 2: Auto-Fix with Spotless**
```bash
cd backend
mvn spotless:apply
cd ..
```

**Then Re-Commit:**
```bash
git add backend/src/main/java/de/softwaretesting/studyconnect/models/Task.java
git commit -m "refactor: Update Task model with new fields"
```

**Output (after fix):**
```
Checkstyle...............................................................Passed
Spotless Format Check....................................................Passed
Maven Compile............................................................Passed
YAML Lint............................................(no files to check)Skipped
Check JSON...........................................(no files to check)Skipped
Check YAML...........................................(no files to check)Skipped
Check for merge conflicts................................................Passed
Fix end of file..........................................................Passed
Trim trailing whitespace.................................................Passed
Check for case conflicts.................................................Passed

[main b2c3d4e5] refactor: Update Task model with new fields
 1 file changed, 18 insertions(+)
```

---

## Example 3: Compilation Error ❌

### Scenario
Committing Java code with a compilation error (missing import or type mismatch).

### Code Error

```java
// ❌ Using TaskRepository without importing it
@Service
public class TaskService {
    @Autowired
    private TaskRepository taskRepository;  // ERROR: Cannot find symbol

    public void saveTask(Task task) {
        taskRepository.save(task);
    }
}
```

### Commands Executed

```bash
git add backend/src/main/java/de/softwaretesting/studyconnect/services/TaskService.java

git commit -m "feat: Add task service methods"
```

### Output

```
Checkstyle...............................................................Passed
Spotless Format Check....................................................Passed
Maven Compile..............................................................Failed
- hook id: maven-compile
- exit code: 1

[INFO] Compiling 24 source files to backend/target/classes
[ERROR] /backend/src/main/java/de/softwaretesting/studyconnect/services/TaskService.java:[8,0] error: cannot find symbol
[ERROR]   symbol:   class TaskRepository
[ERROR]   location: class TaskService
[ERROR] COMPILATION ERROR - Fix the above errors and try again.
```

### Result
❌ **Commit blocked! Compilation error.**

**Fix:**
```java
// ✅ Add missing import
import de.softwaretesting.studyconnect.repositories.TaskRepository;

@Service
public class TaskService {
    @Autowired
    private TaskRepository taskRepository;

    public void saveTask(Task task) {
        taskRepository.save(task);
    }
}
```

**Then Re-Commit:**
```bash
git add backend/src/main/java/de/softwaretesting/studyconnect/services/TaskService.java
git commit -m "feat: Add task service methods"
```

**Output (after fix):**
```
Checkstyle...............................................................Passed
Spotless Format Check....................................................Passed
Maven Compile............................................................Passed
[main c3d4e5f6] feat: Add task service methods
 1 file changed, 15 insertions(+)
```

---

## Example 4: Auto-Fix with Trailing Whitespace & EOF ✅

### Scenario
Committing files with trailing whitespace and missing newlines at end-of-file.

### Issues Found

```
File: docs/README.md
- Line 5: Has trailing whitespace at end: "## Overview    " (4 spaces)
- Line 12: Has trailing whitespace: "- Install Maven    "
- EOF: File doesn't end with newline
```

### Commands Executed

```bash
git add docs/README.md

git commit -m "docs: Update README"
```

### Output

```
Checkstyle...................................................................Skipped
Spotless Format Check....................................................Skipped
Maven Compile..............................................................Skipped
YAML Lint........................................(no files to check)Skipped
Check JSON..............................(no files to check)Skipped
Check YAML..............................(no files to check)Skipped
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

### Result
⚠️ **Commit blocked but files were AUTO-FIXED!**

The hooks automatically:
- ✅ Removed all trailing whitespace
- ✅ Added newline at end of file

**Simply Re-Commit (files already fixed):**

```bash
git add docs/README.md
git commit -m "docs: Update README"
```

### Output (after re-commit)

```
Checkstyle...................................................................Skipped
Spotless Format Check....................................................Skipped
Maven Compile..............................................................Skipped
YAML Lint........................................(no files to check)Skipped
Check JSON..............................(no files to check)Skipped
Check YAML..............................(no files to check)Skipped
Check for merge conflicts................................................Passed
Fix end of file..........................................................Passed
Trim trailing whitespace.................................................Passed
Check for case conflicts.................................................Passed

[main d4e5f6g7] docs: Update README
 1 file changed, 2 insertions(+)
```

✅ **Commit successful!**

---

## Example 5: Manual Hook Execution on Entire Codebase

### Scenario
Running pre-commit hooks on all files in the project (not triggered by commit).

### Commands Executed

```bash
cd C:\Users\nilsr\Documents\H Esslingen\Semester 5\SW-Testing\Lab\studyconnect

# Run all hooks on all files
pre-commit run --all-files --hook-stage commit
```

### Output (partial - first 50 lines)

```
[INFO] Initializing environment for https://github.com/adrienverge/yamllint.git.
[INFO] Initializing environment for https://github.com/pre-commit/pre-commit-hooks.
[INFO] Installing environment for https://github.com/adrienverge/yamllint.git.
[INFO] Once installed this environment will be reused.
[INFO] This may take a few minutes...
[INFO] Installing environment for https://github.com/pre-commit/pre-commit-hooks.
[INFO] Once installed this environment will be reused.
[INFO] This may take a few minutes...

Checkstyle...............................................................Passed
Spotless Format Check....................................................Passed
Maven Compile............................................................Passed
YAML Lint...................................................................Passed
Check JSON...........................................(no files to check)Skipped
Check YAML...................................................................Passed
Check for merge conflicts................................................Passed
Fix end of file..........................................................Passed
Trim trailing whitespace.................................................Passed
Check for case conflicts.................................................Passed
```

### Result
✅ **All hooks passed on entire codebase!**

---

## Example 6: Skipping Hooks (Not Recommended!)

### Scenario
Forcefully committing code without running hooks (emergency-only, not recommended).

### Commands Executed

```bash
git add .

# Skip pre-commit hooks with --no-verify flag
git commit --no-verify -m "emergency: Hotfix for production issue"
```

### Output

```
[main e5f6g7h8] emergency: Hotfix for production issue
 3 files changed, 42 insertions(+)
```

### Result
⚠️ **Commit successful WITHOUT running hooks!**

**⚠️ WARNING:** This should only be used in emergencies. Always run hooks before pushing!

```bash
# After emergency fix, still run hooks to verify:
pre-commit run --all-files
```

---

## Example 7: Testing Specific Hook

### Scenario
Running only the Checkstyle hook on Java files to verify style compliance.

### Commands Executed

```bash
cd C:\Users\nilsr\Documents\H Esslingen\Semester 5\SW-Testing\Lab\studyconnect

# Run only Checkstyle on all Java files
pre-commit run checkstyle --all-files
```

### Output

```
Checkstyle...............................................................Passed
```

### Result
✅ **Checkstyle passed!**

All Java code complies with Checkstyle rules.

---

## Summary Table

| Scenario | Result | Action |
|----------|--------|--------|
| Valid code, correct formatting | ✅ Pass | Commit succeeds |
| Checkstyle violation (line too long) | ❌ Fail | Fix code or run `mvn spotless:apply` |
| Compilation error | ❌ Fail | Fix syntax/imports |
| Trailing whitespace | ⚠️ Auto-fix | Re-commit (already fixed) |
| Missing newline at EOF | ⚠️ Auto-fix | Re-commit (already fixed) |
| All passes on full codebase | ✅ Pass | All good! |
| Emergency skip | ⚠️ Success | Still verify hooks later |

---

## Tips for Developers

### 1. **Run Before Committing**
```bash
pre-commit run --all-files
```
This saves time by catching issues early.

### 2. **Auto-Fix Formatting**
```bash
cd backend && mvn spotless:apply
```
Use this to automatically fix all formatting issues.

### 3. **Check Single File**
```bash
pre-commit run --files "path/to/file.java"
```

### 4. **Update Hooks**
```bash
pre-commit autoupdate
```
Periodically update hook versions to latest.

### 5. **Read Error Messages**
Error messages are usually clear about what to fix. Read them carefully!

---

*Last Updated: December 10, 2025*

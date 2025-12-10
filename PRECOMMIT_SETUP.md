# Pre-Commit Hooks Setup Guide for Developers

## Quick Setup (5 minutes)

### 1. Install Python 3.12

**Windows:**
```powershell
winget install python.python.3.12
```

**macOS:**
```bash
brew install python@3.12
```

**Linux (Ubuntu/Debian):**
```bash
sudo apt-get install python3.12 python3-pip
```

### 2. Install Pre-Commit

```bash
python -m pip install pre-commit
```

### 3. Navigate to Project and Install Hooks

```bash
cd studyconnect
pre-commit install
```

**Output:**
```
pre-commit installed at .git/hooks/pre-commit
```

### 4. Done! ✅

Now pre-commit hooks will run automatically on every commit.

---

## What Happens Next?

### On Your First Commit

When you commit code:

```bash
git add .
git commit -m "Your message"
```

Pre-commit hooks will run automatically:

```
Checkstyle...............................................................Passed
Spotless Format Check....................................................Passed
Maven Compile............................................................Passed
[main abc1234] Your message
 5 files changed
```

### If Hooks Fail

If code doesn't pass checks:

```
Checkstyle...............................................................Failed
- hook id: checkstyle
- exit code: 1

[ERROR] Line is longer than 120 characters
```

**Fix it:**
1. Read the error message
2. Fix the code manually OR auto-fix with: `mvn spotless:apply`
3. Re-add and commit: `git add . && git commit -m "Your message"`

---

## Useful Commands

| Command | Purpose |
|---------|---------|
| `pre-commit run --all-files` | Test all hooks on entire codebase |
| `pre-commit run checkstyle --all-files` | Test only Checkstyle hook |
| `pre-commit run --files file.java` | Test only specific file |
| `git commit --no-verify` | Skip hooks (use sparingly!) |
| `pre-commit autoupdate` | Update hook versions |

---

## Troubleshooting

### "pre-commit: command not found"

Add Python Scripts folder to PATH or use full path:

```powershell
# Windows - find Python installation:
where python.exe
# Then add the Scripts folder to your PATH

# Or use full path:
C:\Users\YourName\AppData\Local\Programs\Python\Python312\Scripts\pre-commit.exe run --all-files
```

### "mvn: command not found"

Ensure Maven is installed and in PATH:

```bash
mvn --version
```

### Hooks Run Slowly

That's normal on first run. Maven compiles everything. Subsequent runs are faster (incremental compilation).

---

## Full Documentation

See `docs/pre-commit-hooks-configuration.md` for complete documentation with:
- Hook details
- Execution examples
- Troubleshooting guide
- Best practices

---

## Questions?

Refer to:
- [Pre-Commit Documentation](https://pre-commit.com/)
- `docs/pre-commit-hooks-configuration.md`
- `docs/checkstyle-configuration.md`

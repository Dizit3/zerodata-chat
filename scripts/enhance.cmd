@echo off
REM Windows launcher: cursor-harness 3.2.1 crashes on cp1251 when writing hook scripts (emoji).
setlocal
cd /d "%~dp0.."
set PYTHONUTF8=1
set PYTHONIOENCODING=utf-8
chcp 65001 >nul

if not exist "docs\development\enhancement_spec.txt" (
  echo Run this from the git clone that contains docs\development\enhancement_spec.txt
  echo Current directory: %CD%
  exit /b 1
)

python scripts\repair-harness-prompts.py
if errorlevel 1 exit /b 1

python -m cursor_harness.cli enhance . --spec docs\development\enhancement_spec.txt %*
exit /b %ERRORLEVEL%

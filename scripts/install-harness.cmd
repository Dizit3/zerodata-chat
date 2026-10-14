@echo off
setlocal
cd /d "%~dp0.."
set PYTHONUTF8=1
set PYTHONIOENCODING=utf-8
python -m pip install -r requirements-harness.txt
python -m cursor_harness.cli --version
echo.
echo Then run: scripts\enhance.cmd

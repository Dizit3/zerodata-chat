Prompt templates copied from nirmalarya/cursor-harness tag v3.2.1.

The PyPI/git wheel for 3.2.1 ships only .py files (no package_data for prompts/*.md).
After `pip install`, copy this prompts/ folder into the installed cursor_harness package:

  python scripts/repair-harness-prompts.py

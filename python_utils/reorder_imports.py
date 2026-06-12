#!/usr/bin/env python3
"""Reorganize Java imports into 3 groups, each sorted alphabetically:
  1. java.*
  2. Third-party libraries
  3. Internal project (com.serbekun.*)
"""

import os
import re

JAVA_DIR = "src/main/java"
SKIP_FILE = os.path.join(JAVA_DIR, "com/serbekun/Main.java")

def group_key(imp):
    """Return (group, sort_key) where group is 0=java, 1=third-party, 2=internal."""
    text = imp.strip()
    if text.startswith("import java."):
        return (0, text.lower())
    elif text.startswith("import com.serbekun."):
        return (2, text.lower())
    else:
        return (1, text.lower())

def reorganize_imports(content):
    lines = content.splitlines(keepends=True)

    # Find the package line and import lines
    package_line = None
    import_lines = []
    body_start = 0

    for i, line in enumerate(lines):
        stripped = line.strip()
        if stripped.startswith("package ") and stripped.endswith(";"):
            package_line = i
        elif stripped.startswith("import "):
            import_lines.append(i)
        elif package_line is not None and not stripped.startswith("import ") and not stripped == "":
            if body_start == 0:
                body_start = i
            break

    if not import_lines:
        return content

    # If body_start wasn't set (imports go to end of file somehow), set it
    if body_start == 0:
        body_start = import_lines[-1] + 1

    # Classify, sort
    imports = [lines[i].strip() for i in import_lines]
    imports.sort(key=group_key)

    # Separate into groups
    java_imports = [imp for imp in imports if group_key(imp)[0] == 0]
    third_party_imports = [imp for imp in imports if group_key(imp)[0] == 1]
    internal_imports = [imp for imp in imports if group_key(imp)[0] == 2]

    # Build new import block
    new_import_lines = []
    for group in (java_imports, third_party_imports, internal_imports):
        if group:
            if new_import_lines:
                new_import_lines.append("\n")
            for imp in group:
                new_import_lines.append(imp + "\n")

    # If the original file has a blank line after package, keep it
    # Find the blank line(s) between package and first import
    first_import_idx = import_lines[0]
    blank_lines_before = []
    idx = package_line + 1
    while idx < first_import_idx:
        if lines[idx].strip() == "":
            blank_lines_before.append(lines[idx])
        idx += 1

    # Find blank lines after last import
    last_import_idx = import_lines[-1]
    blank_lines_after = []
    idx = last_import_idx + 1
    while idx < len(lines) and lines[idx].strip() == "":
        blank_lines_after.append(lines[idx])
        idx += 1

    # Preserve body from after the imports
    body_start_actual = last_import_idx + 1 + len(blank_lines_after)
    body = lines[body_start_actual:]

    # Reassemble
    new_content_lines = []
    new_content_lines.append(lines[package_line])  # package line
    for bl in blank_lines_before:
        new_content_lines.append(bl)

    # Add newline before imports if there are blank lines before
    if blank_lines_before:
        # already have blank lines
        pass
    else:
        new_content_lines.append("\n")

    new_content_lines.extend(new_import_lines)

    # Maintain blank lines between imports and body
    if new_import_lines and body and body[0].strip() == "":
        # body already starts with blank line
        new_content_lines.extend(body)
    elif new_import_lines and body:
        new_content_lines.append("\n")
        new_content_lines.extend(body)
    else:
        new_content_lines.extend(body)

    return "".join(new_content_lines)


def process_file(filepath):
    with open(filepath, "r") as f:
        content = f.read()

    new_content = reorganize_imports(content)

    if new_content != content:
        with open(filepath, "w") as f:
            f.write(new_content)
        print(f"  Reorganized: {filepath}")
    else:
        print(f"  No change:   {filepath}")


def main():
    for root, dirs, files in os.walk(JAVA_DIR):
        for fn in files:
            if not fn.endswith(".java"):
                continue
            filepath = os.path.join(root, fn)
            if os.path.abspath(filepath) == os.path.abspath(SKIP_FILE):
                continue
            process_file(filepath)


if __name__ == "__main__":
    main()

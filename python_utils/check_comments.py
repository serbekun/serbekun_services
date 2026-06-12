import os
import re

def has_java_comment(filepath):
    with open(filepath, 'r', encoding='utf-8') as f:
        content = f.read()
    return bool(re.search(r'//|/\*', content))

def main():
    root = 'src/main/'
    files_no_comment = []
    for dirpath, _, filenames in os.walk(root):
        for f in filenames:
            if f.endswith('.java'):
                full = os.path.join(dirpath, f)
                if not has_java_comment(full):
                    files_no_comment.append(full)
    if not files_no_comment:
        print("All Java files have comments.")
    else:
        print("Files WITHOUT comments:")
        for f in files_no_comment:
            print(f)

if __name__ == '__main__':
    main()

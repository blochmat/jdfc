import regex as re
import os
import sys
import subprocess
import json

def extract_test_methods(file_path):
    with open(file_path, 'r', encoding='utf-8') as file:
        content = file.read()

        # Look for annotations followed by method declarations
        pattern = r'@Test\s+public\s+void\s+([\w_]+)\s*\('
        matches = re.findall(pattern, content)

        return matches

def find_test_methods(directory_path, test_classes):
    # input: org.apache.commons.lang3.ClassA
    all_test_methods = []

    # Walk through the directory to get all .java files
    for root, dirs, files in os.walk(directory_path):
        for file in files:
            # file: ClassA.java
            if file.endswith("Test.java"):
                file_without_ext = os.path.splitext(file)[0]
                if any(file_without_ext in c for c in test_classes):
                    file_path = os.path.join(root, file)
                    methods = extract_test_methods(file_path)
                    file_path = os.path.splitext(file_path)[0]
                    substring = "java/"
                    parts = file_path.split(substring)
                    file_name = parts[1].replace("/", ".")
                    all_test_methods.extend([(file_name, method) for method in methods])

    return all_test_methods

if len(sys.argv) > 2:
    directory_path = sys.argv[1]
    test_classes = sys.argv[2].split(',')
    tests = find_test_methods(directory_path, test_classes)
    methods = []
    with open("test_method_list", 'w') as file:
        for class_name, method_name in tests:
            file.write(f"{class_name}::{method_name}\n")
            methods.append(f"{class_name}::{method_name}")
            #command = f"defects4j coverage -t {class_name}::{method_name}"
            #print(command)
            #subprocess.run(command, shell=True, cwd=directory_path)
    print(json.dumps(methods))
else:
    print("Please pass the target directory and the affected class name as a string")

#bug_ids=$(defects4j query -p Lang)
#for item in $bug_ids; do
    #PINFO=$(defects4j info -p Lang -b "$item")
    #AFFECTEDCLASS=$(echo "$PINFO" | awk '/List of modified sources:/,/---/{if (!/List of modified sources:/ && !/---/) print $2}')
    #echo "$AFFECTEDCLASS"

    ##$(defects4j checkout -p Lang -v "$item"b -w /tmp/lang_"$item"_buggy)

    #python ~/important_tmp_scripts/get_test_methods.py /tmp/lang_"$item"_buggy "$AFFECTEDCLASS"
#done

pc_analysis_dir="/tmp/pc_analysis"

if [ -d "$pc_analysis_dir" ]; then
    rm -r "$pc_analysis_dir"
fi
mkdir -p "$pc_analysis_dir"


project="Lang"
bug_id=1

repo_dir="/tmp/${project}_${bug_id}_buggy"
project_output_dir="${pc_analysis_dir}/${project}"
bug_output_dir="${project_output_dir}/${bug_id}"
coverage_file="${bug_output_dir}/coverage.csv"

if [ -d "$bug_output_dir" ]; then
    rm -r "$bug_output_dir"
fi
mkdir -p "$bug_output_dir"

## Setup
# Query bug_id, relevant test classes map
relevant_test_classes_map=$(defects4j query -p "$project" -q "tests.relevant")
# Extract relevant test classes by bug id
relevant_test_classes_string=$(echo "$relevant_test_classes_map" | grep "^$bug_id," | cut -d',' -f2 | tr -d '"')
# Split line by ";" and store in array
IFS=';' read -ra test_classes_array <<< "$relevant_test_classes_string"
#echo "${test_classes_array[@]}"

# Query bug_id, triggering tests map
triggering_tests_map=$(defects4j query -p "$project" -q "tests.trigger")
# Extract relevant test classes by bug id
triggering_tests_string=$(echo "$triggering_tests_map" | grep "^$bug_id," | cut -d',' -f2 | tr -d '"')
# Split line by ";" and store in array
IFS=';' read -ra triggering_tests_array <<< "$triggering_tests_string"
#echo "${triggering_tests_array[@]}"

# Checkout and compile one buggy version
$(defects4j checkout -p "$project" -v "$bug_id"b -w "$repo_dir")
$(defects4j compile -w "$repo_dir")

# Run all relevant tests
echo "$(defects4j coverage -w "$repo_dir" -r)"

# Extract all coverage goals
readarray -t line_numbers < <(xmlstarlet sel -T -t -m "/coverage/packages/package/classes/class/lines/line" -v "@number" -n "$repo_dir"/coverage.xml | sort -n | uniq)

# Add header row to csv
echo ",failed,${line_numbers[*]}" | sed 's/ /,/g' > "$coverage_file"

## Get test methods

# Join the array elements in order to pass it as single parameter to the python script
test_classes_array_joined=$(printf "%s," "${test_classes_array[@]}")
test_classes_array_joined=${test_classes_array_joined%,}
#echo "${test_classes_array_joined[@]}"

# Extract all relevant test methods (format: ["a","b",...])
test_methods_py=$(python ~/repos/hub/jdfc/tmp_scripts/get_test_methods.py "$repo_dir" "$test_classes_array_joined")
#echo $test_methods_py

# Convert test methods to sh array
test_methods_sh=($(echo $test_methods_py | jq -r '.[]'))

## Execute test methods one by one
for test_method in "${test_methods_sh[@]}"; do
    echo "$test_method"
    # Execute test method
    echo "$(defects4j coverage -w "$repo_dir" -t "$test_method")"
    # Extract covered coverage goals from coverage.xml
    readarray -t hits < <(xmlstarlet sel -T -t -m "/coverage/packages/package/classes/class/lines/line" -v "@hits" -n "$repo_dir"/coverage.xml)
    coverage=()
    failed=""

    for ttest in "${triggering_tests_array[@]}"; do
        if [ "$ttest" == "$test_method" ]; then
            failed="x"
            break
        fi
    done

    for i in "${hits[@]}"; do
        if [ $i -gt 0 ]; then
            coverage+=("x")
        else
            coverage+=("")
        fi
    done
    echo "$test_method,$failed,${coverage[*]}" | sed 's/ /,/g' >> "$coverage_file"
done

sh compute_pc_all_lines.sh "$coverage_file" "$bug_output_dir"
sh compute_pc_relevant_lines.sh "$coverage_file" "$bug_output_dir"
sh compute_pc_bug.sh "$coverage_file" "$project_output_dir" "$bug_id"

#bug_ids=$(defects4j query -p Lang)
#for item in $bug_ids; do
    #PINFO=$(defects4j info -p Lang -b "$item")
    #AFFECTEDCLASS=$(echo "$PINFO" | awk '/List of modified sources:/,/---/{if (!/List of modified sources:/ && !/---/) print $2}')
    #echo "$AFFECTEDCLASS"

    ##$(defects4j checkout -p Lang -v "$item"b -w /tmp/lang_"$item"_buggy)

    #python ~/important_tmp_scripts/get_test_methods.py /tmp/lang_"$item"_buggy "$AFFECTEDCLASS"
#done

## Setup
# Query bug_id, relevant test classes map
relevant_test_classes_map=$(defects4j query -p Lang -q "tests.relevant")
# Extract relevant test classes by bug id
id=1
relevant_test_classes_string=$(echo "$relevant_test_classes_map" | grep "^$id," | cut -d',' -f2 | tr -d '"')
# Split line by ";" and store in array
IFS=';' read -ra test_classes_array <<< "$relevant_test_classes_string"
echo "${test_classes_array[@]}"

# Query bug_id, triggering tests map
triggering_tests_map=$(defects4j query -p Lang -q "tests.trigger")
# Extract relevant test classes by bug id
id=1
triggering_tests_string=$(echo "$triggering_tests_map" | grep "^$id," | cut -d',' -f2 | tr -d '"')
# Split line by ";" and store in array
IFS=';' read -ra triggering_tests_array <<< "$triggering_tests_string"
echo "${triggering_tests_array[@]}"

# Checkout and compile one buggy version
$(defects4j checkout -p Lang -v 1b -w /tmp/lang_1_buggy)
$(defects4j compile -w /tmp/lang_1_buggy)

# Run all relevant tests
echo "$(defects4j coverage -w /tmp/lang_1_buggy -r)"

# Extract all coverage goals
readarray -t line_numbers < <(xmlstarlet sel -T -t -m "/coverage/packages/package/classes/class/lines/line" -v "@number" -n /tmp/lang_1_buggy/coverage.xml | sort -n | uniq)

# Add header row to csv
echo ",failed,${line_numbers[*]}" | sed 's/ /,/g' > analysis_result_1.csv

## Get test methods

# Join the array elements in order to pass it as single parameter to the python script
test_classes_array_joined=$(printf "%s," "${test_classes_array[@]}")
test_classes_array_joined=${test_classes_array_joined%,}
echo "${test_classes_array_joined[@]}"

# Extract all relevant test methods (format: ["a","b",...])
test_methods_py=$(python ~/repos/hub/jdfc/tmp_scripts/get_test_methods.py /tmp/lang_1_buggy "$test_classes_array_joined")
echo $test_methods_py

# Convert test methods to sh array
test_methods_sh=($(echo $test_methods_py | jq -r '.[]'))

## Execute test methods one by one
for test_method in "${test_methods_sh[@]}"; do
    echo "$test_method"
    # Execute test method
    echo "$(defects4j coverage -w /tmp/lang_1_buggy -t "$test_method")"
    # Extract covered coverage goals from coverage.xml
    readarray -t hits < <(xmlstarlet sel -T -t -m "/coverage/packages/package/classes/class/lines/line" -v "@hits" -n /tmp/lang_1_buggy/coverage.xml)
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
    echo "$test_method,$failed,${coverage[*]}" | sed 's/ /,/g' >> analysis_result_1.csv
done




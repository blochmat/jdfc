#!/opt/homebrew/bin/bash
current_dir=$(pwd)
working_dir=$1
pc_analysis_dir="/tmp/pc_analysis/dataflow"

if [ ! -d "${pc_analysis_dir:?}" ]; then
    mkdir -p "${pc_analysis_dir:?}"
#elif [ -n "$(ls -A ${pc_analysis_dir:?})" ]; then
#    rm -r "${pc_analysis_dir:?}"/*
fi

#bug_ids=$(defects4j query -p "$project")
# Lang
# YES: 1 3 4 7 11 12 15 16 23 24 27 36 44 53 54 55 57 65
# NOPE: 4 6 8 9 10 13 14 17 18 19 20 21 22 25 26 28 29 30 31 32 33 34 35 37 38 39 40 41 42 45 46 47 48 49 50 51 52 56 58 59 60 61 62 63 64
# ENDLESS: 43

project="Time"
# Time
# YES:
# NOPE:
# ENDLESS:
bug_ids=(1 2 3 4 5 6 7 8 9 10 11 12 13 14 15 16 17 18 19 20 22 23 24 25 26 27)
echo "Starting dataflow analysis for project:  ${project}."
for bug_id in "${bug_ids[@]}"; do
    repo_dir="${working_dir}/${project}_${bug_id}b"
    project_output_dir="${pc_analysis_dir}/${project}"
    bug_output_dir="${project_output_dir}/${bug_id}"
    coverage_file="${bug_output_dir}/dataflow_coverage.csv"

    echo "Starting dataflow analysis for bugId:  ${bug_id}."
    echo "  - Working directory:  ${repo_dir}."

    # create if directory does not exist
    if [ ! -d "$bug_output_dir" ]; then
        mkdir -p "$bug_output_dir"
    fi

    # delere if coverage file already exists
    if [ -f "$coverage_file" ]; then
        rm "$coverage_file"
    fi

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
    echo "$(defects4j jdfc -w "$repo_dir" -r)"

    # Extract all coverage goals
    readarray -t pair_ids < <(xmlstarlet sel -T -t -m "/coverage/packages/package/classes/class/pairs/pair" -v "@id" -n "$repo_dir"/jdfc-report/coverage.xml | sort -n | uniq)

    # Add header row to csv
    echo ",failed,${pair_ids[*]}" | sed 's/ /,/g' > "$coverage_file"

    ## Get test methods

    # Join the array elements in order to pass it as single parameter to the python script
    test_classes_array_joined=$(printf "%s," "${test_classes_array[@]}")
    test_classes_array_joined=${test_classes_array_joined%,}
    #echo "${test_classes_array_joined[@]}"

    # Extract all relevant test methods (format: ["a","b",...])
    test_methods_py=$(python "$current_dir/get_test_methods.py" "$repo_dir" "$test_classes_array_joined")
    #echo $test_methods_py

    # Convert test methods to sh array
    test_methods_sh=($(echo $test_methods_py | jq -r '.[]'))

    ## Execute test methods one by one
    for test_method in "${test_methods_sh[@]}"; do
        echo "$test_method"
        # Execute test method
        echo "$(defects4j jdfc -w "$repo_dir" -t "$test_method")"
        # Extract covered coverage goals from coverage.xml
        readarray -t covered < <(xmlstarlet sel -T -t -m "/coverage/packages/package/classes/class/pairs/pair" -v "@covered" -n "$repo_dir"/jdfc-report/coverage.xml)
        coverage=()
        failed=""

        for ttest in "${triggering_tests_array[@]}"; do
            if [ "$ttest" == "$test_method" ]; then
                failed="x"
                break
            fi
        done

        for i in "${covered[@]}"; do
            if [ "$i" = "true" ]; then
                coverage+=("x")
            else
                coverage+=("")
            fi
        done
        echo "$test_method,$failed,${coverage[*]}" | sed 's/ /,/g' >> "$coverage_file"
    done
    
    bash "$current_dir/compute_pc_all.sh" "$coverage_file" "$bug_output_dir"
    bash "$current_dir/compute_pc_relevant.sh" "$coverage_file" "$bug_output_dir"
    bash "$current_dir/compute_pc_bug.sh" "$coverage_file" "$project_output_dir" "$bug_id"

    # delete repo after analysis
    rm -rf "$repo_dir"
done

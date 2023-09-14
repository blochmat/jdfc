project="Lang"
#bug_ids=(1)
bug_ids=$(defects4j query -p "$project")
for bug_id in $bug_ids; do
    repo_dir="/home/blochmat/Desktop/${project}_${bug_id}_buggy"

    # Checkout and compile one buggy version
    $(defects4j checkout -p "$project" -v "$bug_id"b -w "$repo_dir")
    $(defects4j compile -w "$repo_dir")
done

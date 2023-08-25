#!/bin/sh

if [ -z "$1" ]; then
  echo "Please provide the path to the CSV file."
  exit 1
fi

bug_id=$(basename "$1" | cut -d '.' -f 1 | cut -d 'g' -f 2)
max_quotient=0
output_file="prob_coupling_final.csv"

# Add header if file does not exist
if [ ! -f "$output_file" ]; then
  echo "BugId,ProbCoup" > "$output_file"
fi

awk -F',' -v bug_id="$bug_id" -v output_file="$output_file" '
  NR == 1 { for (i=3; i<=NF; i++) lines[i-2] = $i }
  NR > 1 {
    failed = $2 == "x" ? 1 : 0
    for (i=3; i<=NF; i++) {
      line_number = lines[i-2]
      if ($i == "x") {
        den[line_number]++
        if (failed) num[line_number]++
      }
    }
  }
  END { 
    for (i in lines) {
      line_number = lines[i]
      if (num[line_number] > 0 && den[line_number] > 0) {
        quotient = num[line_number] / den[line_number]
        if (quotient > max_quotient) {
          max_quotient = quotient
        }
      }
    }
    print bug_id "," max_quotient >> output_file
  }' "$1"

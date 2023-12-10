#!/bin/sh

# Computes probabilistic coupling for all lines but only saves
# all lines with pc != 0 in a csv file.

if [ -z "$1" ]; then
  echo "Please provide a path to the coverage CSV file."
  exit 1
fi

if [ -z "$2" ]; then
  echo "Please provide an output path."
  exit 1
fi

output_file="${2}/pc_relevant.csv"

if [ ! -d "$2" ]; then
    mkdir -p "$2"
fi

awk -F',' -v output_file="$output_file" '
  BEGIN { print "Line,ProbCoup" >> output_file }
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
      if (num[line_number] > 0) {
        prob = num[line_number] "\\" den[line_number]
        print line_number "," prob >> output_file
      }
    }
  }' "$1"

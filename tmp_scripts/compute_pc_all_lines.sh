#!/bin/sh

if [ -z "$1" ]; then
  echo "Please provide a path to the coverage CSV file."
  exit 1
fi

if [ -z "$2" ]; then
  echo "Please provide an output path."
  exit 1
fi

output_file="${2}/pc_all_lines.csv"

if [ ! -d "$2" ]; then
    mkdir -p "$2"
fi

awk -F',' -v output_file="$output_file" '
  BEGIN { print "Line,ProbCoup" >> output_file }
  NR == 1 { for (i=3; i<=NF; i++) { num[$i] = 0; den[$i] = 0; lines[i-2] = $i } }
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
      prob = (den[line_number] > 0 ? num[line_number] "\\" den[line_number] : 0)
      print line_number "," prob >> output_file
    }
  }' "$1"

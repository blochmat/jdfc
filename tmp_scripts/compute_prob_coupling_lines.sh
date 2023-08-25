#!/bin/sh

if [ -z "$1" ]; then
  echo "Please provide the path to the CSV file."
  exit 1
fi

awk -F',' '
  BEGIN { print "Line,ProbCoup" > "prob_coupling_lines.csv" }
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
        print line_number "," prob > "prob_coupling_lines.csv"
      }
    }
  }' "$1"

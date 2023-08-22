#BUGIDS=$(defects4j query -p Lang)
#for item in $BUGIDS; do
    #PINFO=$(defects4j info -p Lang -b "$item")
    #AFFECTEDCLASS=$(echo "$PINFO" | awk '/List of modified sources:/,/---/{if (!/List of modified sources:/ && !/---/) print $2}')
    #echo "$AFFECTEDCLASS"

    ##$(defects4j checkout -p Lang -v "$item"b -w /tmp/lang_"$item"_buggy)

    #python ~/important_tmp_scripts/get_test_methods.py /tmp/lang_"$item"_buggy "$AFFECTEDCLASS"
#done

pinfo=$(defects4j info -p Lang -b 1)
affected=$(echo "$pinfo" | awk '/List of modified sources:/,/---/{if (!/List of modified sources:/ && !/---/) print $2}')
echo "$affected"

#bla="$(defects4j export -w /tmp/lang_1_buggy -p classes.modified)"
#echo $bla

$(defects4j checkout -p Lang -v 1b -w /tmp/lang_1_buggy)
$(defects4j compile -w /tmp/lang_1_buggy)

methods=$(python ~/important_tmp_scripts/get_test_methods.py /tmp/lang_1_buggy "$affected")
echo $methods
sh_array=($(echo $methods | jq -r '.[]'))
echo "${sh_array[@]}"
#echo "$(defects4j coverage -w /tmp/lang_1_buggy -t ${sh_array[0]})"


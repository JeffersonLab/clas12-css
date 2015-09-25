#!/bin/bash

TOP=`pwd`
MVN_CMD="mvn --debug --settings $TOP/settings.xml clean verify"

# used in settings.xml
export CSS_TOP=$TOP

# order matters!!!
MODULES="cs-studio/core \
    cs-studio/applications \
    org.csstudio.sns"

start=$(date +"%s")
cols=`tput cols`
FAILED=0

function printbar {
    printf "=%.0s" $(seq 1 $cols)
}

for mod in $MODULES
do
	cd $TOP/$mod
    $MVN_CMD
    if [ $? -eq 0 ]; then
        printbar
        printf "\n%s %s\n" $mod passed
        printbar
    else
        FAILED=1
        printbar
        printf "\n%s %s\n" $mod failed
        printbar
        break
    fi
done

#cd org.cstudio.clas12
#mvn clean verify
#cd $TOP

end=$(date +"%s")
tdiff=$(($end-$start))

printbar
if [ $FAILED -eq 0 ]; then
    printf "\nBuild successful\n\n"
else
    printf "\nBuild failed\n\n"
fi
printf "Time Elapsed:  %dm%ds\n" $(($tdiff/60)) $(($tdiff%60))
printbar

exit


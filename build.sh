#!/bin/bash

TOP=`pwd`
# used in settings.xml
export CSS_TOP=$TOP

start=$(date +"%s")
cols=`tput cols`
ALL=0
LOCAL=0
REBUILD=0
CLEAN_ONLY=0
FAILED=0
OFFLINE=0
CLIENT=0
NOPLUGINUPDATES=0
NOSNAPSHOTUPDATES=0

function usage {
    echo "Usage: $0 [-a | --all] [-l | --local] [-r | --rebuild] [-c | --clean]"
    echo ""
    echo "  -a: use all modules, not just clas12"
    echo "  -l: use local, build from source (default: remote, downloads everything)"
    echo "  -r: use 'mvn clean verify' to rebuild"
    echo "  -c: use 'mvn clean' only"
    echo "  -o: offline mode"
    echo "  -C: client mode"
    echo "  -p: no plugin updates mode"
    echo "  -s: no snapshot updates mode"
    echo ""
    echo "Defaults will run the build on the CLAS12 module only.  Rebuilding all"
    echo "modules can take up to 1hr."
    echo ""
}

function printbar {
    printf "=%.0s" $(seq 1 $cols)
}

while [ "$#" -gt 0 ]; do
    case "$1" in
        -a) ALL=1; shift 1;;
        -l) LOCAL=1; shift 1;;
        -r) REBUILD=1; shift 1;;
        -c) CLEAN_ONLY=1; shift 1;;
        -o) OFFLINE=1; shift 1;;
        -C) CLIENT=1; shift 1;;
        -p) NOPLUGINUPDATES=1; shift 1;;
        -s) NOSNAPSHOTUPDATES=1; shift 1;;
        -h) usage; exit 1;;
        --all) ALL=1; shift 1;;
        --local) LOCAL=1; shift 1;;
        --rebuild) REBUILD=1; shift 1;;
        --clean) CLEAN_ONLY=1; shift 1;;
        --offline) OFFLINE=1; shift 1;;
        --client) CLIENT=1; shift 1;;
        --nopluginupdates) NOPLUGINUPDATES=1; shift 1;;
        --nosnapshotupdates) NOSNAPSHOTUPDATES=1; shift 1;;
        --help) usage; exit 1;;
        -*) echo "Unknown option: $1" >&2; exit 1;;
    esac
done

if [ $LOCAL -eq 1 ]; then
    # order matters!!
    MODULES="cs-studio/core cs-studio/applications org.csstudio.clas12"
    MVN_SETTINGS=$TOP/settings_local.xml
    if [ -d cs-studio ]; then
        cd cs-studio
        git pull origin 4.1.x
    else 
        git clone https://github.com/controlsystemstudio/cs-studio
        cd cs-studio
        git checkout 4.1.x
    fi
    cd ..
else
    MODULES=org.csstudio.clas12
    MVN_SETTINGS=$TOP/settings_remote.xml
fi

MVN_CMD="mvn"

if [ $OFFLINE -eq 1 ]; then
    MVN_CMD="$MVN_CMD --offline"
fi

if [ $CLIENT -eq 1 ]; then
    MVN_CMD="$MVN_CMD -client"
fi

if [ $NOPLUGINUPDATES -eq 1 ]; then
    MVN_CMD="$MVN_CMD --no-plugin-updates"
fi

if [ $NOSNAPSHOTUPDATES -eq 1 ]; then
    MVN_CMD="$MVN_CMD --no-snapshot-updates"
fi

MVN_CMD="$MVN_CMD --settings $MVN_SETTINGS"

if [ $REBUILD -eq 1 ]; then
    MVN_CMD="$MVN_CMD clean verify"
elif [ $CLEAN_ONLY -eq 1 ]; then
    MVN_CMD="$MVN_CMD clean"
else
    MVN_CMD="$MVN_CMD verify"
fi

printbar
printf "\n%s\n" "Modules included: $MODULES"
printf "%s\n" "Maven cmd: $MVN_CMD"
printbar 

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

end=$(date +"%s")
tdiff=$(($end-$start))

printbar
if [ $FAILED -eq 0 ]; then
    printf "\nBuild successful\n\n"
    printf "Copying needed zip files to downloads/..."
    cd $TOP
    if [ ! -d downloads ]; then
        mkdir downloads
    fi
    cp org.csstudio.clas12/repository/target/products/alarm*zip ./downloads
    cp org.csstudio.clas12/repository/target/products/jms2rdb*zip ./downloads
    cp org.csstudio.clas12/repository/target/products/sns*zip ./downloads
    printf "Done\n"
else
    printf "\nBuild failed\n\n"
fi
printf "Time Elapsed:  %dm%ds\n" $(($tdiff/60)) $(($tdiff%60))
printbar

exit


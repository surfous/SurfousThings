#! /bin/bash

DEFAULT_CMD='default'
PROJECT_CMD='project'
TMP_CMD='tmp'
ASSEMBLE_CMD='assemble'
ARCHIVE_CMD='archive'
LIB_CMD='lib'
SCRIPT_CMD='script'

CMD=$DEFAULT_CMD
if (( "$#" > "0" )); then
	CMD=$1
fi

if [ $CMD == $PROJECT_CMD ] || [ $CMD == $DEFAULT_CMD ]; then
	SOURCE="${BASH_SOURCE[0]}"
	while [ -h "$SOURCE" ]; do # resolve $SOURCE until the file is no longer a symlink
	  DIR="$( cd -P "$( dirname "$SOURCE" )" && pwd )"
	  SOURCE="$(readlink "$SOURCE")"
	  [[ $SOURCE != /* ]] && SOURCE="$DIR/$SOURCE" # if $SOURCE was a relative symlink, we need to resolve it relative to the path where the symlink file was located
	done
	SCRIPT_DIR="$( cd -P "$( dirname "$SOURCE" )" && pwd )"
	PROJECT_PATH=`dirname $SCRIPT_DIR`
	echo $PROJECT_PATH
	exit 0
fi

if [ -z "$PROJECT_PATH" ]; then
	PROJECT_PATH=$($0)
fi

if [ $CMD == $TMP_CMD ]; then
	echo "${PROJECT_PATH}/tmp"
elif [ $CMD == $ASSEMBLE_CMD ]; then
	echo "${PROJECT_PATH}/assemble"
elif [ $CMD == $ARCHIVE_CMD ]; then
	echo "${PROJECT_PATH}/assemble/archive"
elif [ $CMD == $LIB_CMD ]; then
	echo "${PROJECT_PATH}/src/lib"
elif [ $CMD == $SCRIPT_CMD ]; then
	echo "${PROJECT_PATH}/script"
else
	echo "Error: Invalid command '$CMD'"
	exit 1
fi
exit 0

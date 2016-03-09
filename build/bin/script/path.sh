#! /bin/bash

declare -a TOP_LEVEL_DIRS=(build devicetypes smartapps)

DEFAULT_CMD='default'
PROJECT_CMD='project'
TMP_CMD='tmp'
APPS_CMD='smartapps'
DEVICES_CMD='devicetypes'
APP_ARCHIVE_CMD='smartapps_archive'
DEVICE_ARCHIVE_CMD='devicetypes_archive'
LIB_CMD='lib'
BIN_CMD='bin'
SCRIPT_CMD='script'

CMD=$DEFAULT_CMD
if (( "$#" > "0" )); then
	CMD=$1
fi

if [ -z "$PROJECT_PATH" ]; then
	SOURCE="${BASH_SOURCE[0]}"
	while [ -h "$SOURCE" ]  # resolve $SOURCE until the file is no longer a symlink
	do
	  DIR="$( cd -P "$( dirname "$SOURCE" )" && pwd )"
	  SOURCE="$(readlink "$SOURCE")"
	  [[ $SOURCE != /* ]] && SOURCE="$DIR/$SOURCE" # if $SOURCE was a relative symlink, we need to resolve it relative to the path where the symlink file was located
	done

	SCRIPT_DIR="$( cd -P "$( dirname "$SOURCE" )" && pwd )"

	PROJECT_PATH=""
	CHECK_PATH="$(dirname $SCRIPT_DIR)"
	echo "check dir path: $CHECK_PATH"

	IS_TOP=false

	while [[ "$IS_TOP" == false ]]
	do
		# Are the three subdirs that indicate that this is the project top directory here?
		((DIR_MATCH_COUNT=0))
		for tldir in "${TOP_LEVEL_DIRS[@]}"
		do
			CHECK_FOR_DIR="$CHECK_PATH/$tldir"
			if [[ -d "$CHECK_FOR_DIR" ]]
			then
				((DIR_MATCH_COUNT+=1))
			else
				# Not this level - missing one
				break
			fi
		done
		if [ ${#TOP_LEVEL_DIRS[@]} -eq $DIR_MATCH_COUNT ]
		then
			IS_TOP=true
			PROJECT_PATH=$CHECK_PATH
			break
		else
			CHECK_PATH="$(dirname $CHECK_PATH)"
		fi
	done

	if [[ $CHECK_PATH == "/" ]]
	then
		echo "Error: top level project directory not found"
		exit 1
	fi
fi

if [[ $CMD == $PROJECT_CMD || $CMD == $DEFAULT_CMD ]]; then
	echo "${PROJECT_PATH}"
elif [ $CMD == $TMP_CMD ]; then
	echo "${PROJECT_PATH}/build/tmp"
elif [ $CMD == $APPS_CMD ]; then
	echo "${PROJECT_PATH}/build/smartapps"
elif [ $CMD == $DEVICES_CMD ]; then
	echo "${PROJECT_PATH}/build/decicetypes"
elif [ $CMD == $APP_ARCHIVE_CMD ]; then
	echo "${PROJECT_PATH}/build/smartapps/archive"
elif [ $CMD == $DEVICE_ARCHIVE_CMD ]; then
	echo "${PROJECT_PATH}/build/devicetypes/archive"
elif [ $CMD == $LIB_CMD ]; then
	echo "${PROJECT_PATH}/build/src/lib"
elif [ $CMD == $BIN_CMD ]; then
	echo "${PROJECT_PATH}/build/bin"
elif [ $CMD == $SCRIPT_CMD ]; then
	echo "${PROJECT_PATH}/build/bin/script"
else
	echo "Error: Invalid command '$CMD'"
	exit 1
fi
exit 0

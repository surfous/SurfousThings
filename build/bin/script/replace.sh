#!/bin/bash

if [ -z "$SCRIPT_PATH" ]; then
	SCRIPT_PATH=`dirname "$0"`
fi
source "$SCRIPT_PATH/commons.sh"
SCRIPTNAME=$(basename "$0")

# getopts defaults
OPT_lIST_INSTEAD=false # list targets
OPT_REGION='' # region id

#Help function
function USAGE {
  echo -e "${REV}Usage:${NORM} ${BOLD}$SCRIPTNAME [-l] [-r REGION] TARGETPATH ADDIN SNIPPETPATH${NORM}"\\n
  echo "Parameters:"
  echo " TARGETPATH  --Path of the file to replace or list targets in"
  echo " ADDIN       --name of the add-in target. Incidentally matches the lib file the add-in region is from"
  echo " SNIPPETPATH --Path of the file containing just the replacement text"
  echo "Options:"
  echo    " ${REV}-r REGION${NORM}  --If the add-in file contains more than one region, this option specifies which named region"
  echo    "                           Omitting this option will perform the replacement for the main region"
  echo    " ${REV}-l${NORM}         --Lists the targets found in source ${BOLD}FILEPATH${NORM}. Ignores ${BOLD}-r${NORM} if specified"
  echo -e " ${REV}-h${NORM}         --Displays this help message. No further functions are performed."\\n
}

#Check the number of arguments. If none are passed, print help and exit.
NUMARGS=$#
if [ $NUMARGS -eq 0 ]; then
  USAGE
  exit 1
fi

### Start getopts code ###

while getopts :lr:h FLAG; do
  case $FLAG in
    l)  #set option "a"
      OPT_lIST_INSTEAD=true
      ;;
    r)  #set option "b"
      OPT_REGION=$OPTARG
      ;;
    h)  #show help
      USAGE
	  exit 0
      ;;
    \?) #unrecognized option - show help
      echo -e \\n"Option -${BOLD}$OPTARG${NORM} not allowed."
      #If you just want to display a simple error message instead of the full
      #help, remove the 2 lines above and uncomment the 2 lines below.
      echo -e "Use ${BOLD}$SCRIPT -h${NORM} to see the help documentation."\\n
      exit 2
      ;;
  esac
done

shift $((OPTIND-1))  #This tells getopts to move on to the next argument.

### End getopts code ###

# Functions

replace_target() {
	REGION_ID_STR=''
	if [ -n "$OPT_REGION" ]; then
		REGION_ID_STR=" $OPT_REGION"
	fi

	ADDIN_TARGET_PATTERN="^//\s*ADDIN TARGET +$ADDIN"
	if [ -n "$OPT_REGION" ]; then
		ADDIN_TARGET_PATTERN="$ADDIN_TARGET_PATTERN $OPT_REGION"
	fi

	$SEDCMD -r -e "\|$ADDIN_TARGET_PATTERN| {" -e "r $SNIPPETPATH" -e 'd' -e '}' -i $TARGETPATH
	if [ $? -ne 0 ]; then
		errorexit 40 "sed extract failed" false
	fi
}

list_targets() {
	ADDIN_TARGET_PATTERN="^//\s*ADDIN TARGET\s+(\w+)\s*(\w*)?.*$"
	TARGET_LIST=$($SEDCMD -nr "s|$ADDIN_TARGET_PATTERN|\1\t\2|p" $TARGETPATH)
	if [ $? -ne 0 ]; then
		errorexit 41 "target enumeration failed" false
	fi
	echo "$TARGET_LIST"
}

# Main

# get positional parameters
TARGETPATH="$1"
ADDIN="$2"
SNIPPETPATH="$3"

if [ ! -e "$TARGETPATH" ] || [ ! -f "$TARGETPATH" ] || [ ! -r "$TARGETPATH" ] || [ ! -w "$TARGETPATH" ]; then
	errorexit 10 "problem accessing '$TARGETPATH'. Specified file must exist, be a regular file, and be readable and writable."
fi

if [ -n "$SNIPPETPATH" ] && ( [ ! -e "$SNIPPETPATH" ] || [ ! -f "$SNIPPETPATH" ] || [ ! -r "$SNIPPETPATH" ] ); then
	errorexit 10 "problem accessing '$SNIPPETPATH'. Specified file must exist, be a regular file, and be readable."
fi

if [ "$OPT_lIST_INSTEAD" == true ]; then
	list_targets
else
	replace_target
fi
exit 0

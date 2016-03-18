#! /bin/bash

if [ -z "$SCRIPT_PATH" ]; then
	SCRIPT_PATH=`dirname "$0"`
fi
source "$SCRIPT_PATH/commons.sh"
SCRIPTNAME=$(basename "$0")

# capture args
CMD=$1
TARGET_PATH=$2

# Patterns
BUILDDATE_TOKEN='\{\{BUILDDATE\}\}'
BUILDDATE_LINE_PATTERN="^.*?Date:"

BUILDTAG_TOKEN='\{\{BUILDTAG\}\}'
BUILDTAG_LINE_PATTERN="^.*?Build:"
BUILDTAG_EXTRACT_TAG_PATTERN="[0-9]{8}-[0-9]{6}\.[0-9]+"
BUILDTAG_EXTRACT_LINE_PATTERN="^.*?Build:\s+($BUILDTAG_EXTRACT_TAG_PATTERN).*$"

# getopts defaults
OPT_GET_TAG=false # read the buildtag from a built artifact rather than writing it

USAGE() {
	echo "${BOLD}$SCRIPTNAME${NORM}"
	echo "${REV}Usage${NORM} ${BOLD}$SCRIPTNAME [-g] PATH${NORM}"
	echo " Generates a build tag and replace {{BUILDTAG}} if preceded by 'Build:' in PATH "
	echo "${REV}Options${NORM}"
	echo " ${BOLD}-g${NORM}                      get the build tag from an already-built artifact at PATH"
	echo "${REV}Examples${NORM}"
	echo " ${BOLD}buildtag.sh PATH${NORM}        Replaces build date and tag tokens from the target file at PATH"
	echo " ${BOLD}buildtag.sh -g PATH${NORM}     Returns the build tag from the already-built artifact at PATH"
	echo ""
}

#Check the number of arguments. If none are passed, print help and exit.
NUMARGS=$#
if [ $NUMARGS -eq 0 ]; then
  USAGE
  exit 1
fi

### Start getopts code ###

#Parse command line flags
#If an option should be followed by an argument, it should be followed by a ":".
#Notice there is no ":" after "h". The leading ":" suppresses error messages from
#getopts. This is required to get my unrecognized option code to work.

while getopts :gh FLAG; do
  case $FLAG in
    g) # get tag from artifact
      OPT_GET_TAG=true
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

# populates BUILDTAG with YYYYmmdd-HHMMSS-PPID
generate() {
	BUILDDATE=`date +%F`
	DATETIME=`TZ=UTC date +%Y%m%d-%H%M%S`
	BUILDTAG="$DATETIME.$PPID"
}


checkargs() {
	if [ -z "$TARGET_PATH" ]; then
		errorexit 10 "No file path specified"
	fi

	if ! [ -e "$TARGET_PATH" ]; then
		errorexit 11 "file '$TARGET_PATH' does not exist"
	fi

	if [ ! -f "$TARGET_PATH" ]; then
		errorexit 12 "file '$TARGET_PATH' is not a regular file"
	fi

	if [ ! -r "$TARGET_PATH" ]; then
		errorexit 13  "no read permission to '$TARGET_PATH'"
	fi

	if [ "$OPT_GET_TAG" == "false" ] && [ ! -r "$TARGET_PATH" ]; then
		errorexit 14  "no write permission to '$TARGET_PATH' for $TAG_CMD"
	fi
}

tag() {
	generate
	$SEDCMD -r -i.bak "/$BUILDDATE_LINE_PATTERN/s/$BUILDDATE_TOKEN/$BUILDDATE/" $TARGET_PATH
	$SEDCMD -r -i "/$BUILDTAG_LINE_PATTERN/s/$BUILDTAG_TOKEN/$BUILDTAG/" $TARGET_PATH
}

gettag() {
	BUILDTAG=$($SEDCMD -n -r "s|$BUILDTAG_EXTRACT_LINE_PATTERN|\1|p" $TARGET_PATH)
	if [ -z "$BUILDTAG" ]; then
		errorexit 30 "no build tag found in '$TARGET_PATH'"
	fi
	echo $BUILDTAG
}

checkargs

if [ "$OPT_GET_TAG" == "true" ]; then
	gettag
else
	tag
fi
exit 0

#! /bin/bash
###
# extract.sh FILEPATH [REGION ID]
# Copies tagged region from FILEPATH and returns on stdout
# Supply optional REGION ID to copy different regions from same file, if any.
# No REGION ID references the main region
###
if [ -z "$SCRIPT_PATH" ]; then
	SCRIPT_PATH=`dirname "$0"`
fi
source "$SCRIPT_PATH/commons.sh"
SCRIPTNAME=$(basename "$0")

# getopts defaults
OPT_lIST_INSTEAD=false # list
OPT_REGION='' # region id

#Help function
function USAGE {
  echo -e "${REV}Usage:${NORM} ${BOLD}$SCRIPTNAME [-l] [-r REGION] FILEPATH${NORM}"\\n
  echo "${BOLD}Parameters${NORM}"
  echo " FILEPATH  --Path of the origin file to extract from"
  echo "${BOLD}Options${NORM}"
  echo    " ${REV}-l${NORM}         --Lists the regions found in ${BOLD}FILEPATH${NORM}. Ignores ${BOLD}-r${NORM} if specified"
  echo    " ${REV}-r REGION${NORM}  --Specifies a optional named region to extract. No value is the main region"
  echo -e " ${REV}-h${NORM}         --Displays this help message. No further functions are performed."\\n
  echo -e "${BOLD}Examples${NORM}"
  echo -e " ${BOLD}$SCRIPTNAME src/lib/smartlib.groovy ${NORM}"\\n
  echo -e " ${BOLD}$SCRIPTNAME -l src/lib/smartlib.groovy ${NORM}"\\n
  echo -e " ${BOLD}$SCRIPTNAME -r tile src/lib/smartlib.groovy ${NORM}"\\n
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


extract_region() {
	REGION_ID_STR=''
	if [ -n "$OPT_REGION" ]; then
		REGION_ID_STR=" $OPT_REGION"
	fi

	# // ORIGIN REGION BEGIN <optional region id>
	BEGIN_REGION_PATTERN="^//\s+ORIGIN REGION BEGIN$REGION_ID_STR\s*$"
	# // ORIGIN REGION END <optional region id>
	END_REGION_PATTERN="^//\s+ORIGIN REGION END$REGION_ID_STR\s*$"
	REGION_BODY=$($SEDCMD -nr "\|$BEGIN_REGION_PATTERN|{:a;n;\|$END_REGION_PATTERN|q;p;ba}" $FILEPATH)
	if [ $? -ne 0 ]; then
		errorexit 40 "sed extract failed" false
	fi
	echo "$REGION_BODY"
}

list_regions() {
	BEGIN_REGION_PATTERN="^.*?ORIGIN REGION BEGIN\s*(\w*)"
	REGION_LIST=$($SEDCMD -nr "s/$BEGIN_REGION_PATTERN/\1/p" $FILEPATH | $SORTCMD | $UNIQCMD)
	if [ $? -ne 0 ]; then
		errorexit 41 "region enumeration failed" false
	fi
	echo "$REGION_LIST"
}


# Main

# get positional parameters
FILEPATH="$1"

if [ ! -e "$FILEPATH" ] || [ ! -f "$FILEPATH" ] || [ ! -r "$FILEPATH" ]; then
	errorexit 10 "problem accessing '$FILEPATH'. Specified file must exist, be a regular file, and be readable"
fi

if [ "$OPT_lIST_INSTEAD" == true ]; then
	list_regions
else
	extract_region
fi
exit 0

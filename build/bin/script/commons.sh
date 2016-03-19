# no shebang, This is for sourcing only

# other unix commands used in this script
SEDCMD=$(which gsed)
GREPCMD=$(which grep)
SORTCMD=$(which sort)
UNIQCMD=$(which uniq)
CUTCMD=$(which cut)
MKTEMPCMD=$(which mktemp)
MKDIRCMD=$(which mkdir)
COMPRESSCMD=$(which zip)

# Set font faces for Help.
NORM=`tput sgr0`
BOLD=`tput bold`
REV=`tput smso`

showUsageMsg() {
	echo "${BOLD}No usage defined.${NORM} Please define a usage method in the sourcing script"
}

errorexit() {
	STATUSCODE=$1
	ERRORMSG=$2
	showUsageMsg=$3

	if [ -z "$STATUSCODE" ]; then
		STATUSCODE=125
	fi

	if [ -z "$showUsageMsg" ]; then
		showUsageMsg=true
	fi

	if [ -n "$ERRORMSG" ]; then
		>&2 echo "${BOLD}FATAL ERROR (${STATUSCODE})${NORM}: $ERRORMSG"
	fi
	if [ "$showUsageMsg" == true ]; then
		>&2 showUsageMsg
	fi

	exit $STATUSCODE
}

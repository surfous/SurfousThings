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

#Set fonts for Help.
NORM=`tput sgr0`
BOLD=`tput bold`
REV=`tput smso`

USAGE() {
	echo "No usage defined. Please define a usage method in the sourcing script"
}

errorexit() {
	STATUSCODE=$1
	ERRORMSG=$2
	SHOWUSAGE=$3

	if [ -z "$STATUSCODE" ]; then
		STATUSCODE=125
	fi

	if [ -z "$SHOWUSAGE" ]; then
		SHOWUSAGE=true
	fi

	if [ -n "$ERRORMSG" ]; then
		>&2 echo "${BOLD}FATAL ERROR${NORM}: $ERRORMSG"
	fi
	if [ "$SHOWUSAGE" == true ]; then
		>&2 USAGE
	fi

	exit $STATUSCODE
}

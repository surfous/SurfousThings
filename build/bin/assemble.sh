#!/bin/bash

set -e

SCRIPT_PATH="$(dirname $0)"
SCRIPTNAME=$(basename "$0")

export PROJECT_PATH="$($SCRIPT_PATH/path.sh)"

SCRIPT_PATH="$($SCRIPT_PATH/path.sh script)"

TMP_PATH="$($SCRIPT_PATH/path.sh tmp)"
LIB_PATH="$($SCRIPT_PATH/path.sh lib)"
source "$SCRIPT_PATH/commons.sh"

# getopts defaults
OPT_DEBUG=false # debug mode

USAGE() {
	echo "$SCRIPTNAME"
	echo "${REV}Usage${BOLD} $SCRIPTNAME [-d] TARGET_SOURCE_PATH${NORM}"
	echo "${REV}Parameters${NORM}"
	echo " ${BOLD}TARGET_SOURCE_PATH${NORM}        the code file to be copied and assembled"
	echo "${REV}Options${NORM}"
	echo " ${BOLD}-d${NORM}                        Sets debug mode (don't clear away temp build dir & files)"
	echo "\n"
}

#Check the number of arguments. If none are passed, print help and exit.
NUMARGS=$#
if [ $NUMARGS -eq 0 ]; then
	errorexit 1 "no parameters provided" True
fi

### Start getopts code ###

#Parse command line flags
#If an option should be followed by an argument, it should be followed by a ":".
#Notice there is no ":" after "h". The leading ":" suppresses error messages from
#getopts. This is required to get my unrecognized option code to work.

while getopts :dh FLAG; do
  case $FLAG in
    d)  #set option "d"
      OPT_DEBUG=true
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
#
# parse arguments
TARGET_SOURCE_PATH="$1"
if [ ! -e "$TARGET_SOURCE_PATH" ] || [ ! -f "$TARGET_SOURCE_PATH" ] || [ ! -r "$TARGET_SOURCE_PATH" ]; then
	errorexit 10 "problem accessing '$TARGET_SOURCE_PATH'. Target source must exist, be a regular file, and be readable"
fi
ARTIFACT_FILE="$(basename $TARGET_SOURCE_PATH)"
ARTIFACT_EXTENSION="${ARTIFACT_FILE##*.}"
ARTIFACT_NAME="${ARTIFACT_FILE%.*}"

# setup temp dir for the build
BUILDTMP=$($MKTEMPCMD -d "$TMP_PATH/assembly-${ARTIFACT_NAME}-XXXXX")
if [ $? -ne 0 ]; then
	errorexit 13 "could not create build temp directory" false
fi

# this is where we will assemble the final product
TMP_BUILD_PATH="$BUILDTMP/$ARTIFACT_FILE"

# copy target source file to assemble directory
cp $TARGET_SOURCE_PATH $TMP_BUILD_PATH

# tag build with datetime+pid: 20151031-121212.000
$SCRIPT_PATH/buildtag.sh tag $TMP_BUILD_PATH

# find the targets
TARGET_LIST="$($SCRIPT_PATH/replace.sh -l $TMP_BUILD_PATH)"
echo "$TARGET_LIST"

# Loop over targets
while read -r target_line; do
	read addin_source_name addin_source_region <<<$(echo $target_line)

	echo "searching for $addin_source_name in $LIB_PATH"
	# find the file under /lib with the desired region
	addin_candidates=$(find "$LIB_PATH" -type f -not -path "$LIB_PATH/classes*" -name "${addin_source_name}*" -print)
	addin_candidate_count=$(echo "$addin_candidates" | wc -l)
	# make sure there's exactly one candidate
	if [[ $addin_candidate_count -gt 1 ]]; then
		errorexit 32 "Found multiple matches for $addin_source_name under $LIB_PATH. Please rename to disambiguate." false
	elif [[ $addin_candidate_count -eq 0 ]]; then
		errorexit 31 "No matches found for $addin_source_name under $LIB_PATH" false
	fi
	addin_source_path=$addin_candidates

	if [ ! -e "$addin_source_path" ]; then
		errorexit 14 "Add-in file not found at path '$addin_source_path'"
	fi

	addin_source_file=$(basename "$addin_source_path")
	addin_source_extension="${addin_source_file##*.}"

	# build the extract and replace command strings
	region_text="main region"
	extract_cmd="$SCRIPT_PATH/extract.sh"
	replace_cmd="$SCRIPT_PATH/replace.sh"
	if [[ ${#addin_source_region} -gt 0 ]]; then
		region_text="region $addin_source_region"
		extract_cmd+=" -r $addin_source_region"
		replace_cmd+=" -r $addin_source_region"
	fi
	extract_cmd+=" $addin_source_path"
	replace_cmd+=" $TMP_BUILD_PATH $addin_source_name"

	# execute the extract command to pull out the region
	# echo "running extract: $extract_cmd"
	extracted_region="$($extract_cmd)"
	if [ $? -ne 0 ]; then
		errorexit 15 "failed to extract $region_text from $addin_path"
	fi

	# store the extracted region
	extracted_region_name="$addin_source_name-$addin_source_region"
	extracted_region_name="${extracted_region_name%-}"
	extracted_region_file="$extracted_region_name.$addin_source_extension"
	extracted_region_path=$($MKTEMPCMD "$BUILDTMP/region-${extracted_region_file}")
	echo "// REGION BEGIN - ORIGIN $addin_source_file $region_text" > "$extracted_region_path"
	echo "$extracted_region" >> "$extracted_region_path"
	echo "// REGION END - ORIGIN $addin_source_file $region_text" >> "$extracted_region_path"

	# replace the target with the extracted region in the artifact
	replace_cmd+=" $extracted_region_path"
	# echo "running replace: $replace_cmd"
	$($replace_cmd)
	if [ $? -ne 0 ]; then
		errorexit 16 "failed to replace target $addin_source_file $region_text in $TMP_BUILD_PATH"
	fi

done <<< "$TARGET_LIST"

# the artifact should be fully assembed in the build temp dir at TMP_BUILD_PATH at this point

# Move the assembled artifact to the assemble directory
ASSEMBLE_DIR="$($SCRIPT_PATH/path.sh assemble)"
OLD_ARTIFACT_DEST_DIR="$ASSEMBLE_DIR/archive"
ARTIFACT_DEST_PATH="$ASSEMBLE_DIR/$ARTIFACT_FILE"

## Check the destination for a previously built artifact for the same source and move it
if [ -e "$ARTIFACT_DEST_PATH" ]; then
	# get the buidtag from the prior artifact
	OLD_ARTIFACT_BUILDTAG="$($SCRIPT_PATH/buildtag.sh -g $ARTIFACT_DEST_PATH)"
	if [ $? -ne 0 ]; then
		errorexit 17 "buildtag.sh get on $ARTIFACT_DEST_PATH exited with error"
	fi

	# rename it using its buildtag
	$($MKDIRCMD -p $OLD_ARTIFACT_DEST_DIR)
	OLD_ARTIFACT_DEST_PATH="$OLD_ARTIFACT_DEST_DIR/$ARTIFACT_FILE.$OLD_ARTIFACT_BUILDTAG"
	mv "$ARTIFACT_DEST_PATH" "$OLD_ARTIFACT_DEST_PATH"
	if [ $? -ne 0 ]; then
		errorexit 18 "mv from $ARTIFACT_DEST_PATH to $OLD_ARTIFACT_DEST_PATH failed"
	fi
fi

## Move the artifact into place
mv "$TMP_BUILD_PATH" "$ARTIFACT_DEST_PATH"
if [ $? -ne 0 ]; then
	errorexit 18 "mv from $TMP_BUILD_PATH to $ARTIFACT_DEST_PATH failed"
fi

# clean up tmp dir
if [ "$OPT_DEBUG" = "false" ]; then
	rm -rf $BUILDTMP
	if [ $? -ne 0 ]; then
		errorexit 19 "'rm -rf $BUILDTMP' failed"
	fi
fi

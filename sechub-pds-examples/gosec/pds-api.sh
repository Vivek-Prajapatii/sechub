#!/bin/bash
# SPDX-License-Identifier: MIT

# ---------------------------------------------
# Shell front end to selected SecHub API calls
# ---------------------------------------------

# Tip: Set PDS_SERVER, PDS_USERID and PDS_APITOKEN as environment variables

function usage {
  cat - <<EOF
---
Usage: `basename $0` [-p] [-s <pds server url> [-u <pds user>] [-a <pds api token>] action [<action's parameters>]

Shell front end for the Product Delegation Server (PDS)
Output will be beautified/colorized by piping json output through jq command (https://github.com/stedolan/jq)
unless you specify -p or -plain option.

You are encouraged to set PDS_SERVER, PDS_USERID and PDS_APITOKEN as environmet variables
so you can omit setting them via options which is better, because your secrets will not be revealed in the process list.

List of actions and mandatory parameters:
ACTION [PARAMETERS] - EXPLANATION
----------------------------------
check_alive - Check if the server is running.
create_job <product-id> <sechub-job-uuid> - Create a new job using <product-id> and a <sechub-job-uuid>.
mark_job_ready_to_start <job-uuid> - Mark a job with <job-uuid> as ready to start.
job_status <job-uuid> - Get the status of a job using the <job-uuid>.

EOF
}

function check_parameter {
  param="$1"
  if [ -z "${!param}" ] ; then
    echo "$param not set"
    FAILED=1
  fi
}

function check_alive {
  curl $CURL_PARAMS -i -X HEAD "$PDS_SERVER/api/anonymous/check/alive"
}

function mark_job_ready_to_start {
  local jobUUID=$1

  curl $CURL_AUTH $CURL_PARAMS -i -X PUT --header "Accept: application/json" --header "Content-Type: application/json" "$PDS_SERVER/api/job/$jobUUID/mark-ready-to-start" | $RESULT_FILTER | $JSON_FORMATTER
}

function job_status {
  local jobUUID=$1

  curl $CURL_AUTH $CURL_PARAMS -i -X GET --header "Accept: application/json" "$PDS_SERVER/api/job/$jobUUID/status" | $RESULT_FILTER | $JSON_FORMATTER
}

function job_result {
  local jobUUID=$1

  curl $CURL_AUTH $CURL_PARAMS -i -X GET --header "Accept: application/json" "$PDS_SERVER/api/job/$jobUUID/result" | $RESULT_FILTER | $JSON_FORMATTER
}

function monitoring_status {
    curl $CURL_AUTH $CURL_PARAMS -i -X GET --header "Accept: application/json" "$PDS_SERVER/api/admin/monitoring/status" | $RESULT_FILTER | $JSON_FORMATTER
}

function create_job {
  local productId=$1
  local sechubJobUUID=$2

  curl $CURL_AUTH $CURL_PARAMS -i -X POST --header "Content-Type: application/json" \
    -d "$(generate_pds_job_data $sechubJobUUID $productId)" \
    "$PDS_SERVER/api/job/create" | $RESULT_FILTER | $JSON_FORMATTER
}

function generate_pds_job_data {
  cat <<EOF
{
  "apiVersion":"$SECHUB_API_VERSION",
  "sechubJobUUID":"$1",
  "productId":"$2"
}
EOF
}

########
# main #
########

FAILED=0

PDS_API_VERSION="1.0"
NOFORMAT_PIPE="cat -"
RESULT_FILTER="tail -1"
if which jq >/dev/null 2>&1 ; then
  JSON_FORMATTER="jq ."   # . is needed or pipeing the result is not possible
  JSON_FORMAT_SORT="jq sort"
else
  echo "### Hint: Install jq (https://github.com/stedolan/jq) to improve output." >&2  # appears only on stderr
  JSON_FORMATTER="$NOFORMAT_PIPE"
  JSON_FORMAT_SORT="$NOFORMAT_PIPE"
fi

# Parse command line options (everything starting with '-')
opt="$1"
while [[ "${opt:0:1}" == "-" ]] ; do
  case $opt in
  -a|-apitoken)
    PDS_APITOKEN="$2"
    shift 2
    ;;
  -d|-debug)
    # Plain output including header information from curl
    RESULT_FILTER="$NOFORMAT_PIPE"
    JSON_FORMATTER="$NOFORMAT_PIPE"
    JSON_FORMAT_SORT="$NOFORMAT_PIPE"
    shift
    ;;
  -p|-plain)
    JSON_FORMATTER="$NOFORMAT_PIPE"
    JSON_FORMAT_SORT="$NOFORMAT_PIPE"
    shift
    ;;
  -s|-server)
    PDS_SERVER="$2"
    shift 2
    ;;
  -u|-user)
    PDS_USERID="$2"
    shift 2
    ;;
  -v|-verbose)
    echo "### Connecting as $PDS_USERID to $PDS_SERVER"
    shift
    ;;
  *)
    echo "Unknown option: \"$opt\""
    usage
    exit 1
    ;;
  esac
  opt="$1"
done

# Check if mandatory parameters are defined
for parameter in PDS_SERVER PDS_USERID PDS_APITOKEN ; do
  check_parameter "$parameter"
done

AUTH="$PDS_USERID:$PDS_APITOKEN"
CURL_AUTH="-u $AUTH"
CURL_PARAMS="--silent --insecure --show-error"

action="$1" && shift
case "$action" in
  check_alive)
    check_alive
    ;;
  create_job)
    PDS_PRODUCT_ID="$1" ; check_parameter PDS_PRODUCT_ID
    SECHUB_JOB_UUID="$2" ; check_parameter SECHUB_JOB_UUID
    [ $FAILED == 0 ] && create_job "$PDS_PRODUCT_ID" "$SECHUB_JOB_UUID" 
    ;;
  mark_job_ready_to_start)
    JOB_UUID="$1"   ; check_parameter JOB_UUID
    [ $FAILED == 0 ] && mark_job_ready_to_start "$JOB_UUID"
    ;;
  job_status)
    JOB_UUID="$1"   ; check_parameter JOB_UUID
    [ $FAILED == 0 ] && job_status "$JOB_UUID"
    ;;
  job_result)
    JOB_UUID="$1"   ; check_parameter JOB_UUID
    [ $FAILED == 0 ] && job_result "$JOB_UUID"
    ;;
  monitoring_status)
    [ $FAILED == 0 ] && monitoring_status
    ;;
  "")
    usage
    ;;
  *)
  echo "Unknown action: \"$action\""
  usage
  exit 1
  ;;
esac

# add missing newline if not formatted
[ "$JSON_FORMATTER" == "$NOFORMAT_PIPE" ] && echo ""

# failed?
if [ $FAILED != 0 ] ; then
  usage
  exit 1
fi

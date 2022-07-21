function json_is_element_in_array() {
    local element="$1"
    local elements="$2"

    is_element_in_elements=$(echo "$elements" | jq --exit-status --arg element "$element" '. as $elements | $element | IN($elements[])')

    echo "$is_element_in_elements"
}

function usage() {
    local script_name="$1"
    local parameters="$2"

    echo "`basename $script_name` $parameters"
    echo ""
    
    cat <<'USAGE'
# Please set the environment variables:

export SECHUB_SERVER=https://<server>:<port>
export SECHUB_USERID=<username>
export SECHUB_APITOKEN=<password>

# Example:

export SECHUB_SERVER=https://localhost:8443
export SECHUB_USERID=admin
export SECHUB_APITOKEN='myTop$ecret!'
USAGE
}

function print_error_message() {
    local message="$1"

    printf "[ERROR] $message\n"
}
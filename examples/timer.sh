set -eu

TO=0.300
DUR_S=30
TIME=300

# ! Requires SYSTEM_ALERT_WINDOW permission
#   to start in the background

# UP 300ms = start timer for 300s


on_up() {
    read -t $TO key && {
        volume music up
        return
    }
    vibrate $DUR_S
    set-timer $TIME
}


loop() {
    while read key; do
        wakelock acquire 5000
        
        case "$key" in
            "$PRESS_UP" )
                on_up
            ;;
            "$PRESS_DOWN" )
                volume music down
            ;;
        esac
        wakelock release
    done
}


set-timer() {
    MSG=$(date +%H:%M:%S)
    intent -a 'android.intent.action.SET_TIMER' \
        -t 'activity' \
        -e "android.intent.extra.alarm.MESSAGE:${MSG}" \
        -e "android.intent.extra.alarm.LENGTH:${1}" \
        -e "android.intent.extra.alarm.SKIP_UI:true"
}

###

DELIM="|+|"

vibrate() {
    # <duration_ms>
    echo "vibrate $*";
}
wakelock() {
    # acquire|release [timeout_ms]
    echo "wakelock $*";
}
volume() {
    # music|notification|ring|call up|down|<level>
    echo "volume $*";
}
intent() {
    # -a <action>
    # -t service|broadcast|activity
    # -p <package/.Component>
    # -d <data> -m <mimetype> -c <category>
    # -e <extra_key>:<extra_value>
    # -e <extra_key>:{:<delimiter>:<ex_val1><delimiter><ex_val2>}
    ARGS=""
    for arg in "$@"; do
        ARGS="${ARGS}${DELIM}${arg}"
    done
    echo ":${DELIM}:intent${ARGS}";
}

loop

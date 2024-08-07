set -eu

TO=0.300
DUR_S=30
TIME=300

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
    permission BACKGROUND_ACTIVITY
    
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
    echo "vibrate $*";
}
wakelock() {
    echo "wakelock $*";
}
volume() {
    echo "volume $*";
}
intent() {
    ARGS=""
    for arg in "$@"; do
        ARGS="${ARGS}${DELIM}${arg}"
    done
    echo ":${DELIM}:intent${ARGS}";
}
permission() {
    echo "permission $*";
}


loop

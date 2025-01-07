set -eu

TO=0.300
DUR_S=30
TIME=300
WL_MAX=5000

# UP 300ms = start timer for 300s


on_up() {
    read -t $TO key && {
        cmd volume music up
        return
    }
    cmd vibrate $DUR_S
    set-timer $TIME
}


loop() {
    cmd permission BACKGROUND_ACTIVITY
    
    while read key; do
        cmd wakelock acquire $WL_MAX

        case "$key" in
            "$PRESS_UP" )
                on_up
            ;;
            "$PRESS_DOWN" )
                cmd volume music down
            ;;
        esac
        cmd wakelock release
    done
}


set-timer() {
    MSG=$(date +%H:%M:%S)
    cmd intent -a 'android.intent.action.SET_TIMER' \
        -t 'activity' \
        -e "android.intent.extra.alarm.MESSAGE:${MSG}" \
        -e "android.intent.extra.alarm.LENGTH:${1}" \
        -e "android.intent.extra.alarm.SKIP_UI:true"
}


encode_list() {
    ENCODED=""; for arg in "$@"; do
        ENCODED="${ENCODED}${#arg}:${arg}"
    done
    ENCODED="${#ENCODED}:$ENCODED"
}
cmd() {
    encode_list "$@"; 
    echo "$ENCODED"
}

loop

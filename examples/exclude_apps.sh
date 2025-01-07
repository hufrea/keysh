set -eu

# Up 300ms  -> next track
# Down 30ms -> prev track

TO=0.300
DUR_S=25

WORK=1
EXCLUDE_APPS="com.android.camera com.android.camera2"


on_up() {
    read_key -t $TO && {
        cmd volume current up
        return
    }
    cmd media next
    cmd vibrate $DUR_S
}


on_down() {
    read_key -t $TO && {
        cmd volume current down
        return
    }
    cmd media previous
    cmd vibrate $DUR_S
}


read_key() {
    while read $@ key; do
        case "$key" in
        "app:"* )
            on_app "$key" ;;
        * )
            return 0 ;;
        esac
    done
    return 1
}

    
on_app() {
    data="${1#*:}"
    pkg="${data%%:*}"
    
    case "$EXCLUDE_APPS " in 
    *"$pkg "* )
        [ "$WORK" = "1" ] && self PAUSE
        WORK=0 ;;
    * )
        [ "$WORK" = "0" ] && self RESUME
        WORK=1 ;;
    esac
}


loop() {
    while read_key; do
    case "$key" in
        "$PRESS_UP" )
            on_up
        ;;
        "$PRESS_DOWN" )
            on_down
        ;;
    esac
    done
}


self() {
    cmd intent -a "${PACKAGE_NAME}.${1}" -t 'broadcast'
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


cmd permission ACCESSIBILITY

self RECV_APP_SWITCH

loop

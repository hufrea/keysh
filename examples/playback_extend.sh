set -eu

STO=0.250
DUR_S=25
DUR_L=50

WL_MAX=3000
IDLE_TO=1.5

# Up 250ms -> Up        = next track
# Up 250ms -> Down      = prev track

# Dw 250ms -> Down      = play/pause
# Dw 250ms -> Up        = (loop)
#                      -> Up        = rewind
#                      -> Down      = forward
#                      -> Dw 250ms  = break


on_forward() {
    while read key; do
        case "$key" in
        "$PRESS_DOWN" )
            read -t $STO key || {
                break
            }
            cmd media forward
        ;;
        "$PRESS_UP" )
            cmd media rewind
        ;;
        esac
    done
    cmd vibrate $DUR_L
}


on_up() {
    read -t $STO key && {
        cmd volume music up
        return
    }
    cmd vibrate $DUR_S
    read key
    
    read -t $IDLE_TO key || {
        cmd vibrate $DUR_L
        return 
    }
    case "$key" in
        "$PRESS_UP" )
            cmd media next
        ;;
        "$PRESS_DOWN" )
            cmd media previous
        ;;
    esac
}


on_down() {
    read -t $STO key && {
        cmd volume music down
        return
    }
    cmd vibrate $DUR_S
    read key

    read -t $IDLE_TO key || {
        cmd vibrate $DUR_L
        return 
    }
    case "$key" in
        "$PRESS_DOWN" )
            cmd media play_pause
        ;;
        "$PRESS_UP" )
            on_forward
        ;;
    esac
}


on_key() {
    case "$1" in
        "$PRESS_UP" )
            on_up ;;
        "$RELEASE_UP" )
            ;;
        "$PRESS_DOWN" )
            on_down ;;
        "$RELEASE_DOWN" )
            ;;
    esac
}


loop() {
    while read key; do
        cmd wakelock acquire $WL_MAX
        on_key "$key"

        while read -t $IDLE_TO key; do # keep wakelock
            on_key "$key"
        done
        cmd wakelock release
    done
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

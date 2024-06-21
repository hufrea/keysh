set -eu

STO=0.250

# Up 250ms -> Up        = next track
# Up 250ms -> Down      = prev track

# Dw 250ms -> Down      = play/pause
# Dw 250ms -> Up        = (loop)
#                      -> Up        = rewind
#                      -> Down      = forward
#                      -> Dw 250ms  = break

DUR_S=25
DUR_L=50

WL_MAX=3000
IDLE_TO=1.5


on_forward() {
    while read key; do
        case "$key" in
        "$PRESS_DOWN" )
            read -t $STO key || {
                break
            }
            media forward
        ;;
        "$PRESS_UP" )
            media rewind
        ;;
        esac
    done
    vibrate $DUR_L
}


on_up() {
    read -t $STO key && {
        volume music up
        return
    }
    vibrate $DUR_S
    read key
    
    read -t $IDLE_TO key || {
        vibrate $DUR_L
        return 
    }
    case "$key" in
        "$PRESS_UP" )
            media next
        ;;
        "$PRESS_DOWN" )
            media previous
        ;;
    esac
}


on_down() {
    read -t $STO key && {
        volume music down
        return
    }
    vibrate $DUR_S
    read key

    read -t $IDLE_TO key || {
        vibrate $DUR_L
        return 
    }
    case "$key" in
        "$PRESS_DOWN" )
            media play_pause
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
        wakelock acquire $WL_MAX
        on_key "$key"

        while read -t $IDLE_TO key; do # keep wakelock
            on_key "$key"
        done

        wakelock release
    done
}

#####

vibrate() {
    # <duration_ms>
    echo "vibrate $*";
}
wakelock() {
    # acquire|release [timeout_ms]
    echo "wakelock $*";
}
media() {
    # play_pause|next|previous|rewind|forward|stop
    echo "media $*";
}
volume() {
    # music|notification|ring|call up|down|<level>
    echo "volume $*";
}

loop

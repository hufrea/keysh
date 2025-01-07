set -eu

STO=250
LTO=500

# Up 250ms -> Up        = next track
# Up 250ms -> Down      = prev track

# Dw 250ms -> Down      = play/pause
# Dw 250ms -> Up        = (loop)
#                      -> Up        = rewind
#                      -> Down      = forward
#                      -> Dw 250ms  = break

# Up 500ms -> (loop 5s)
#             Up        = min += 5
#             Down      = min += 1 (-1 if before 5)
#             Up 250ms  = min *= 2
#             Dw 250ms  = start timer and break

# Dw 500ms = torch on
#            -> Up/Dw   = torch Off

DUR_S=25
DUR_L=50
TIMER_TO=5
TORCH_OFF_TO=300

WL_MAX=100000
IDLE_TO=1.5

LTO=0.$((LTO - STO))
STO=0.$STO


on_timer() {
    cmd vibrate $DUR_S
    V=0; I=0

    while read -t $TIMER_TO key; do
        case "$key" in
        "$PRESS_UP" )
            cmd vibrate $DUR_S
            read -t $STO key || {
                cmd vibrate $DUR_S
                V=$((V * 2))
                continue
            }
            V=$((V + 5 - I))
            I=0
        ;;
        "$PRESS_DOWN" )
            cmd vibrate $DUR_S
            read -t $STO key || {
                break
            }
            I=$((I + 1))
        ;;
        esac
    done
    set-timer $(((V + I) * 60))
    cmd vibrate $DUR_L
}


on_flashlight() {
    cmd vibrate $DUR_S
    cmd torch on
    while read -t $TORCH_OFF_TO key; do
        [ "$key" = "$RELEASE_DOWN" ] || break;
    done
    cmd torch off
}


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

    read -t $LTO key || {
        on_timer
        return
    }

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

    read -t $LTO key || {
        on_flashlight
        return
    }

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
        # "") ;;
        *)
            cmd vibrate 500
            cmd notify -c "Unknown-code: $1" ;;
    esac
}


loop() {
    cmd permission BACKGROUND_ACTIVITY

    while read key; do
        cmd wakelock acquire $WL_MAX
        on_key "$key"

        while read -t $IDLE_TO key; do # keep wakelock
            on_key "$key"
        done

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

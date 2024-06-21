#!/system/bin/sh

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

# ! Requires SYSTEM_ALERT_WINDOW permission
# Up 500ms -> (loop 5s)
#             Up        = min += 5
#             Down      = min += 1 (-1 if before 5)
#             Up 250ms  = min *= 2
#             Dw 250ms  = start timer and break

# Dw 500ms = torch on
#            -> Up/Dw   = torch Off

DUR_S=25
DUR_L=50

WL_MAX=100000
IDLE_TO=1.5

LTO=0.$((LTO - STO))
STO=0.$STO


on_timer() {
    vibrate $DUR_S
    V=0; I=0

    while read -t 5 key; do
        case "$key" in
        "$PRESS_UP" )
            vibrate $DUR_S
            read -t $STO key || {
                vibrate $DUR_S
                V=$((V * 2))
                continue
            }
            V=$((V + 5 - I))
            I=0
        ;;
        "$PRESS_DOWN" )
            vibrate $DUR_S
            read -t $STO key || {
                break
            }
            I=$((I + 1))
        ;;
        esac
    done
    set-timer $(((V + I) * 60))
    vibrate $DUR_L
}


on_flashlight() {
    vibrate $DUR_S
    torch on
    while read -t 300 key; do
        [ "$key" = "$RELEASE_DOWN" ] || break;
    done
    torch off
}


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

    read -t $LTO key || {
        on_timer
        return
    }

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

    read -t $LTO key || {
        on_flashlight
        return
    }

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
        # "") ;;
        *)
            vibrate 500
            notify -c "Unknown-code: $1" ;;
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


set-timer() {
    MSG=$(date +%H:%M:%S)
    intent -a 'android.intent.action.SET_TIMER' \
        -t 'activity' \
        -e "android.intent.extra.alarm.MESSAGE:${MSG}" \
        -e "android.intent.extra.alarm.LENGTH:${1}" \
        -e "android.intent.extra.alarm.SKIP_UI:true"
}


#####

DELIM="|+|"

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
torch() {
    # on|off
    echo "torch $*";
}
notify() {
    # -c <content> -t <title>
    # -i <id> -l <timeout_ms>
    ARGS=""
    for arg in "$@"; do
        ARGS="${ARGS}${DELIM}${arg}"
    done
    echo ":${DELIM}:notify${ARGS}";
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


echo "stop_access"
loop

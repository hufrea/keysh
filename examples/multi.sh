set -eu

STO=0.250
LTO=0.350 # 0.600 - 0.250

# Up 250ms -> Up        = next track
# Up 250ms -> Down      = prev track

# Dw 250ms -> Down      = play/pause
# Dw 250ms -> Up        = (loop)
#                      -> Up        = rewind
#                      -> Down      = forward
#                      -> Up 250ms  = play/pause
#                      -> Dw 250ms  = break

# Up 600ms -> (loop 5s)
#             Up        = min += 5
#             Down      = min += 1 (-1 if before 5)
#             Up 250ms  = min *= 2
#             Dw 250ms  = start timer and break

# Dw 600ms = torch on
#            -> Up/Dw   = torch Off

DUR_S=25
DUR_L=50
TIMER_TO=5
TORCH_OFF_TO=300
MAIN_TO=86400
IDLE_TO=1.5


on_timer() {
    cmd vibrate $DUR_S
    V=0; I=0
    
    while c_read $TO; do
        case "$C" in
        "d"|"u")
            cmd vibrate $DUR_S
            TO=$STO continue ;;
        "d-" )
            I=$((I + 1)) ;;
        "u-" )
            V=$((V + 5 - I))
            I=0 ;;
        "u=" )
            cmd vibrate $DUR_S 
            V=$(((V + I) * 2))
            I=0 ;;
        "d="|"=" )
            break ;;
        esac
        C="" TO=$TIMER_TO
    done
    set-timer $(((V + I) * 60))
    cmd vibrate $DUR_L
}


on_flashlight() {
    cmd vibrate $DUR_S
    cmd torch on
    C=""
    while c_read $TORCH_OFF_TO; do
        [ "$C" != "-" ] && break
    done
    cmd torch off
}


on_forward() {
    while c_read $TO; do
        case "$C" in
        "d"|"u" )
            TO="$STO" continue ;;
        "d-" )
            cmd media forward ;;
        "u-" )
            cmd media rewind ;;
        "u=" )
            cmd vibrate $DUR_S
            cmd media play_pause ;;
        "d=" )
            break ;;
        esac
        C="" TO=$MAIN_TO
    done
    cmd vibrate $DUR_L
}


loop() {
    cmd permission BACKGROUND_ACTIVITY
    
    C=""
    TO=$MAIN_TO
    while c_read $TO; do  
        case "$C" in 
        "d"|"u" )
            TO=$STO continue 
        ;;
        "d-" )
            cmd volume current down ;;
        "u-" )
            cmd volume current up 
        ;;
        "d="|"u=" )
            cmd vibrate $DUR_S
            TO=$LTO continue
        ;;
        "d==" )
            on_flashlight ;;
        "u==" )
            on_timer
        ;;
        "d=-"|"u=-" )
            TO=$IDLE_TO continue 
        ;;
        "d=-u" )
            on_forward ;;
        "d=-d" )
            cmd media play_pause 
        ;;
        "u=-u" )
            cmd media next ;;
        "u=-d" )
            cmd media previous 
        ;;
        "d=-="|"u=-=" )
            cmd vibrate $DUR_L ;;
        esac
        C="" TO=$MAIN_TO
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


c_read() {
    read -t $1 key || {
        C="${C}="
        return 0
    }
    case "$key" in
        "$PRESS_UP" )
            C="${C}u" ;;
        "$PRESS_DOWN" )
            C="${C}d" ;;
        "$RELEASE_UP"|"$RELEASE_DOWN" )
            C="${C}-" ;;
    esac
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

set -eu

TO=0.300
DUR_S=30
TORCH_OFF_TO=300
WL_MAX=310000

# Down 300ms = torch on
#           -> Up/Down   = torch Off


on_flashlight() {
    cmd vibrate $DUR_S
    cmd torch on
    while read -t $TORCH_OFF_TO key; do
        [ "$key" = "$RELEASE_DOWN" ] || break;
    done
    cmd torch off
}


on_down() {
    read -t $TO key && {
        cmd volume music down
        return
    }
    on_flashlight
}


loop() {
    while read key; do
        cmd wakelock acquire $WL_MAX
        
        case "$key" in
            "$PRESS_UP" )
                cmd volume music up
            ;;
            "$PRESS_DOWN" )
                on_down
            ;;
        esac
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

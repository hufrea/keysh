set -eu

TO=0.300
DUR_S=30
OFF_TIMEOUT=300

# Down 300ms = torch on
#           -> Up/Down   = torch Off


on_flashlight() {
    vibrate $DUR_S
    torch on
    while read -t $OFF_TIMEOUT key; do
        [ "$key" = "$RELEASE_DOWN" ] || break;
    done
    torch off
}


on_down() {
    read -t $TO key && {
        volume music down
        return
    }
    on_flashlight
}


loop() {
    while read key; do
        wakelock acquire 310000
        
        case "$key" in
            "$PRESS_UP" )
                volume music up
            ;;
            "$PRESS_DOWN" )
                on_down
            ;;
        esac
        wakelock release
    done
}

###

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
torch() {
    # on|off
    echo "torch $*";
}

loop

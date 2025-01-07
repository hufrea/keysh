set -eu

TO=0.300
DUR_S=30
WL_MAX=5000

# UP 300ms = run command in Termux


on_up() {
    read -t $TO key && {
        cmd volume music up
        return
    }
    cmd vibrate $DUR_S
    termux-run bash -c 'top -n 5'
}


loop() {
    cmd permission TERMUX_RUN_COMMAND
    
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


termux-run() {
    BIN="/data/data/com.termux/files/usr/bin/${1}"
    shift; encode_list "$@"; ARGS="$ENCODED"
    
    cmd intent -a 'com.termux.RUN_COMMAND' \
        -t 'service' \
        -p 'com.termux/com.termux.app.RunCommandService' \
        -e "com.termux.RUN_COMMAND_PATH:${BIN}" \
        -e "com.termux.RUN_COMMAND_BACKGROUND:false" \
        -e "com.termux.RUN_COMMAND_ARGUMENTS:{${ARGS}}"
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

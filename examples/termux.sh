set -eu

TO=0.300
DUR_S=30

# UP 300ms = run command in Termux


on_up() {
    read -t $TO key && {
        volume music up
        return
    }
    vibrate $DUR_S
    termux-run bash -c 'top -n 5'
}


loop() {
    permission TERMUX_RUN_COMMAND
    
    while read key; do
        wakelock acquire 5000

        case "$key" in
            "$PRESS_UP" )
                on_up
            ;;
            "$PRESS_DOWN" )
                volume music down
            ;;
        esac
        wakelock release
    done
}


termux-run() {
    BIN="/data/data/com.termux/files/usr/bin/${1}"
    ARGS=":+-+:${2}"
    shift; shift

    for arg in "$@"; do
        ARGS="${ARGS}+-+${arg}"
    done

    intent -a 'com.termux.RUN_COMMAND' \
        -t 'service' \
        -p 'com.termux/com.termux.app.RunCommandService' \
        -e "com.termux.RUN_COMMAND_PATH:${BIN}" \
        -e "com.termux.RUN_COMMAND_BACKGROUND:false" \
        -e "com.termux.RUN_COMMAND_ARGUMENTS:{${ARGS}}"
}

###

DELIM="|+|"

vibrate() {
    echo "vibrate $*";
}
wakelock() {
    echo "wakelock $*";
}
volume() {
    echo "volume $*";
}
intent() {
    ARGS=""
    for arg in "$@"; do
        ARGS="${ARGS}${DELIM}${arg}"
    done
    echo ":${DELIM}:intent${ARGS}";
}
permission() {
    echo "permission $*";
}


loop

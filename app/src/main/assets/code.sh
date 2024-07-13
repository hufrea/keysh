set -eu

# Up 300ms  -> next track
# Down 30ms -> prev track

TO=0.300
DUR_S=25
WL_MAX=1500


on_up() {
    read -t $TO key && {
        volume music up
        return
    }
    media next
    vibrate $DUR_S
}


on_down() {
    read -t $TO key && {
        volume music down
        return
    }
    media previous
    vibrate $DUR_S
}


loop() {
  while read key; do
    wakelock acquire $WL_MAX
      case "$key" in
          "$PRESS_UP" )
              on_up
          ;;
          "$PRESS_DOWN" )
              on_down
          ;;
      esac
      wakelock release
  done
}


### Supported commands

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
    # music|notification|ring|call|current up|down|<level>
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
permission() {
    # BACKGROUND_ACTIVITY
    # STORAGE|TERMUX_RUN_COMMAND
    echo "permission $*";
}


loop

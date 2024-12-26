set -eu

# Up 300ms  -> next track
# Down 30ms -> prev track

TO=0.300
DUR_S=25

WORK=1
EXCLUDE_APPS="com.android.camera com.android.camera2"

on_up() {
    read_key -t $TO && {
        volume current up
        return
    }
    media next
    vibrate $DUR_S
}

on_down() {
    read_key -t $TO && {
        volume current down
        return
    }
    media previous
    vibrate $DUR_S
}

read_key() {
    while read $@ key; do
        case "$key" in
        "app:"* )
            on_app "$key" ;;
        * )
            return 0 ;;
        esac
    done
    return 1
}
    
on_app() {
    data="${1#*:}"
    pkg="${data%%:*}"
    
    case "$EXCLUDE_APPS " in 
    *"$pkg "* )
        [ "$WORK" = "1" ] && self PAUSE
        WORK=0
        return ;;
    esac
    if [ "$WORK" = "0" ]; then
        self RESUME
        WORK=1
    fi
}

loop() {
  while read_key; do
      case "$key" in
          "$PRESS_UP" )
              on_up
          ;;
          "$PRESS_DOWN" )
              on_down
          ;;
      esac
  done
}

self() {
    intent -a "${PACKAGE_NAME}.${1}" -t 'broadcast'
}


DELIM="|+|"

vibrate() {
    echo "vibrate $*";
}
media() {
    echo "media $*";
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


permission ACCESSIBILITY

self RECV_APP_SWITCH

loop

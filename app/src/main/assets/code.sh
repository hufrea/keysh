set -eu

# Up 300ms  -> next track
# Down 30ms -> prev track

TO=0.300
DUR_S=25
WL_MAX=1500


on_up() {
    read -t $TO key && {
        cmd volume current up
        return
    }
    cmd media next
    cmd vibrate $DUR_S
}


on_down() {
    read -t $TO key && {
        cmd volume current down
        return
    }
    cmd media previous
    cmd vibrate $DUR_S
}


loop() {
  while read key; do
    cmd wakelock acquire $WL_MAX
    case "$key" in
          "$PRESS_UP" )
              on_up
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

### IO

# SEncoded item format: <size>:<data>
# Output format: <s:<s:command><s:arg1><s:arg2>>
# Example: "24:8:wakelock7:acquire3:100" (wakelock acquire 100)
# Input format: <event_name>:<data>


### Supported commands

# vibrate <duration_ms>

# wakelock acquire|release [timeout_ms]

# media play|pause|play_pause|next|previous|rewind|forward|stop

# volume music|notification|ring|call|current up|down|<level>

# torch on|off

# notify -c <content> -t <title> -i <id> -l <timeout_ms>

# intent
    # -a <action>
    # -t service|broadcast|activity
    # -p <package/.Component>
    # -d <data> -m <mimetype> -c <category>
    # -e <extra_key>:<extra_value>
    # -e <extra_key>:{<s:<s:val1><s:val2>>}
    #    format: float - 0.1f, long - 1l, int - 1, double - 1.0, bool - true, string - 'str'
    
# permission BACKGROUND_ACTIVITY|STORAGE|TERMUX_RUN_COMMAND|ACCESSIBILITY


### Supported intent actions

# action=.PAUSE; action=.RESUME, target=broadcast
# Pause or resume key press events. Works only on accessibility service.

# action=.RECV_APP_SWITCH, target=broadcast
# Receive an foreground app changed events. Works only on accessibility service.
# Event format: app:<package_name>


### Vars

# PRESS_UP = "key:2"
# RELEASE_UP = "key:3"
# PRESS_DOWN = "key:4"
# RELEASE_DOWN = "key:5"
# PACKAGE_NAME = <application package name>
# VERSION = <application version code>


### Examples: https://github.com/hufrea/keysh/tree/master/examples

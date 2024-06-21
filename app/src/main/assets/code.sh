set -eu
TO=0.300


on_up() {
    read -t $TO key && {
        echo "volume music up"
        return
    }
    echo "media next"
    echo "vibrate 30"
}


on_down() {
    read -t $TO key && {
        echo "volume music down"
        return
    }
    echo "media previous"
    echo "vibrate 30"
}


while read key; do
    echo "wakelock acquire 3000"
    
    case "$key" in
        "$PRESS_UP" )
            on_up
        ;;
        "$PRESS_DOWN" )
            on_down
        ;;
    esac
    echo "wakelock release"
done

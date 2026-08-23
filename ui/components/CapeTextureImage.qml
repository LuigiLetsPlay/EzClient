import QtQuick 2.15

// Displays the visible 10x16 vanilla cape face, not the entire sparse atlas.
Item {
    id: root
    property string capeSource: ""

    Image {
        source: root.capeSource
        asynchronous: true
        fillMode: Image.Stretch
        smooth: true
        mipmap: true

        // A normal cape atlas is 64x32. Its visible back face occupies
        // x=1..11 and y=1..17, i.e. one tenth of the width and half of the
        // height. Scale the whole atlas accordingly, then shift that face
        // into this portrait-sized item.
        width: root.width * 6.4
        height: root.height * 2.0
        x: -root.width * 0.1
        y: -root.height * 0.0625
    }
}

import QtQuick 2.15

WheelHandler {
    // ScrollView puts declared children into its content item, so "parent"
    // is the internal Flickable that owns contentX/contentY.
    target: parent
    acceptedDevices: PointerDevice.Mouse
    rotationScale: 2.0
}

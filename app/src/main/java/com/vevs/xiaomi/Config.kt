package com.vevs.xiaomi

// ✏️ EDIT THESE, then run the app.
object Config {
    // Camera is OPTIONAL. Leave "" to hide it (bulbs then fill the whole screen).
    // Example: "rtsp://USER:PASS@192.168.100.6:554/stream1"
    const val CAMERA_RTSP = ""

    // Bulb 1: Mi Smart LED Bulb (Global), model yeelink.light.mono6
    val BULB_1: Light = MiioLight("Bulb 1", "192.168.100.7", "f9276cab497adbce1272082a9aa797a8")
}

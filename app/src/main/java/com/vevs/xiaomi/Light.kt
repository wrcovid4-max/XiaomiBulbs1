package com.vevs.xiaomi

import java.io.IOException

interface Light {
    val name: String
    fun setPower(on: Boolean)   // blocking: call off the main thread
}

/** Wi-Fi Xiaomi bulb over local miIO (UDP 54321). Tries MIoT first, then legacy set_power. */
class MiioLight(override val name: String, ip: String, token: String) : Light {
    private val client = MiioClient(ip, token)

    override fun setPower(on: Boolean) {
        val r = client.call("set_properties", """[{"did":"power","siid":2,"piid":1,"value":$on}]""")
        if (r.contains("\"code\":0")) return
        val s = if (on) "on" else "off"
        val r2 = client.call("set_power", """["$s","smooth",300]""")
        if (!r2.contains("ok")) throw IOException("Bulb replied: ${r2.take(60)}")
    }
}

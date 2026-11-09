package com.vevs.xiaomi

import java.io.IOException
import java.net.DatagramPacket
import java.net.DatagramSocket
import java.net.InetAddress
import java.nio.ByteBuffer
import java.security.MessageDigest
import javax.crypto.Cipher
import javax.crypto.spec.IvParameterSpec
import javax.crypto.spec.SecretKeySpec

/** Minimal miIO local protocol client. */
class MiioClient(private val ip: String, tokenHex: String) {
    private val token = ByteArray(16) { tokenHex.substring(it * 2, it * 2 + 2).toInt(16).toByte() }
    private val key = md5(token)
    private val iv = md5(key + token)
    private var msgId = (System.currentTimeMillis() % 10000).toInt()

    private fun md5(b: ByteArray) = MessageDigest.getInstance("MD5").digest(b)

    private fun aes(mode: Int, data: ByteArray): ByteArray {
        val c = Cipher.getInstance("AES/CBC/PKCS5Padding")
        c.init(mode, SecretKeySpec(key, "AES"), IvParameterSpec(iv))
        return c.doFinal(data)
    }

    @Synchronized
    fun call(method: String, paramsJson: String): String {
        DatagramSocket().use { sock ->
            sock.soTimeout = 3000
            val addr = InetAddress.getByName(ip)
            fun send(b: ByteArray) = sock.send(DatagramPacket(b, b.size, addr, 54321))
            fun recv(): ByteArray {
                val buf = ByteArray(2048)
                val p = DatagramPacket(buf, buf.size)
                sock.receive(p)
                return buf.copyOf(p.length)
            }
            // 1) Handshake
            val hello = ByteArray(32) { 0xFF.toByte() }
            hello[0] = 0x21; hello[1] = 0x31; hello[2] = 0; hello[3] = 0x20
            send(hello)
            val h = ByteBuffer.wrap(recv())
            val devId = h.getInt(8)
            val stamp = h.getInt(12) + 1
            // 2) Command
            msgId++
            val json = """{"id":$msgId,"method":"$method","params":$paramsJson}"""
            val enc = aes(Cipher.ENCRYPT_MODE, json.toByteArray())
            val hdr = ByteBuffer.allocate(32)
            hdr.putShort(0x2131); hdr.putShort((32 + enc.size).toShort())
            hdr.putInt(0); hdr.putInt(devId); hdr.putInt(stamp); hdr.put(token)
            val sum = md5(hdr.array() + enc)
            hdr.position(16); hdr.put(sum)
            send(hdr.array() + enc)
            // 3) Reply
            val resp = recv()
            val len = ByteBuffer.wrap(resp).getShort(2).toInt() and 0xFFFF
            if (len <= 32) throw IOException("Empty reply")
            val plain = aes(Cipher.DECRYPT_MODE, resp.copyOfRange(32, len))
            return String(plain).trim { it == '\u0000' || it.isWhitespace() }
        }
    }
}

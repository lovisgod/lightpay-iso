package com.lovisgod.lightpayiso.tcp

import java.io.DataInputStream
import java.io.DataOutputStream
import java.io.IOException
import java.io.InputStream
import java.net.SocketAddress
import java.security.KeyManagementException
import java.security.NoSuchAlgorithmException
import java.security.SecureRandom
import javax.net.ssl.SSLContext
import javax.net.ssl.SSLSocket
import javax.net.ssl.SSLSocketFactory
import javax.net.ssl.TrustManager

/*
* To change this template, choose Tools | Templates
* and open the template in the editor.
 */
class IsoSocketImpl : IsoSocket {
    private var socket: SSLSocket? = null
    private var socketAddress: SocketAddress? = null
    private var serverIp: String? = null
    private var serverPort = 0
    private var timeout: Int
    private var factory: SSLSocketFactory? = null

    constructor(socketAddress: SocketAddress?, timeout: Int) {
        this.socketAddress = socketAddress
        this.timeout = timeout
    }

    constructor(serverIp: String?, serverPort: Int, timeout: Int) {
        this.serverIp = serverIp
        this.serverPort = serverPort
        this.timeout = timeout
        configureSSLContext()
    }

    private fun createSocketWithNoSSL() {}
    private fun configureSSLContext() {
        val tm = SavingTrustManager()
        try {
            val sc = SSLContext.getInstance("TLS")
            sc.init(null, arrayOf<TrustManager>(tm), SecureRandom())
            factory = sc.socketFactory
        } catch (e: KeyManagementException) {
            logEx(e)
        } catch (e: NoSuchAlgorithmException) {
            logEx(e)
        }
    }

    private fun logEx(e: Exception) {
        e.printStackTrace()
    }

    override fun setTimeout(timeout: Int) {
        this.timeout = timeout
    }

    override fun setIpAndPort(ip: String, port: Int) {
        serverIp = ip
        serverPort = port
    }

    private fun concat(A: ByteArray, B: ByteArray): ByteArray {
        val aLen = A.size
        val bLen = B.size
        val C = ByteArray(aLen + bLen)
        System.arraycopy(A, 0, C, 0, aLen)
        System.arraycopy(B, 0, C, aLen, bLen)
        return C
    }

    override fun open(): Boolean {
        return try {
            socket = factory!!.createSocket(serverIp, serverPort) as SSLSocket
            socket!!.setSoTimeout(timeout)
            true
        } catch (ex: IOException) {
            logEx(ex)
            false
        }
    }

    override fun close() {
        try {
            if (socket!!.isConnected) {
                socket!!.close()
            }
        } catch (ex: IOException) {
            logEx(ex)
        }
    }

    override fun send(data: ByteArray): Boolean {
        val length = data.size
        val headerbytes = ByteArray(2)
        headerbytes[0] = (length shr 8).toByte()
        headerbytes[1] = length.toByte()
        val dataToSend = concat(headerbytes, data)
        if (socket!!.isConnected) {
            try {
                val os = DataOutputStream(socket!!.getOutputStream())
                os.write(dataToSend)
                os.flush()
                return true
            } catch (ex: IOException) {
                logEx(ex)
            }
        }
        return false
    }

    private fun resize(data: ByteArray?): ByteArray {
        if (data == null || data.size == 0) {
            return ByteArray(SOCKET_SIZE_INCREAMENT)
        }
        val newData = ByteArray(data.size + SOCKET_SIZE_INCREAMENT)
        System.arraycopy(data, 0, newData, 0, data.size)
        return newData
    }

    @Throws(IOException::class)
    fun readFully(`in`: InputStream, b: ByteArray?, off: Int, len: Int): Int {
        if (len < 0) throw IndexOutOfBoundsException()
        var n = 0
        while (n < len) {
            val count = `in`.read(b, off + n, len - n)
            if (count < 0) break
            n += count
        }
        return n
    }

    /*data comes over the stream untill a timeout or nibss closes the socket*/
    override fun receive(): ByteArray? {
        val lenData = ByteArray(2)
        var receivedData: ByteArray? = null
        val dataLen: Int
        try {
            val `is` = DataInputStream(socket!!.getInputStream())
            readFully(`is`, lenData, 0, 2)
            //is.readFully(lenData, 0, 2);
            dataLen = (0xFF and lenData[0].toInt()) * 256 + (0xFF and lenData[1].toInt())
            receivedData = ByteArray(dataLen)
            readFully(`is`, receivedData, 0, dataLen)
        } catch (ex: IOException) {
            logEx(ex)
        }
        return receivedData
    }

    override fun sendReceive(data: ByteArray): ByteArray? {
        send(data)
        return receive()
    }

    companion object {
        private const val SOCKET_SIZE_INCREAMENT = 100 * 1024
    }
}

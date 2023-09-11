package com.lovisgod.lightpayiso.data

import com.lovisgod.lightpayiso.utild.DateUtils.monthFormatter
import com.lovisgod.lightpayiso.utild.DateUtils.timeAndDateFormatter
import com.lovisgod.lightpayiso.utild.DateUtils.timeFormatter
import com.lovisgod.lightpayiso.data.constants.Constants.ISW_TERMINAL_IP_CTMS
import com.lovisgod.lightpayiso.data.constants.Constants.ISW_TERMINAL_IP_CTMS_PROD
import com.lovisgod.lightpayiso.data.constants.Constants.ISW_TERMINAL_PORT_CTMS
import com.lovisgod.lightpayiso.data.constants.Constants.ISW_TERMINAL_PORT_CTMS_PROD
import com.lovisgod.lightpayiso.data.constants.Constants.getNextStan
import com.lovisgod.lightpayiso.tcp.IsoSocket
import com.lovisgod.lightpayiso.tcp.IsoSocketImpl
import com.lovisgod.lightpayiso.utild.HexUtil
import com.lovisgod.lightpayiso.utild.IsoUtils
import com.lovisgod.lightpayiso.utild.TerminalInfoParser
import com.lovisgod.lightpayiso.utild.TripleDES
import org.jpos.iso.ISOException
import org.jpos.iso.ISOMsg
import org.jpos.iso.packager.GenericPackager
import org.jpos.iso.packager.ISO87APackager
import java.util.*


class IsoMessageBuilder {
    var data = javaClass.getResourceAsStream("/fields.xml")
    var packager = GenericPackager(data)

    val socket: IsoSocket = IsoSocketImpl(ISW_TERMINAL_IP_CTMS, ISW_TERMINAL_PORT_CTMS, 60000)


    fun generateKeyDownloadMessage(
        processCode: String,
        terminalId: String,
        key: String): Any {
        return try {
            val now = Date()
            val stan = getNextStan()
            // Load package from resources directory.
            val isoMsg = ISOMsg()
            isoMsg.packager = packager
            isoMsg.mti = "0800"
            isoMsg[3] = processCode
            isoMsg[7] = timeAndDateFormatter.format(now)
            isoMsg[11] = stan
            isoMsg[12] = timeFormatter.format(now)
            isoMsg[13] = monthFormatter.format(now)
            isoMsg[41] = terminalId
            printISOMessage(isoMsg)
            val dataToSend = isoMsg.pack()
            // set server Ip and port
            socket.setIpAndPort(ISW_TERMINAL_IP_CTMS, ISW_TERMINAL_PORT_CTMS)

            // open to socket endpoint
            socket.open()

            // send request and process response
            val response = socket.sendReceive(dataToSend)

            println("response from nibbs : ${response?.asList()}")

            isoMsg.unpack(response)
            printISOMessage(isoMsg)
            println(isoMsg.getValue(39))
            println(isoMsg.getString(53))
            // close connection
            socket.close()

            if (isoMsg.getValue(39) != "00") {
                return "no key"
            } else {
                val encryptedKey = isoMsg.getString(53)
                val decryptedKey = TripleDES.soften(key, encryptedKey)
                println("Decrypted Key => ${decryptedKey.toString()}")
                return decryptedKey
            }

        } catch (e: ISOException) {
            throw Exception(e)
        }
    }


    fun downloadTerminalParam(
        processCode: String,
        terminalId: String,
        key: String): Any {
        return try {
            val now = Date()
            val stan = getNextStan()
            val field62 = "01009280824266"
//            val packager = ISO87APackager()
            // Load package from resources directory.
            val isoMsg = ISOMsg()
            isoMsg.packager = packager
            isoMsg.mti = "0800"
            isoMsg[3] = processCode
            isoMsg[7] = timeAndDateFormatter.format(now)
            isoMsg[11] = stan
            isoMsg[12] = timeFormatter.format(now)
            isoMsg[13] = monthFormatter.format(now)
            isoMsg[41] = terminalId
            isoMsg[62] = field62
            var dataToSend = isoMsg.pack()
            dataToSend[19]++
            val length = dataToSend.size
            println("length => $length")
//            val temp = ByteArray(length - 64)
//            if (length >= 64) {
//                System.arraycopy(dataToSend, 0, temp, 0, length - 64)
//            }

            val hashValue = IsoUtils.getMac(key, dataToSend) //SHA256
            isoMsg[64] = hashValue
             printISOMessage(isoMsg)

            println(isoMsg.pack().decodeToString())

            // set server Ip and port
            socket.setIpAndPort(ISW_TERMINAL_IP_CTMS, ISW_TERMINAL_PORT_CTMS)

            // open to socket endpoint
            socket.open()

            // send request and process response
            val response = socket.sendReceive(isoMsg.pack())

            println("response from nibbs : ${response?.asList()}")
            println("response from nibbsparams : ${HexUtil.toHexString(response)}")

            isoMsg.unpack(response)
            printISOMessage(isoMsg)
            println(isoMsg.getValue(39))
            println(isoMsg.getString(62))
            // close connection
            socket.close()

            if (isoMsg.getValue(39) != "00") {
                return "no key"
            }

            val terminalString = isoMsg.getString(62)
            println("Terminal Data String => $terminalString")

            // parse and save terminal info
            val terminalData =
                TerminalInfoParser.parse(terminalId, ISW_TERMINAL_IP_CTMS, ISW_TERMINAL_PORT_CTMS, terminalString)
            println("Terminal Data => " +
                    "currency code  :: ${terminalData?.transCurrencyCode}")

            return terminalData!!
        } catch (e: ISOException) {
            throw Exception(e)
        }
    }


    private fun printISOMessage(isoMsg: ISOMsg) {
        try {
            System.out.printf("MTI = %s%n", isoMsg.mti)
            for (i in 1..isoMsg.maxField) {
                if (isoMsg.hasField(i)) {
                    System.out.printf("Field (%s) = %s%n", i, isoMsg.getString(i))
                }
            }
        } catch (e: ISOException) {
            e.printStackTrace()
        }
    }
}
package com.lovisgod.lightpayiso.data

import com.lovisgod.lightpayiso.data.constants.Constants
import com.lovisgod.lightpayiso.utild.DateUtils.monthFormatter
import com.lovisgod.lightpayiso.utild.DateUtils.timeAndDateFormatter
import com.lovisgod.lightpayiso.utild.DateUtils.timeFormatter
import com.lovisgod.lightpayiso.data.constants.Constants.ISW_TERMINAL_IP_CTMS
import com.lovisgod.lightpayiso.data.constants.Constants.ISW_TERMINAL_IP_CTMS_PROD
import com.lovisgod.lightpayiso.data.constants.Constants.ISW_TERMINAL_PORT_CTMS
import com.lovisgod.lightpayiso.data.constants.Constants.ISW_TERMINAL_PORT_CTMS_PROD
import com.lovisgod.lightpayiso.data.constants.Constants.UP_IP
import com.lovisgod.lightpayiso.data.constants.Constants.UP_PORT
import com.lovisgod.lightpayiso.data.constants.Constants.getNextStan
import com.lovisgod.lightpayiso.data.models.AccountType
import com.lovisgod.lightpayiso.data.models.PurchaseResponse
import com.lovisgod.lightpayiso.data.models.RequestIccData
import com.lovisgod.lightpayiso.data.models.TerminalInfo
import com.lovisgod.lightpayiso.tcp.IsoNoSslSocketImpl
import com.lovisgod.lightpayiso.tcp.IsoSocket
import com.lovisgod.lightpayiso.tcp.IsoSocketImpl
import com.lovisgod.lightpayiso.utild.HexUtil
import com.lovisgod.lightpayiso.utild.IsoUtils
import com.lovisgod.lightpayiso.utild.IsoUtils.getBINFromPAN
import com.lovisgod.lightpayiso.utild.TerminalInfoParser
import com.lovisgod.lightpayiso.utild.TripleDES
import org.jpos.iso.ISOException
import org.jpos.iso.ISOMsg
import org.jpos.iso.packager.GenericPackager
import org.jpos.iso.packager.ISO87APackager
import java.util.*


class IsoMessageBuilderUp {
    var data = javaClass.getResourceAsStream("/fields.xml")
    var packager = GenericPackager(data)

    val socket: IsoSocket = IsoNoSslSocketImpl(UP_IP, UP_PORT, 60000)


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
            socket.setIpAndPort(UP_IP, UP_PORT)

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

            // set server Ip and port
            socket.setIpAndPort(UP_IP, UP_PORT)

            // open to socket endpoint
            socket.open()

            // send request and process response
            val response = socket.sendReceive(isoMsg.pack())

            println("response from up : ${response?.asList()}")
            println("response from upparams : ${response?.let { HexUtil.toHexString(it) }}")

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
                TerminalInfoParser.parse(terminalId, UP_IP, UP_PORT, terminalString)
            println("Terminal Data => " +
                    "currency code  :: ${terminalData?.transCurrencyCode}")

            return terminalData!!
        } catch (e: ISOException) {
            throw Exception(e)
        }
    }

    fun getCashoutRequest(
        iccString: String,
        terminalInfo: TerminalInfo,
        transaction: RequestIccData,
        accountType: AccountType,
        posDataCode: String,
        sessionKey: String
    ): PurchaseResponse {
        val now = Date()
        val message = ISOMsg()
        val processCode = "01" + accountType.value + "00"
        var hasPin = transaction.haspin
        val stan = getNextStan()
        val randomReference = "${Date().time}".substring(0, 12)

        val track2data =transaction.TRACK_2_DATA
        println("track2 data => ${track2data}")
        // extract pan and expiry
        val strTrack2 = track2data.split("F")[0]
        println(strTrack2.split("D"))
        var panX = strTrack2.split("D")[0]
        val expiry = strTrack2.split("D")[1].substring(0, 4)
        val src = strTrack2.split("D")[1].substring(4, 7)

        message.packager = packager
        message.mti = "0200"
        message.set(2, panX)
        message.set(3, processCode)
        message.set(4, transaction.TRANSACTION_AMOUNT)
        message.set(7, timeAndDateFormatter.format(now))
        message.set(11, stan)
            message.set(12, timeFormatter.format(now))
            message.set(13, monthFormatter.format(now))
            message.set(14, expiry)
            message.set(18, terminalInfo.merchantCategoryCode)
            message.set(22, if (hasPin == true) "051" else "950")
            message.set(23, transaction.APP_PAN_SEQUENCE_NUMBER)
            message.set(25, "00")
            message.set(26, "06")
            message.set(28, "C00000000")
            message.set(32, getBINFromPAN(panX))
            message.set(35, transaction.TRACK_2_DATA)
            message.set(37, randomReference)
            message.set(40, src)
            message.set(41, terminalInfo.terminalCode)
            message.set(42, terminalInfo.merchantId)
            message.set(43, terminalInfo.merchantName)
            message.set(49, "566")
            message.set(55, iccString)


        if (hasPin == true) {
            message.set(52, transaction.EMV_CARD_PIN_DATA.CardPinBlock)
                message.set(123, posDataCode)
        } else {
            message.set(123, posDataCode)
        }


        // set message hash
        val bytes = message.pack()
        val length = bytes.size
//        val temp = ByteArray(length - 64)
//        if (length >= 64) {
//            System.arraycopy(bytes, 0, temp, 0, length - 64)
//        }


        val hashValue = IsoUtils.getMac(sessionKey, bytes) //SHA256
        message.set(128, hashValue)
        message.dump(System.out, "request -- ")

        try {

            // set server Ip and port
            socket.setIpAndPort(UP_IP, UP_PORT)
            // open connection
            val isConnected = socket.open()
            if (!isConnected) return PurchaseResponse(
                responseCode = Constants.TIMEOUT_CODE,
                authCode = "",
                stan = "",
                scripts = "",
                date = now.time,
                description=  IsoUtils.getIsoResultMsg(Constants.TIMEOUT_CODE) ?: "Unknown Error",
                referenceNumber = randomReference,
                transactionTime = timeFormatter.format(now),
                transactionDate = monthFormatter.format(now),
                transactionDateTime = timeAndDateFormatter.format(now),
                hasPinValue = hasPin
            )


            val request = message.pack()
            println("Purchase Request HEX ---> ${IsoUtils.bytesToHex(request)}")

            val response = socket.sendReceive(request)
            println("response from nibbspurchase : ${response?.let { HexUtil.toHexString(it) }}")

            println("response from nibbspurchase length : ${response?.size}")

            // close connection
            socket.close()

           message.unpack(response)

            printISOMessage(message)
            println(message.getValue(39))
            // close connection
            socket.close()

//            if (message.getValue(39) != "00") {
//                return "no key"
//            }


            // return response
            return message.let {
                val authCode = it.getValue(38) ?: ""
                val code = it.getValue(39) ?: ""
                val scripts = it.getValue(55) ?: ""
                val transTime = it.getValue(12) ?: ""
                val transDate = it.getValue(13) ?: ""

                println("code is code is ::: $code while time is $transTime date is ${transDate}")

                val responseMsg = IsoUtils.getIsoResultMsg(code.toString()) ?: "Unknown Error"

                return@let PurchaseResponse(
                    responseCode = code.toString(),
                    authCode = authCode.toString(),
                    stan = stan,
                    scripts = scripts.toString(),
                    date = now.time,
                    description = responseMsg.toString(),
                    referenceNumber = randomReference,
                    transactionTime = transTime.toString(),
                    transactionDate = transDate.toString(),
                    transactionDateTime = timeAndDateFormatter.format(now),
                    hasPinValue = hasPin
                )
            }
        } catch (e: Exception) {
            // log error
            e.printStackTrace()
            e.printStackTrace()
            // return response
            return PurchaseResponse(
                responseCode = Constants.TIMEOUT_CODE,
                authCode = "",
                stan = stan,
                scripts = "",
                date = now.time,
                description=  IsoUtils.getIsoResultMsg(Constants.TIMEOUT_CODE) ?: "Unknown Error",
                referenceNumber = randomReference,
                transactionTime = timeFormatter.format(now),
                transactionDate = monthFormatter.format(now),
                transactionDateTime = timeAndDateFormatter.format(now),
                hasPinValue = hasPin
            )
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
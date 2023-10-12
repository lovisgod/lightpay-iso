package com.lovisgod.lightpayiso.data

import com.lovisgod.lightpayiso.data.constants.Constants
import com.lovisgod.lightpayiso.utild.DateUtils.monthFormatter
import com.lovisgod.lightpayiso.utild.DateUtils.timeAndDateFormatter
import com.lovisgod.lightpayiso.utild.DateUtils.timeFormatter
import com.lovisgod.lightpayiso.data.constants.Constants.ISW_TERMINAL_IP_CTMS
import com.lovisgod.lightpayiso.data.constants.Constants.ISW_TERMINAL_IP_CTMS_PROD
import com.lovisgod.lightpayiso.data.constants.Constants.ISW_TERMINAL_PORT_CTMS
import com.lovisgod.lightpayiso.data.constants.Constants.ISW_TERMINAL_PORT_CTMS_PROD
import com.lovisgod.lightpayiso.data.constants.Constants.SRCI
import com.lovisgod.lightpayiso.data.constants.Constants.getNextStan
import com.lovisgod.lightpayiso.data.models.AccountType
import com.lovisgod.lightpayiso.data.models.PurchaseResponse
import com.lovisgod.lightpayiso.data.models.RequestIccData
import com.lovisgod.lightpayiso.data.models.TerminalInfo
import com.lovisgod.lightpayiso.tcp.IsoSocket
import com.lovisgod.lightpayiso.tcp.IsoSocketImpl
import com.lovisgod.lightpayiso.utild.*
import com.lovisgod.lightpayiso.utild.TerminalInfoParser
import com.solab.iso8583.parse.ConfigParser
import org.jpos.iso.ISOException
import org.jpos.iso.ISOMsg
import org.jpos.iso.packager.GenericPackager
import org.jpos.iso.packager.ISO87APackager
import java.io.StringReader
import java.util.*


class IsoMessageBuilderJ8583 {

    private val messageFactory by lazy {
        try {

            val data = FileUtils.getFromAssets("/jpos.xml")
            val string = String(data!!)
            val stringReader = StringReader(string)
            val messageFactory = ConfigParser.createFromReader(stringReader)
            messageFactory.isUseBinaryBitmap = false //NIBSS usebinarybitmap = false
            messageFactory.characterEncoding = "UTF-8"

            return@lazy messageFactory

        } catch (e: Exception) {
            e.printStackTrace()
            throw e
        }
    }


    val socket: IsoSocket = IsoSocketImpl(ISW_TERMINAL_IP_CTMS_PROD, ISW_TERMINAL_PORT_CTMS_PROD, 20000)


    fun generateKeyDownloadMessage(
        processCode: String,
        terminalId: String,
        key: String): Any {
        return try {
            val now = Date()
            val stan = getNextStan()
            // Load package from resources directory.
            val message = NibssIsoMessage(messageFactory.newMessage(0x800))
            message
                    .setValue(3, processCode)
                    .setValue(7, timeAndDateFormatter.format(now))
                    .setValue(11, stan)
                    .setValue(12, timeFormatter.format(now))
                    .setValue(13, monthFormatter.format(now))
                    .setValue(41, terminalId)

            // remove unset fields
            message.message.removeFields(62, 64)
            message.dump(System.out, "request -- ")

            // set server Ip and port
            socket.setIpAndPort(ISW_TERMINAL_IP_CTMS_PROD, ISW_TERMINAL_PORT_CTMS_PROD)

            // open to socket endpoint
            socket.open()

            val request = message.message.writeData()
            println("Key Xch Request HEX ---> ${IsoUtils.bytesToHex(request)}")

            // send request and process response
            val response = socket.sendReceive(message.message.writeData())
            // close connection
            socket.close()

            // read message
            val msg = NibssIsoMessage(messageFactory.parseMessage(response, 0))
            msg.dump(System.out, "response -- ")


            // extract encrypted key with clear key
            val encryptedKey = msg.message.getField<String>(SRCI)
            val decryptedKey = TripleDES.soften(key, encryptedKey.value)

            return decryptedKey

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

            val message = NibssIsoMessage(messageFactory.newMessage(0x800))
            message
                    .setValue(3, processCode)
                    .setValue(7, timeAndDateFormatter.format(now))
                    .setValue(11, stan)
                    .setValue(12, timeFormatter.format(now))
                    .setValue(13, monthFormatter.format(now))
                    .setValue(41, terminalId)
                    .setValue(62, field62)


            val bytes = message.message.writeData()
            val length = bytes.size
            val temp = ByteArray(length - 64)
            if (length >= 64) {
                System.arraycopy(bytes, 0, temp, 0, length - 64)
            }


            // confirm that key was downloaded
            if (key.isEmpty()) return false

            val hashValue = IsoUtils.getMac(key, temp) //SHA256
            message.setValue(64, hashValue)
            message.dump(System.out, "parameter request ---- ")

            // set server Ip and port
            socket.setIpAndPort(ISW_TERMINAL_IP_CTMS_PROD, ISW_TERMINAL_PORT_CTMS_PROD)

            // open socket connection
            socket.open()

            // send request and receive response
            val response = socket.sendReceive(message.message.writeData())
            // close connection
            socket.close()

            // read message
            val responseMessage = NibssIsoMessage(messageFactory.parseMessage(response, 0))
            responseMessage.dump(System.out, "parameter response ---- ")


            // getResult string formatted terminal info
            val terminalString = responseMessage.message.getField<String>(62).value

            // parse and save terminal info
            val terminalData =
                    TerminalInfoParser.parse(terminalId, ISW_TERMINAL_IP_CTMS_PROD, ISW_TERMINAL_PORT_CTMS_PROD, terminalString)
            println("Terminal Data => " +
                    "currency code  :: ${terminalData?.transCurrencyCode}")

            return terminalData!!
        } catch (e: ISOException) {
            throw Exception(e)
        }
    }

    fun getPurchaseRequest(
        iccString: String,
        terminalInfo: TerminalInfo,
        transaction: RequestIccData,
        accountType: AccountType,
        posDataCode: String,
        sessionKey: String
    ): PurchaseResponse {
        val now = Date()
        val message = NibssIsoMessage(messageFactory.newMessage(0x200))
        val processCode = "00" + accountType.value + "00"
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

        message
                .setValue(2, panX)
                .setValue(3, processCode)
                .setValue(4, transaction.TRANSACTION_AMOUNT)
                .setValue(7, timeAndDateFormatter.format(now))
                .setValue(11, stan)
                .setValue(12, timeFormatter.format(now))
                .setValue(13, monthFormatter.format(now))
                .setValue(14, expiry)
                .setValue(18, terminalInfo.merchantCategoryCode)
                .setValue(22, "051")
                .setValue(23, transaction.APP_PAN_SEQUENCE_NUMBER)
                .setValue(25, "00")
                .setValue(26, "06")
                .setValue(28, "C00000000")
                .setValue(35, transaction.TRACK_2_DATA)
                .setValue(32, IsoUtils.getBINFromPAN(panX))
                .setValue(37, randomReference)
                .setValue(40, src)
                .setValue(41, terminalInfo.terminalCode)
                .setValue(42, terminalInfo.merchantId)
                .setValue(43, terminalInfo.merchantName)
                .setValue(49, "566")
                .setValue(55, iccString)

        if (hasPin == true) {
            message.setValue(52, transaction.EMV_CARD_PIN_DATA.CardPinBlock)
                    .setValue(123, "510101511344101")
            // remove unset fields
            message.message.removeFields( 59)
        } else {
            message.setValue(123, "511101511344101")
            // remove unset fields
            message.message.removeFields( 52, 59)
        }

        // set message hash
        val bytes = message.message.writeData()
        println(IsoUtils.bytesToHex(bytes))
        val length = bytes.size
        val temp = ByteArray(length - 64)
        if (length >= 64) {
            System.arraycopy(bytes, 0, temp, 0, length - 64)
        }

//        val sessionKey = store.getString(KEY_SESSION_KEY, "")
        val hashValue = IsoUtils.getMac(sessionKey, temp) //SHA256
        message.setValue(128, hashValue)
        message.dump(System.out, "request -- ")

        try {
            // open connection
            val isConnected = socket.open()
            if (!isConnected) PurchaseResponse(
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

            val request = message.message.writeData()
            println("Purchase Request HEX ---> ${IsoUtils.bytesToHex(request)}")

            val response = socket.sendReceive(request)
            // close connection
            socket.close()

//            if(response == null || response.isEmpty())
//                return reversePurchase(message, now.time, "20")

            println("Purchase Response HEX ---> ${response?.let { IsoUtils.bytesToHex(it) }}")

            val responseMsg = NibssIsoMessage(messageFactory.parseMessage(response, 0))
            responseMsg.dump(System.out, "")


            // return response
            return responseMsg.message.let {
                val authCode = it.getObjectValue<String>(38) ?: ""
                val code = it.getObjectValue<String>(39) ?: ""
                val scripts = it.getObjectValue<String>(55) ?: ""
                val transTime = it.getObjectValue<String>(12) ?: ""
                val transDate = it.getObjectValue<String>(13) ?: ""

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
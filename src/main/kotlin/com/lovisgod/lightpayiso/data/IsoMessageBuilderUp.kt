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
import com.lovisgod.lightpayiso.utild.*
import com.lovisgod.lightpayiso.utild.IsoUtils.getBINFromPAN
import com.lovisgod.lightpayiso.utild.TerminalInfoParser
import com.solab.iso8583.parse.ConfigParser
import org.jpos.iso.ISOException
import org.jpos.iso.ISOMsg
import org.jpos.iso.packager.GenericPackager
import org.jpos.iso.packager.ISO87APackager
import java.io.StringReader
import java.util.*
import kotlin.math.exp


class IsoMessageBuilderUp {
    // set up message factoctory

    private val messageFactory by lazy {
        try {

            val data = FileUtils.getFromAssets("/jpos_up.xml")
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



    var data = javaClass.getResourceAsStream("/fields.xml")
    var packager = GenericPackager(data)

    val socket: IsoSocket = IsoNoSslSocketImpl(UP_IP, UP_PORT, 40000)


    fun generateKeyDownloadMessage(
        processCode: String,
        terminalId: String,
        key: String): Any {
        return try {
            val now = Date()
            val stan = getNextStan()
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

            socket.setIpAndPort(UP_IP, UP_PORT)

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
            if (msg.message.getField<String>(39).value != "00") {
                return "no key"
            } else {
                val encryptedKey = msg.message.getField<String>(Constants.SRCI)
                val decryptedKey = TripleDES.soften(key, encryptedKey.value)

                return decryptedKey
            }
        } catch (e: Exception) {
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
            socket.setIpAndPort(UP_IP, UP_PORT)

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

    fun getCashoutRequest(
        iccString: String,
        terminalInfo: TerminalInfo,
        transaction: RequestIccData,
        accountType: AccountType,
        posDataCode: String,
        sessionKey: String
    ): PurchaseResponse {
        val now = Date()
//        val message = ISOMsg()
        val message = NibssIsoMessage(messageFactory.newMessage(0x200))
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

//        message.packager = packager
//        message.mti = "0200"
            message.setValue(2, panX)
            message.setValue(3, processCode)
            message.setValue(4, transaction.TRANSACTION_AMOUNT)
            message.setValue(7, timeAndDateFormatter.format(now))
            message.setValue(11, stan)
            message.setValue(12, timeFormatter.format(now))
            message.setValue(13, monthFormatter.format(now))
            message.setValue(14, expiry)
            message.setValue(18, terminalInfo.merchantCategoryCode)
            message.setValue(22, if (hasPin == true) "051" else "051")
            message.setValue(23, transaction.APP_PAN_SEQUENCE_NUMBER)
            message.setValue(25, "00")
            message.setValue(26, "06")
            message.setValue(28, "C00000000")
            message.setValue(32, "111129")
            message.setValue(35, transaction.TRACK_2_DATA)
            message.setValue(37, randomReference)
            message.setValue(40, src)
            message.setValue(41, terminalInfo.terminalCode)
            message.setValue(42, terminalInfo.merchantId)
            message.setValue(43, terminalInfo.merchantName)
            message.setValue(49, "566")
            message.setValue(55, iccString)


        if (hasPin == true) {
            message.setValue(52, transaction.EMV_CARD_PIN_DATA.CardPinBlock)
            message.setValue(123, posDataCode)
            message.setValue(62, "010085C54697648036MeterNumber=12.87001585.Acct=0000000000")
            message.message.removeFields( 59)
        } else {
            message.setValue(123, posDataCode)
            message.setValue(62, "010085C54697648036MeterNumber=12.87001585.Acct=0000000000")
            message.message.removeFields( 52, 59)
        }


        val bytes = message.message.writeData()
        println(IsoUtils.bytesToHex(bytes))
        val length = bytes.size
        val temp = ByteArray(length - 64)
        if (length >= 64) {
            System.arraycopy(bytes, 0, temp, 0, length - 64)
        }


        val hashValue = IsoUtils.getMac(sessionKey, temp) //SHA256
        message.setValue(128, hashValue)
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


            val request = message.message.writeData()
            println("Purchase Request HEX ---> ${IsoUtils.bytesToHex(request)}")

            val response = socket.sendReceive(request)
            println("response from nibbspurchase : ${response?.let { HexUtil.toHexString(it) }}")

            println("response from nibbspurchase length : ${response?.size}")

            // close connection
            socket.close()


//            if(response == null || response.isEmpty())
//                return reversePurchase(message, now.time, "20")

            println("Purchase Response HEX ---> ${response?.let { IsoUtils.bytesToHex(it) }}")

            val responseMsgx = NibssIsoMessage(messageFactory.parseMessage(response, 0))
            responseMsgx.dump(System.out, "")

            // return response
            return responseMsgx.message.let {
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


    fun getPayAttitudeRequest(
        iccString: String,
        terminalInfo: TerminalInfo,
        transaction: RequestIccData,
        accountType: AccountType,
        posDataCode: String,
        sessionKey: String,
        field7: String,
        field12: String
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
        val strTrack2 = track2data
        println(strTrack2.split("D"))
        var panX = strTrack2.split("D")[0]
        val expiry = strTrack2.split("D")[1].substring(0, 4)
//        val src = strTrack2.split("D")[1].substring(4, 7)
        var src = "221"

        message.packager = packager
        message.mti = "0200"
        message.set(2, panX)
        message.set(3, processCode)
        message.set(4, transaction.TRANSACTION_AMOUNT)
        message.set(7, field7)
        message.set(11, stan)
        message.set(12, field12)
        message.set(13, monthFormatter.format(now))
        message.set(14, expiry)
        message.set(18, terminalInfo.merchantCategoryCode)
        message.set(22, if (hasPin == true) "051" else "051")
        message.set(23, transaction.APP_PAN_SEQUENCE_NUMBER)
        message.set(25, "00")
        message.set(26, "06")
        message.set(28, "C00000000")
        message.set(32, "111129")
        message.set(35, transaction.TRACK_2_DATA)
        message.set(37, randomReference)
        message.set(40, src)
        message.set(41, terminalInfo.terminalCode)
        message.set(42, terminalInfo.merchantId)
        message.set(43, terminalInfo.merchantName)
        message.set(49, "566")
//        message.set(55, iccString)


        if (hasPin == true) {
            message.set(52, transaction.EMV_CARD_PIN_DATA.CardPinBlock)
            message.set(123, posDataCode)
            message.set(60, "010083K16395448041MeterNumber=12.87001585.Acct=${transaction.agentPhoneNumber}.${transaction.userPhoneNumber}")
            message.set(62, "00698WD0101333${transaction.userPhoneNumber}")
        } else {
            message.set(123, posDataCode)
            message.set(60, "010083K16395448041MeterNumber=12.87001585.Acct=${transaction.agentPhoneNumber}.${transaction.userPhoneNumber}")
            message.set(62, "00698WD0101333${transaction.userPhoneNumber}")
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
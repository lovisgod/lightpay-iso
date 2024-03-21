package com.lovisgod.lightpayiso.services.KeyHandler

import com.lovisgod.lightpayiso.data.IsoMessageBuilderJ8583
import com.lovisgod.lightpayiso.data.IsoMessageBuilderUp
import com.lovisgod.lightpayiso.data.constants.Constants
import com.lovisgod.lightpayiso.data.constants.Processor
import com.lovisgod.lightpayiso.data.db.KeyService
import com.lovisgod.lightpayiso.data.models.KeyResponse
import com.lovisgod.lightpayiso.data.models.Keymodel
import com.lovisgod.lightpayiso.data.models.TerminalInfo
import com.lovisgod.lightpayiso.utild.HexUtil
import jakarta.annotation.PostConstruct

import org.springframework.beans.factory.annotation.Autowired
import org.springframework.core.env.Environment
import org.springframework.stereotype.Component

@Component
class LocalKeyHandler {

    @Autowired
    lateinit var keyGenerator: KeyGenerator

    @Autowired
    private lateinit var environment: Environment

    @Autowired
    lateinit var service: KeyService



    fun initializeUpKeys(terminalId: String, processor: Processor) {
         if (processor == Processor.UP) {
            val mk = keyGenerator.generateMasterKey()
            val pk = keyGenerator.getPinKey(mk)
            val mkStr = HexUtil.toHexString(mk)
            val pkStr = HexUtil.toHexString(pk)
            val keyResponse = downloadKeysFromUP(terminalId)
            service.insertUserKeys(
                Keymodel(
                    TMK = keyResponse.masterKey,
                    TSK = keyResponse.sessionKey,
                    TPK = keyResponse.pinKey,
                    TERMINALID = terminalId,
                    LOCAL_TMK = mkStr,
                    LOCAL_TPK = pkStr,
                    LOCAL_TSK = keyResponse.sessionKey,
                    PROCESSOR = Processor.UP.name)
            )

            service.insertTerminalData(keyResponse.params as TerminalInfo)
        }
    }


    fun getKeys(terminalId: String, processor: Processor): KeyResponse {
        val mk = keyGenerator.generateMasterKey()
        val pk = keyGenerator.getPinKey(mk)
        val mkStr = HexUtil.toHexString(mk)
        val pkStr = HexUtil.toHexString(pk)

        // download keys from the processor and save in the db when successful

        if (processor == Processor.NIBSS) {
            val keyResponse = downloadKeysFromNibss(terminalId)
            service.insertUserKeys(
                Keymodel(
                TMK = keyResponse.masterKey,
                TSK = keyResponse.sessionKey,
                TPK = keyResponse.pinKey,
                TERMINALID = terminalId,
                LOCAL_TMK = mkStr,
                LOCAL_TPK = pkStr,
                LOCAL_TSK = keyResponse.sessionKey,
                PROCESSOR = Processor.NIBSS.name)
            )
            service.insertTerminalData(keyResponse.params as TerminalInfo)
            return KeyResponse(
                sessionKey = keyResponse.sessionKey,
                masterKey = mkStr,
                pinKey = pkStr,
                params =  keyResponse.params
            )

        } else if (processor == Processor.UP) {
            println("got here for download UP")
            val key  = service.getUserKeys(TERMINALID = terminalId, processor.name)
            val params = service.getTerminalDetails(terminalCode = terminalId)
            return KeyResponse(
                sessionKey = key.LOCAL_TSK,
                masterKey = key.LOCAL_TMK,
                pinKey = key.LOCAL_TPK,
                params =  params
            )
        } else {
            return KeyResponse(
                sessionKey = "",
                masterKey = "",
                pinKey = "",
                params =  null
            )
        }

    }

    fun downloadKeysFromNibss(terminalId: String): KeyResponse {
        val isoHelper = IsoMessageBuilderJ8583()
        var pinkKey: Any = ""
        var sessionKey: Any = ""
        var param: Any = TerminalInfo()
        var masterKey = isoHelper.generateKeyDownloadMessage(
            processCode = Constants.TMK,
            terminalId = terminalId,
            key = Constants.productionCMS
        )

        if (masterKey != "no key") {
            sessionKey = isoHelper.generateKeyDownloadMessage(
                processCode = Constants.TSK,
                terminalId = terminalId,
                key = masterKey.toString()
            )
            println("sessionKey is => ::: ${sessionKey}")
        }

        if (sessionKey != "no key") {
            pinkKey = isoHelper.generateKeyDownloadMessage(
                processCode = Constants.TPK,
                terminalId = terminalId,
                key = masterKey.toString()
            )
            println("pinkey is => ::: ${pinkKey}")
        }
        if (sessionKey != "no key") {
            param = downloadParameter(terminalId = terminalId, sessionKey = sessionKey.toString())
        }

        return KeyResponse(
            sessionKey = sessionKey.toString(),
            masterKey = masterKey.toString(),
            pinKey = pinkKey.toString(),
            params =  param
        )
    }

    fun downloadKeysFromUP(terminalId: String): KeyResponse {
        val isoHelper = IsoMessageBuilderUp()
        var pinkKey: Any = ""
        var sessionKey: Any = ""
        var param: Any = TerminalInfo()
        var masterKey = isoHelper.generateKeyDownloadMessage(
            processCode = Constants.TMK,
            terminalId = terminalId,
            key = environment.getProperty("up.ctmk").toString()
        )

        if (masterKey != "no key") {
            sessionKey = isoHelper.generateKeyDownloadMessage(
                processCode = Constants.TSK,
                terminalId = terminalId,
                key = masterKey.toString()
            )
//			println("sessionKey is => ::: ${sessionKey}")
        }

        if (sessionKey != "no key") {
            pinkKey = isoHelper.generateKeyDownloadMessage(
                processCode = Constants.TPK,
                terminalId = terminalId,
                key = masterKey.toString()
            )
//			println("pinkey is => ::: ${pinkKey}")
        }

        if (sessionKey != "no key") {
            param = downloadParameterUp(terminalId = terminalId, sessionKey = sessionKey.toString())
        }

        return KeyResponse(
            sessionKey = sessionKey.toString(),
            masterKey = masterKey.toString(),
            pinKey = pinkKey.toString(),
            params =  param
        )
    }

    fun downloadParameterUp(
         terminalId: String,
         sessionKey: String): Any {
        val isoHelper = IsoMessageBuilderUp()

        var parameter = isoHelper.downloadTerminalParam(
            processCode = Constants.NIBSS_PARAMETER,
            terminalId = terminalId,
            key = sessionKey
        )


        return parameter

    }

    fun downloadParameter(terminalId: String, sessionKey: String): Any {
        val isoHelper = IsoMessageBuilderJ8583()

        var parameter = isoHelper.downloadTerminalParam(
            processCode = Constants.NIBSS_PARAMETER,
            terminalId = terminalId,
            key = sessionKey
        )

        return parameter
    }

    fun testKeys(pinkey: String, master: String) {

        val cleartmk = environment.getProperty("local.tmk").toString()

        val plainText = "123456789ABCDE12"
        val decryptedPinKeyWithMk = HexUtil.toHexString(keyGenerator.decrypt(HexUtil.parseHex(master), HexUtil.parseHex(pinkey)))
        println("decryptedPinKeyWithMk::: ${decryptedPinKeyWithMk}")
        val encryptedWithClearPinKey = HexUtil.toHexString(keyGenerator.encryptWithTmk(HexUtil.parseHex(plainText), HexUtil.parseHex(decryptedPinKeyWithMk)))
        println("encryptedWithClearPinKey::: ${encryptedWithClearPinKey}")
        val decryptedPinClearText = HexUtil.toHexString(keyGenerator.decrypt(HexUtil.parseHex(decryptedPinKeyWithMk), HexUtil.parseHex(encryptedWithClearPinKey)))
        println("decryptedPinClearText::: ${decryptedPinClearText}")
    }
}
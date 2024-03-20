package com.lovisgod.lightpayiso.services.KeyHandler

import com.lovisgod.lightpayiso.data.constants.Processor
import com.lovisgod.lightpayiso.data.models.Keymodel
import com.lovisgod.lightpayiso.utild.HexUtil
import com.lovisgod.lightpayiso.utild.IsoUtils
import com.lovisgod.lightpayiso.utild.TripleDES
import org.bouncycastle.util.encoders.UTF8
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.core.env.Environment
import org.springframework.stereotype.Component
import java.nio.charset.Charset

@Component
class LocalKeyHandler {

    @Autowired
    lateinit var keyGenerator: KeyGenerator

    @Autowired
    private lateinit var environment: Environment


    fun getKeys(terminalId: String, processor: Processor): Keymodel {
        val mk = keyGenerator.generateMasterKey()
        val pk = keyGenerator.getPinKey(mk)
        val mkStr = HexUtil.toHexString(mk)
        val pkStr = HexUtil.toHexString(pk)
        return Keymodel(TERMINALID = terminalId, TMK = mkStr, TPK = pkStr, TSK = "")
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
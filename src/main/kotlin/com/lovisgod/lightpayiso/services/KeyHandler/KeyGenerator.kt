package com.lovisgod.lightpayiso.services.KeyHandler

import com.lovisgod.lightpayiso.utild.HexUtil
import com.lovisgod.lightpayiso.utild.IsoUtils.hexToBytes
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.core.env.Environment
import org.springframework.stereotype.Component
import java.security.Key
import javax.crypto.Cipher
import javax.crypto.SecretKey
import javax.crypto.SecretKeyFactory
import javax.crypto.spec.DESKeySpec
import javax.crypto.spec.SecretKeySpec

@Component
class KeyGenerator {

    @Autowired
    private lateinit var environment: Environment


    fun getPinKey(masterKey: ByteArray): ByteArray {
        // Generate a PIN key
        val pinKey = generateKey()

        println("Generated PIN Key: ${HexUtil.toHexString(pinKey)}")

        // Encrypt PIN key using  TMK
        val encryptedPinKey = encryptWithTmk(pinKey, masterKey)


        return encryptedPinKey
    }

    fun generateMasterKey(): ByteArray {
        val key = generateKey()

        // Assume clear TMK bytes are loaded from somewhere, for example, a file or a secure storage
        val cleartmk = environment.getProperty("local.tmk").toString()

        val clearTmkBytes = hexToBytes(cleartmk)

        // Encrypt  key using clear TMK
        val encryptedPinKey = encryptWithTmk(key, clearTmkBytes)

        return encryptedPinKey
    }

    fun generateKey(): ByteArray {
        // Generate 16 bytes random key for DES
        val keyBytes = ByteArray(16)
        val random = java.security.SecureRandom()
        random.nextBytes(keyBytes)

        // Return the SecretKey
        return keyBytes
    }

    fun encryptWithTmk(pinKey: ByteArray, tmkBytes: ByteArray): ByteArray {
        // Create a Cipher instance for encryption
        val cipher = Cipher.getInstance("DESede/ECB/NoPadding")

        val keyBytes = ByteArray(24)
        System.arraycopy(tmkBytes, 0, keyBytes, 0, 16)
        System.arraycopy(tmkBytes, 0, keyBytes, 16, 8)

        // Convert the TMK bytes into a SecretKey object
        val tmkSpec = SecretKeySpec(keyBytes, "DESede")

        // Initialize the Cipher with the TMK for encryption
        cipher.init(Cipher.ENCRYPT_MODE, tmkSpec)

        // Encrypt the PIN key
        return cipher.doFinal(pinKey)
    }

    fun decrypt(key: ByteArray, encrypted: ByteArray): ByteArray {
        // Create a Cipher instance for encryption
        val cipher = Cipher.getInstance("DESede/ECB/NoPadding")

        val keyBytes = ByteArray(24)
        System.arraycopy(key, 0, keyBytes, 0, 16)
        System.arraycopy(key, 0, keyBytes, 16, 8)

        // Convert the TMK bytes into a SecretKey object
        val tmkSpec = SecretKeySpec(keyBytes, "DESede")

        // Initialize the Cipher with the TMK for encryption
        cipher.init(Cipher.DECRYPT_MODE, tmkSpec)

        // Encrypt the PIN key
        return cipher.doFinal(encrypted)
    }

}
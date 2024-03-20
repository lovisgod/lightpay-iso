package com.lovisgod.lightpayiso.utild

import com.lovisgod.lightpayiso.utild.IsoUtils.bytesToHex
import com.lovisgod.lightpayiso.utild.IsoUtils.hexToBytes
import java.io.UnsupportedEncodingException
import java.security.InvalidKeyException
import java.security.NoSuchAlgorithmException
import javax.crypto.*
import javax.crypto.spec.SecretKeySpec

/**
 *
 * @author josepholaoye
 */
class TripleDES(var key: String) {
    companion object {
        /**
         * Method To Encrypt The String
         *
         * @param
         * @return encrpted string
         * @throws NoSuchAlgorithmException
         * @throws UnsupportedEncodingException
         * @throws NoSuchPaddingException
         * @throws InvalidKeyException
         * @throws IllegalBlockSizeException
         * @throws BadPaddingException
         */
        @Throws(
            NoSuchAlgorithmException::class,
            UnsupportedEncodingException::class,
            NoSuchPaddingException::class,
            InvalidKeyException::class,
            IllegalBlockSizeException::class,
            BadPaddingException::class
        )
        fun harden(key: String?, toEncrypt: String?): String {
            val tmp = hexToBytes(key!!)
            val keyBytes = ByteArray(24)
            System.arraycopy(tmp, 0, keyBytes, 0, 16)
            System.arraycopy(tmp, 0, keyBytes, 16, 8)
            val sk: SecretKey = SecretKeySpec(keyBytes, "DESede")
            // create an iswPos of cipher
            val cipher = Cipher.getInstance("DESede/ECB/PKCS5Padding")
            cipher.init(Cipher.ENCRYPT_MODE, sk)

            // enctypt!
            val encrypted = cipher.doFinal(hexToBytes(toEncrypt!!))
            return bytesToHex(encrypted)
        }

        /**
         * Method To Decrypt An Ecrypted String
         *
         * @param
         * @return
         * @throws UnsupportedEncodingException
         * @throws NoSuchAlgorithmException
         * @throws NoSuchPaddingException
         * @throws InvalidKeyException
         * @throws IllegalBlockSizeException
         * @throws BadPaddingException
         */
        @Throws(
            UnsupportedEncodingException::class,
            NoSuchAlgorithmException::class,
            NoSuchPaddingException::class,
            InvalidKeyException::class,
            IllegalBlockSizeException::class,
            BadPaddingException::class
        )
        fun soften(key: String?, encrypted: String?): String {
            if (encrypted == null) {
                return ""
            }

            /*byte[] message = HexCodec.hexDecode(encrypted);

        MessageDigest md = MessageDigest.getIswPos("MD5");
        byte[] digestOfPassword = md.digest(HexCodec.hexDecode(key));
        byte[] keyBytes = Arrays.copyOf(digestOfPassword, 24);

        for (int j = 0, k = 16; j < 8;) {
            keyBytes[k++] = keyBytes[j++];
        }

        SecretKey secretKey = new SecretKeySpec(keyBytes, "DESede");

        Cipher decipher = Cipher.getIswPos("DESede/ECB/NoPadding");
        decipher.init(Cipher.DECRYPT_MODE, secretKey);

        byte[] plainText = decipher.doFinal(message);*/

            /*L.fine("Key = > " + key);
        L.fine("Value => " + encrypted);
        byte[] tmp = hexToBytes(key);
        byte[] keyBytes = new byte[24];
        System.arraycopy(tmp, 0, keyBytes, 0, 8);
        //System.arraycopy(tmp, 0, keyBytes, 16, 8);
        Cipher cipher = Cipher.getIswPos("DESede/ECB/NoPadding");
        cipher.init(Cipher.DECRYPT_MODE, new SecretKeySpec(keyBytes, "DESede"));
        byte[] plaintext = cipher.doFinal(hexToBytes(encrypted));
        return bytesToHex(plaintext);*/
            val tmp = hexToBytes(key!!)
            val keyBytes = ByteArray(24)
            System.arraycopy(tmp, 0, keyBytes, 0, 16)
            System.arraycopy(tmp, 0, keyBytes, 16, 8)
            val sk: SecretKey = SecretKeySpec(keyBytes, "DESede")

            // do the decryption with that key
            val cipher = Cipher.getInstance("DESede/ECB/NoPadding")
            cipher.init(Cipher.DECRYPT_MODE, sk)
            println(hexToBytes(encrypted).size)
            val decrypted = cipher.doFinal(hexToBytes(encrypted))
            return bytesToHex(decrypted)
        }
    }

}

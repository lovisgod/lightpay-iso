package com.lovisgod.lightpayiso.utild

import java.io.IOException
import java.io.InputStream

object FileUtils {

     fun getFromAssets(fileName: String): ByteArray? {
//        var ins: InputStream? = null
        try {
            val ins = javaClass.getResourceAsStream(fileName)
            println(ins)
            val length = ins.available()
            val buffer = ByteArray(length)
            ins.read(buffer)
            return buffer
        } catch (e: Exception) {
            e.printStackTrace();
        } finally {
//            if (ins != null) {
//                try {
//                    ins.close()
//                } catch (e: IOException) {
//                    e.printStackTrace()
//                }
//            }
        }
        return null
    }
}
package com.lovisgod.lightpayiso.data.constants

import java.util.*
import java.util.prefs.Preferences

object Constants {

    const val SRCI = 53
    val Prefs = Preferences.userRoot().node("com.lovisgod.lightpayiso")
     val ISW_TERMINAL_IP_CTMS = "196.6.103.10"
    val  ISW_TERMINAL_PORT_CTMS = 55533

//    val UP_IP = "196.46.20.30"
//    val UP_PORT = 5334

    val UP_IP = "196.46.20.85"
    val UP_PORT = 5453


    val ISW_TERMINAL_IP_CTMS_PROD = "196.6.103.18"
    val  ISW_TERMINAL_PORT_CTMS_PROD = 4008
    val  productionCMS = "3CDDE1CC6FDD225C9A8BC3EB065509A6"
    val testCMS = "DBEECACCB4210977ACE73A1D873CA59F"
    val UPCTMK = "3CDDE1CC6FDD225C9A8BC3EB065509A6"
    val TMK = "9A0000"
    val TPK = "9G0000"
    val TSK = "9B0000"
    val NIBSS_PARAMETER = "9C0000"
    val NIBSS_MASTERKEY = "NIBSS_MASTERKEY"
    val NIBSS_SESSIONKEY = "NIBSS_SESSIONKEY"
    val NIBSS_PINKEY = "NIBSS_PINKEY"
    const val TIMEOUT_CODE = "0x0x0"
    const val LOCAL_TMS_URL = "http://127.0.0.1:8085/ext/api/v1"
    const val PROD_TMS_URL = "https://payble-tms-1418295d2495.herokuapp.com/ext/api/v1"


    fun getCms(test: Boolean): String{
        return if (test) testCMS else productionCMS
    }

    fun getUPCTMK(test: Boolean): String {
        return if (test) UPCTMK else UPCTMK
    }

    fun getNextStan(): String {
        var stan = Prefs.getInt("STAN", 0)

        // compute and save new stan
        val newStan = if (stan > 999999) 0 else ++stan
        Prefs.putInt("STAN", newStan)

        return String.format(Locale.getDefault(), "%06d", newStan)
    }
}
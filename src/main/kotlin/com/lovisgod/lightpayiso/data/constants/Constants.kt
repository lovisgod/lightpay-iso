package com.lovisgod.lightpayiso.data.constants

import java.util.*
import java.util.prefs.Preferences

object Constants {

    const val SRCI = 53
    val Prefs = Preferences.userRoot().node("com.lovisgod.lightpayiso")
     val ISW_TERMINAL_IP_CTMS = "196.6.103.126"
    val  ISW_TERMINAL_PORT_CTMS = 55533
    val  productionCMS = "3CDDE1CC6FDD225C9A8BC3EB065509A6"
    val testCMS = "DBEECACCB4210977ACE73A1D873CA59F"
    val NIBSS_TMK = "9A0000"
    val NIBSS_TPK = "9G0000"
    val NIBSS_TSK = "9B0000"
    val NIBSS_PARAMETER = "9C0000"
    val NIBSS_MASTERKEY = "NIBSS_MASTERKEY"
    val NIBSS_SESSIONKEY = "NIBSS_SESSIONKEY"
    val NIBSS_PINKEY = "NIBSS_PINKEY"


    fun getCms(test: Boolean): String{
        return if (test) testCMS else productionCMS
    }

    fun getNextStan(): String {
        var stan = Prefs.getInt("STAN", 0)

        // compute and save new stan
        val newStan = if (stan > 999999) 0 else ++stan
        Prefs.putInt("STAN", newStan)

        return String.format(Locale.getDefault(), "%06d", newStan)
    }
}
package com.lovisgod.lightpayiso.data.models


import com.lovisgod.lightpayiso.data.constants.Constants
import jakarta.persistence.Entity
import jakarta.persistence.GeneratedValue
import jakarta.persistence.GenerationType
import jakarta.persistence.Id
import org.simpleframework.xml.*

@Root(name = "allTerminalInfo", strict = false)
data class AllTerminalInfo (
        @field:Element(name = "responseCode", required = false)
        var responseCode: String = "",

        @field:Element(name = "responseMessage", required = false)
        var responseMessage: String = "",

        @field:ElementList(name = "terminalAllowedTxTypes", inline=true, required = false)
//        var terminalAllowedTxTypes: List<TerminalAllowedTxTypes>? = null,
        var terminalAllowedTxTypes: List<String>? = null,

        @field:Element(name = "terminalInfoBySerials", required = false)
        var terminalInfo: TerminalInfo? = null,

        @field:Element(name = "tmsRouteTypeConfig", required = false)
        var tmsRouteTypeConfig: TmsRouteTypeConfig? = null
)

@Entity(name = "Terminalinfo")
@Root(name = "terminalInfoBySerials", strict = false)
data class TerminalInfo(
        @Id
        @GeneratedValue(strategy = GenerationType.IDENTITY)
        var id: Long? = 0,

        @field:Element(name = "terminalCode", required = false)
        var terminalCode: String = "Terminal",

        @field:Element(name = "cardAcceptorId", required = false)
        var cardAcceptorId: String = "000000000000000",

        @field:Element(name = "merchantId", required = false)
        var merchantId: String = "000000000000000",

        @field:Element(name = "merchantName", required = false)
        var merchantName: String = "Railway Operator",

        @field:Element(name = "merchantAddress1", required = false)
        var merchantAddress1: String = "",

        @field:Element(name = "merchantAddress2", required = false)
        var merchantAddress2: String = "",

        @field:Element(name = "merchantPhoneNumber", required = false)
        var merchantPhoneNumber: String = "08000000000",

        @field:Element(name = "merchantEmail", required = false)
        var merchantEmail: String = "sample@interswitchng.com",

        @field:Element(name = "merchantState", required = false)
        var merchantState: String = "Lagos state",

        @field:Element(name = "tmsRouteType", required = false)
        var tmsRouteType: String = "",

        @field:Element(name = "merchantCity", required = false)
        var merchantCity: String = "Lagos",

        @field:Element(name = "qtbMerchantCode", required = false)
        var qtbMerchantCode: String? = "MX1065",

        @field:Element(name = "qtbMerchantAlias", required = false)
        var qtbMerchantAlias: String? = "002208",

        @field:Element(name = "cardAcceptorNameLocation", required = false)
        var cardAcceptorNameLocation: String = "",

        @field:Element(name = "merchantCategoryCode", required = false)
        var merchantCategoryCode: String = "5411",

        var  terminalCountryCode: String = "0566",
        var  transCurrencyCode: String = "0566",
        var transCurrencyExp: String = "02",
        var terminalType: String = "22",
        var terminalCapabilities: String = "E0F8C8",
        var terminalExtCapabilities: String = "F000F0F001",
        var terminalEntryMode: String = "05",
        var nibbsKey: String = Constants.getCms(false),
        var upkey: String = Constants.getUPCTMK(false)
) {
        override fun toString(): String {
                """code: ${terminalCode}
                   capailty: ${terminalCapabilities}
                   name: ${merchantName}
                """.trimIndent()
                return super.toString()
        }
}

@Root(name = "terminalAllowedTxTypes", strict = false)
class TerminalAllowedTxTypes(
        @field:Element(name = "applicationDescription", required = false)
        var applicationDescription: String = ""
)

@Root(name = "tmsRouteTypeConfig", strict = false)
class TmsRouteTypeConfig(

        @field:Element(name = "port", required = false)
        var port: String = "${Constants.ISW_TERMINAL_IP_CTMS}",

        @field:Element(name = "ip", required = false)
        var ip: String = "${Constants.ISW_TERMINAL_PORT_CTMS}",

        @field:Element(name = "key", required = false)
        var key: String = "${Constants.getCms(false)}",

        @field:Element(name = "name", required = false)
        var name: String = "")



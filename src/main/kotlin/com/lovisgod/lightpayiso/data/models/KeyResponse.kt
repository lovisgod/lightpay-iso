package com.lovisgod.lightpayiso.data.models

data class KeyResponse(
        var sessionKey: String,
        var masterKey: String,
        var pinKey: String,
        var params: Any? = TerminalInfo()
)

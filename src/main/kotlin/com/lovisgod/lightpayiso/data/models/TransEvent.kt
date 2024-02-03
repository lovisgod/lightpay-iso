package com.lovisgod.lightpayiso.data.models

data class TransEvent(
    var name: String,
    var api_key: String = "",
    var merchant_id: String = "",
    var data: SubmitTransactionRequestBody
)

data class SampleEvent(
    var name: String,
    var api_key: String = "",
    var merchant_id: String = ""
)

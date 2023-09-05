package com.lovisgod.lightpayiso.data.models

data class ResponseObject (
        val statusCode: Int,
        var message: String,
        var data: Any?
)
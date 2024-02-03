package com.lovisgod.lightpayiso.utild

import com.lovisgod.lightpayiso.data.constants.Constants
import com.lovisgod.lightpayiso.data.models.KeyValidationResponse
import com.lovisgod.lightpayiso.data.models.SubmitTransactionBody
import com.lovisgod.lightpayiso.data.models.SubmitTransactionRequestBody
import java.net.URI
import java.net.http.HttpClient
import java.net.http.HttpRequest
import java.net.http.HttpResponse

class SubmitTransHelper {

    fun submitTransactionToLightPayTMS(data: SubmitTransactionBody, api_key: String,
                                       merchant_id: String): Boolean {
        try {
            val request: HttpRequest = HttpRequest.newBuilder()
                .uri(URI("${Constants.PROD_TMS_URL}/merchant/submit-payment"))
                .headers("api_key", api_key, "merchant_id", merchant_id)
                .POST(
                    HttpRequest.BodyPublishers.ofString(
                    ObjectMapper.convertObjectBackToJson(data)
                ))
                .build()

            val client   = HttpClient.newHttpClient()
            val response  = client.send(request, HttpResponse.BodyHandlers.ofString())
            println(response.body())
            val responseObject = ObjectMapper.convertHttpResponse<KeyValidationResponse>(response.body())


            return if (responseObject.status == "success") true else false
        } catch (e: Exception) {
            e.printStackTrace()
            return false
        }
    }
}
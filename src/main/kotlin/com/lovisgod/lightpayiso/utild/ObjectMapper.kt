package com.lovisgod.lightpayiso.utild;

import com.google.gson.Gson
import com.google.gson.reflect.TypeToken

object ObjectMapper {
    inline fun < reified T> convertHttpResponse(response: String): T {
        val gson = Gson()
        val responseType = object : TypeToken<T>() {}.type
        val converted = gson.fromJson<T>(response, responseType)

        return converted
    }
}
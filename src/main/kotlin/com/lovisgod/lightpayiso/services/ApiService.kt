package com.lovisgod.lightpayiso.services

import com.lovisgod.lightpayiso.data.models.ResponseObject
import org.springframework.scheduling.annotation.Async
import org.springframework.stereotype.Service
import java.util.concurrent.CompletableFuture




@Service
class ApiService {
    @Async
    fun handleTestRequestAsync(): CompletableFuture<ResponseObject> {
        for (i in 1..10000) print("$i\n")
        val result = ResponseObject(statusCode = 200, message = "returning value from bomb testingg", data = null)
        return CompletableFuture.completedFuture(result)
    }
}
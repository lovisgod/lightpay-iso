package com.lovisgod.lightpayiso.utild.events

import com.lovisgod.lightpayiso.utild.SubmitTransHelper
import org.springframework.context.ApplicationListener
import org.springframework.stereotype.Component


@Component
internal class SubmitTransEventListener : ApplicationListener<SubmitTransEvent> {
    override fun onApplicationEvent(event: SubmitTransEvent) {
      if (event.getEvent().name == "SUBTRANS") {
        SubmitTransHelper().submitTransactionToLightPayTMS(
            data = event.getEvent().data,
            api_key = event.getEvent().api_key,
            merchant_id = event.getEvent().merchant_id
        )
      }
    }
}
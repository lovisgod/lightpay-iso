package com.lovisgod.lightpayiso.utild.events

import com.lovisgod.lightpayiso.data.models.SampleEvent
import com.lovisgod.lightpayiso.data.models.TransEvent
import org.springframework.context.ApplicationEvent

class SubmitTransEvent internal constructor(
    source: Any?,
    private val event: TransEvent) : ApplicationEvent(

    source!!
) {
        fun getEvent(): TransEvent {

            return this.event
        }
    }


class SubmitSampleEvent internal constructor(
    source: Any?,
    private val event: SampleEvent) : ApplicationEvent(

    source!!
) {
    fun getEvent(): SampleEvent {

        return this.event
    }
}



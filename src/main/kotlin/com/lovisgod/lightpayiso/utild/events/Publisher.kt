package com.lovisgod.lightpayiso.utild.events

import com.lovisgod.lightpayiso.data.models.TransEvent
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.context.ApplicationEventPublisher
import org.springframework.stereotype.Component


@Component
class Publisher @Autowired constructor( val publisher: ApplicationEventPublisher) {
    fun publishSubmitEvent(data: TransEvent) {
        // Publishing event created by extending ApplicationEvent
        publisher.publishEvent(SubmitTransEvent(this, data))
    }
}
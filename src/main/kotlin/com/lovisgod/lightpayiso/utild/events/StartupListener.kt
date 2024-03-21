package com.lovisgod.lightpayiso.utild.events

import com.lovisgod.lightpayiso.data.constants.Processor
import com.lovisgod.lightpayiso.services.KeyHandler.LocalKeyHandler
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.context.event.ContextRefreshedEvent
import org.springframework.context.event.EventListener
import org.springframework.core.env.Environment
import org.springframework.stereotype.Component
import java.util.logging.Logger


@Component
class StartupListener {

    @Autowired
    lateinit var localKeyHandler: LocalKeyHandler

    @Autowired
    lateinit var environment: Environment

    @EventListener
    fun onApplicationEvent(event: ContextRefreshedEvent?) {
        LOG.info("download terminal key and params for UP and initialize the tables")
        localKeyHandler.initializeUpKeys(environment.getProperty("up.terminalid").toString(), processor = Processor.UP)
    }

    companion object {
        private val LOG: Logger = Logger.getLogger(StartupListener::class.java.toString())

    }
}
package com.lovisgod.lightpayiso.data.models

import com.lovisgod.lightpayiso.data.constants.Processor
import jakarta.persistence.Entity
import jakarta.persistence.GeneratedValue
import jakarta.persistence.GenerationType
import jakarta.persistence.Id

@Entity(name = "Keymodel")
data class Keymodel(
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    var id: Long? = 0,
    var TMK: String = "",
    var TPK: String = "",
    var TSK: String = "",
    var TERMINALID: String = "",
    var PROCESSOR: String = "",
    var LOCAL_TMK: String = "",
    var LOCAL_TPK: String = "",
    var LOCAL_TSK: String = "",
)

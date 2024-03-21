package com.lovisgod.lightpayiso.data.db

import com.lovisgod.lightpayiso.data.models.Keymodel
import com.lovisgod.lightpayiso.data.models.TerminalInfo
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Query
import org.springframework.stereotype.Repository


@Repository
interface KeyDao: JpaRepository<Keymodel, Long> {
    @Query("SELECT t FROM Keymodel t WHERE t.TERMINALID = :TERMINALID AND t.PROCESSOR = :PROCESSOR")
    fun getUserKeys(TERMINALID: String?, PROCESSOR: String): Keymodel?
}

@Repository
interface ParameterDao: JpaRepository<TerminalInfo, Long> {
    @Query("SELECT t FROM Terminalinfo t WHERE t.terminalCode = :TERMINALID")
    fun getTerminalDetails(TERMINALID: String?): TerminalInfo?
}


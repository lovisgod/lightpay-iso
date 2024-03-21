package com.lovisgod.lightpayiso.data.db

import com.lovisgod.lightpayiso.data.models.Keymodel
import com.lovisgod.lightpayiso.data.models.TerminalInfo
import org.springframework.stereotype.Service

@Service
class KeyService(var keyDao: KeyDao, var parameterDao: ParameterDao) {

    fun insertUserKeys(data: Keymodel) {
      try {
          keyDao.save(data)
      } catch (e: Exception) {
          e.printStackTrace()
      }
    }

    fun getUserKeys(TERMINALID: String?, PROCESSOR: String): Keymodel {
        println("got here for download UP 2")
        val key  = keyDao.getUserKeys(TERMINALID, PROCESSOR)
        return Keymodel(
            TMK = key?.TMK ?: "",
            TPK = key?.TPK ?: "",
            TSK = key?.TSK ?: "",
            LOCAL_TSK = key?.LOCAL_TSK ?: "",
            LOCAL_TMK = key?.LOCAL_TMK ?: "",
            LOCAL_TPK = key?.LOCAL_TPK ?: "",
            TERMINALID = TERMINALID ?: "",
            PROCESSOR = PROCESSOR
        )
    }

    fun insertTerminalData(data: TerminalInfo) {
        try {
            parameterDao.save(data)
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }


    fun getTerminalDetails(terminalCode: String?): TerminalInfo {
        return parameterDao.getTerminalDetails(terminalCode) ?: TerminalInfo()
    }
}
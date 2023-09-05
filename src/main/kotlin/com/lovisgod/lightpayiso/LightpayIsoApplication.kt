package com.lovisgod.lightpayiso

import com.lovisgod.lightpayiso.data.IsoMessageBuilder
import com.lovisgod.lightpayiso.data.constants.Constants
import com.lovisgod.lightpayiso.data.models.KeyResponse
import com.lovisgod.lightpayiso.data.models.ResponseObject
import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.runApplication
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController

@SpringBootApplication
@RestController
class LightpayIsoApplication {

	@GetMapping("/health")
	fun checkHealth(): Any {
		return ResponseObject(statusCode = 200, message = "Service is healthy", data = null)
	}

	@GetMapping("/get-nibss-keys")
	fun downloadAllNibssKey(@RequestParam(value = "terminalId") terminalId: String): Any {
		val isoHelper = IsoMessageBuilder()
		var pinkKey: Any = ""
		var sessionKey: Any = ""
		var masterKey = isoHelper.generateKeyDownloadMessage(
			processCode = Constants.NIBSS_TMK,
			terminalId = terminalId,
			key = Constants.testCMS
		)

		if (masterKey != "no key") {
			sessionKey = isoHelper.generateKeyDownloadMessage(
				processCode = Constants.NIBSS_TSK,
				terminalId = terminalId,
				key = masterKey.toString()
			)
			println("sessionKey is => ::: ${sessionKey}")
		}

		if (sessionKey != "no key") {
			pinkKey = isoHelper.generateKeyDownloadMessage(
				processCode = Constants.NIBSS_TPK,
				terminalId = terminalId,
				key = sessionKey.toString()
			)
			println("pinkey is => ::: ${pinkKey}")
		}
		return ResponseObject(
			statusCode = 200,
			message = "terminal key details",
			data = KeyResponse(
				sessionKey = sessionKey.toString(),
				masterKey = masterKey.toString(),
				pinKey = pinkKey.toString()
			)
		)

	}

	@GetMapping("/get-nibss-terminal-param")
	fun downloadParameter(
		@RequestParam(value = "terminalId") terminalId: String,
		@RequestParam(value = "sessionKey") sessionKey: String): Any {
		val isoHelper = IsoMessageBuilder()

		var parameter = isoHelper.downloadTerminalParam(
			processCode = Constants.NIBSS_PARAMETER,
			terminalId = terminalId,
			key = sessionKey
		)


		return ResponseObject(
			statusCode = 200,
			message = "terminal parameter",
			data = parameter
		)

	}
}

fun main(args: Array<String>) {
	runApplication<LightpayIsoApplication>(*args)
}



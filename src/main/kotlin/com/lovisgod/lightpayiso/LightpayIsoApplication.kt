package com.lovisgod.lightpayiso

import com.lovisgod.lightpayiso.data.IsoMessageBuilder
import com.lovisgod.lightpayiso.data.IsoMessageBuilderUp
import com.lovisgod.lightpayiso.data.constants.Constants
import com.lovisgod.lightpayiso.data.models.*
import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.runApplication
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestHeader
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
			processCode = Constants.TMK,
			terminalId = terminalId,
				key = Constants.testCMS
		)

		if (masterKey != "no key") {
			sessionKey = isoHelper.generateKeyDownloadMessage(
				processCode = Constants.TSK,
				terminalId = terminalId,
				key = masterKey.toString()
			)
			println("sessionKey is => ::: ${sessionKey}")
		}

		if (sessionKey != "no key") {
			pinkKey = isoHelper.generateKeyDownloadMessage(
				processCode = Constants.TPK,
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

	@GetMapping("/get-up-keys")
	fun downloadAllupKey(@RequestParam(value = "terminalId") terminalId: String): Any {
		val isoHelper = IsoMessageBuilderUp()
		var pinkKey: Any = ""
		var sessionKey: Any = ""
		var masterKey = isoHelper.generateKeyDownloadMessage(
			processCode = Constants.TMK,
			terminalId = terminalId,
			key = Constants.UPCTMK
		)

		if (masterKey != "no key") {
			sessionKey = isoHelper.generateKeyDownloadMessage(
				processCode = Constants.TSK,
				terminalId = terminalId,
				key = masterKey.toString()
			)
			println("sessionKey is => ::: ${sessionKey}")
		}

		if (sessionKey != "no key") {
			pinkKey = isoHelper.generateKeyDownloadMessage(
				processCode = Constants.TPK,
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

	@GetMapping("/get-up-terminal-param")
	fun downloadParameterUp(
		@RequestParam(value = "terminalId") terminalId: String,
		@RequestParam(value = "sessionKey") sessionKey: String): Any {
		val isoHelper = IsoMessageBuilderUp()

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

	@PostMapping("/perform-cashout-transaction")
	fun performCashout(
		@RequestHeader(value = "sskey") sskey: String,
		@RequestBody(required = true) transactionRequest: TransactionRequest): Any {
		val isoHelper = IsoMessageBuilderUp()

		var terminalInfo = TerminalInfo().copy(
			merchantCategoryCode = transactionRequest.merchantCategoryCode.toString(),
			terminalCode =transactionRequest.terminalCode.toString(),
			merchantName = transactionRequest.merchantName.toString(),
			merchantId = transactionRequest.merchantId.toString()
		)
		var transactionInfo = RequestIccData().apply {
				this.haspin = transactionRequest.haspin?.equals(true)
				this.TRACK_2_DATA = transactionRequest.track2Data.toString()
				this.APP_PAN_SEQUENCE_NUMBER = transactionRequest.panSequenceNumber.toString()
				this.TRANSACTION_AMOUNT = transactionRequest.amount.toString()
				this.EMV_CARD_PIN_DATA.CardPinBlock = transactionRequest.pinBlock.toString()
		}

		val response  = isoHelper.getCashoutRequest(
			iccString = transactionRequest.iccString.toString(),
			terminalInfo = terminalInfo,
			transaction = transactionInfo,
			accountType = AccountType.Default,
			posDataCode = transactionRequest.posDataCode.toString(),
			sessionKey = sskey
		)
		return ResponseObject(
			statusCode = 200,
			message = "terminal transaction",
			data = response
		)

	}
}


fun main(args: Array<String>) {
	runApplication<LightpayIsoApplication>(*args)
}



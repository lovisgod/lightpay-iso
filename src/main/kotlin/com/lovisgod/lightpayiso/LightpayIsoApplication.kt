package com.lovisgod.lightpayiso

import com.lovisgod.lightpayiso.data.IsoMessageBuilder
import com.lovisgod.lightpayiso.data.IsoMessageBuilderUp
import com.lovisgod.lightpayiso.data.constants.Constants
import com.lovisgod.lightpayiso.data.models.*
import com.lovisgod.lightpayiso.utild.ObjectMapper
import com.sun.net.httpserver.Headers
import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.runApplication
import org.springframework.web.bind.annotation.*
import java.net.URI
import java.net.http.HttpClient
import java.net.http.HttpRequest
import java.net.http.HttpResponse
import java.net.http.HttpResponse.BodyHandlers

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
		var param: Any = TerminalInfo()
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
				key = masterKey.toString()
			)
			println("pinkey is => ::: ${pinkKey}")
		}
		if (sessionKey != "no key") {
			param = downloadParameter(terminalId = terminalId, sessionKey = sessionKey.toString())
		}
		return ResponseObject(
			statusCode = 200,
			message = "terminal key details",
			data = KeyResponse(
				sessionKey = sessionKey.toString(),
				masterKey = masterKey.toString(),
				pinKey = pinkKey.toString(),
				params = param
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

	@GetMapping("/get-up-key")
	fun downloadAllupKey(@RequestParam(value = "terminalId") terminalId: String): Any {
		val isoHelper = IsoMessageBuilderUp()
		var pinkKey: Any = ""
		var sessionKey: Any = ""
		var param: Any = TerminalInfo()
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
				key = masterKey.toString()
			)
			println("pinkey is => ::: ${pinkKey}")
		}

		if (sessionKey != "no key") {
			 param = downloadParameterUp(terminalId = terminalId, sessionKey = sessionKey.toString())
		}
		return ResponseObject(
			statusCode = 200,
			message = "terminal key details",
			data = KeyResponse(
				sessionKey = sessionKey.toString(),
				masterKey = masterKey.toString(),
				pinKey = pinkKey.toString(),
				params = param
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
		@RequestHeader(value = "api_key") api_key: String,
		@RequestHeader(value = "merchant_id") merchant_id: String,
		@RequestBody(required = true) transactionRequest: TransactionRequest): Any {

		if (!validateKeys(api_key, merchant_id)) return ResponseObject(
			statusCode = 401,
			message = "Not Authorized",
			data = null
		)
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


	@PostMapping("/perform-purchase-transaction")
	fun performPurchase(
		@RequestHeader(value = "sskey") sskey: String,
		@RequestHeader(value = "api_key") api_key: String,
		@RequestHeader(value = "merchant_id") merchant_id: String,
		@RequestBody(required = true) transactionRequest: TransactionRequest): Any {


		if (!validateKeys(api_key, merchant_id)) return ResponseObject(
			statusCode = 401,
			message = "Not Authorized",
			data = null
		)

//		if (transactionRequest.amount?.toInt()!! > 200000) {
//			return performCashout(sskey, api_key, merchant_id, transactionRequest)
//		} else {
			val isoHelper = IsoMessageBuilder()

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

			val response  = isoHelper.getPurchaseRequest(
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
//		}
	}

	fun validateKeys(api_key: String, merchant_id: String): Boolean {
		try {
			val request: HttpRequest = HttpRequest.newBuilder()
				.uri(URI("${Constants.PROD_TMS_URL}/merchant/validate-key"))
				.headers("api_key", api_key, "merchant_id", merchant_id)
				.GET()
				.build()

			val client   = HttpClient.newHttpClient()
			val response  = client.send(request, BodyHandlers.ofString())
			println(response.body())
			val responseObject = ObjectMapper.convertHttpResponse<KeyValidationResponse>(response.body())


			return if (responseObject.status == "success") true else false
		} catch (e: Exception) {
			e.printStackTrace()
			return false
		}
	}
}



fun main(args: Array<String>) {
	runApplication<LightpayIsoApplication>(*args)
}



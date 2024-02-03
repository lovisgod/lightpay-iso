package com.lovisgod.lightpayiso

import com.lovisgod.lightpayiso.data.IsoMessageBuilderJ8583
import com.lovisgod.lightpayiso.data.IsoMessageBuilderUp
import com.lovisgod.lightpayiso.data.constants.Constants
import com.lovisgod.lightpayiso.data.models.*
import com.lovisgod.lightpayiso.services.ApiService
import com.lovisgod.lightpayiso.utild.IsoUtils
import com.lovisgod.lightpayiso.utild.ObjectMapper
import com.lovisgod.lightpayiso.utild.events.Publisher
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.runApplication
import org.springframework.core.env.Environment
import org.springframework.scheduling.annotation.Async
import org.springframework.web.bind.annotation.*
import java.net.URI
import java.net.http.HttpClient
import java.net.http.HttpRequest
import java.net.http.HttpResponse.BodyHandlers
import java.util.concurrent.CompletableFuture

@SpringBootApplication
@RestController
class LightpayIsoApplication {

	@Autowired
	lateinit var  applicationEventPublisher: Publisher

	@Autowired
	lateinit var environment: Environment

	@Autowired
	lateinit var apiService: ApiService


	@GetMapping("/health")
	fun checkHealth(): Any {
		val event = SampleEvent(
			name ="SAMPLE_EVENT",
			api_key = "sample",
			merchant_id = "merchant id")
		println("got here for check health")

		applicationEventPublisher.testSampleEvent(event) // this is called on another thread
		return ResponseObject(statusCode = 200, message = "Service is healthy!!!", data = null) // this returned using the former thread
	}

	@GetMapping("/test-bombardment")
	fun testBomb(): CompletableFuture<Any> {

		return apiService.handleTestRequestAsync().thenApply {
			it
		}
	}

	@GetMapping("/get-nibss-keys")
	fun downloadAllNibssKey(
		@RequestParam(value = "terminalId") terminalId: String
	): Any {

		val isoHelper = IsoMessageBuilderJ8583()
		var pinkKey: Any = ""
		var sessionKey: Any = ""
		var param: Any = TerminalInfo()
		var masterKey = isoHelper.generateKeyDownloadMessage(
			processCode = Constants.TMK,
			terminalId = terminalId,
				key = Constants.productionCMS
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
		val isoHelper = IsoMessageBuilderJ8583()

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
//		println(environment.getProperty("up.ctmk"))
		val isoHelper = IsoMessageBuilderUp()
		var pinkKey: Any = ""
		var sessionKey: Any = ""
		var param: Any = TerminalInfo()
		var masterKey = isoHelper.generateKeyDownloadMessage(
			processCode = Constants.TMK,
			terminalId = terminalId,
			key = environment.getProperty("up.ctmk").toString()
		)

		if (masterKey != "no key") {
			sessionKey = isoHelper.generateKeyDownloadMessage(
				processCode = Constants.TSK,
				terminalId = terminalId,
				key = masterKey.toString()
			)
//			println("sessionKey is => ::: ${sessionKey}")
		}

		if (sessionKey != "no key") {
			pinkKey = isoHelper.generateKeyDownloadMessage(
				processCode = Constants.TPK,
				terminalId = terminalId,
				key = masterKey.toString()
			)
//			println("pinkey is => ::: ${pinkKey}")
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
		@RequestParam(value = "version", required = false) version: String?,
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

		if (!version.isNullOrEmpty() && version == "1") {
			val pan =  transactionInfo.TRACK_2_DATA.split("F")[0].split("D")[0]

			val data = SubmitTransactionRequestBody(
				description =  response.description,
				responseCode = response.responseCode,
				authCode = response.authCode,
				currencyCode = "566",
				amount = transactionRequest.amount.toString().trimStart('0'),
				masked_pan = IsoUtils.maskPan(pan),
				stan = response.stan,
				transactionRef = response.referenceNumber,
				referenceNumber = response.referenceNumber,
				date = response.transactionDate,
				scripts = "",
				transTYpe = "cashout",
				merchant_code = terminalInfo.merchantId,
				paymentType = "Card",
				terminal_id = terminalInfo.terminalCode,
				transRoute = "up",
				agent_transtype = transactionRequest.agentTransType

			)

			val event = TransEvent(
				name ="SUBTRANS",
				api_key = api_key,
				merchant_id = merchant_id,
				data = data)

			applicationEventPublisher.publishSubmitEvent(event)
		}

		return ResponseObject(
			statusCode = 200,
			message = "terminal transaction",
			data = response
		)

	}


	@PostMapping("/perform-payattitude-transaction")
	fun performPayAttitude(
		@RequestParam(value = "version", required = false) version: String?,
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
			this.agentPhoneNumber = transactionRequest.agentPhoneNumber.toString()
			this.userPhoneNumber = transactionRequest.userPhoneNumber.toString()
		}

		val response  = isoHelper.getPayAttitudeRequest(
			iccString = transactionRequest.iccString.toString(),
			terminalInfo = terminalInfo,
			transaction = transactionInfo,
			accountType = AccountType.Default,
			posDataCode = transactionRequest.posDataCode.toString(),
			sessionKey = sskey,
			field7 = transactionRequest.field7.toString(),
			field12 = transactionRequest.field12.toString()
		)
		val responseObject = ResponseObject(
			statusCode = 200,
			message = "terminal transaction",
			data = response
		)

		if (!version.isNullOrEmpty() && version == "1") {
			val data = SubmitTransactionRequestBody(
				description = "PayAttitude Payment",
				responseCode = "${response.responseCode}",
				authCode = response.authCode,
				currencyCode = "566",
				amount = transactionRequest.amount.toString().trimStart('0'),
				masked_pan = "",
				stan = response.stan,
				transactionRef = response.referenceNumber,
				referenceNumber = response.referenceNumber,
				date = response.transactionDate,
				scripts = "",
				transTYpe = "payment",
				merchant_code = terminalInfo.merchantId,
				paymentType = "payattitude",
				terminal_id = terminalInfo.terminalCode,
				transRoute = "up",
				agent_transtype = "push"

			)

			val event = TransEvent(
				name ="SUBTRANS",
				api_key = api_key,
				merchant_id = merchant_id,
				data = data)

			applicationEventPublisher.publishSubmitEvent(event)
		}

		return responseObject

	}


	@PostMapping("/perform-purchase-transaction")
	fun performPurchase(
		@RequestParam(value = "version", required = false) version: String?,
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
			val isoHelper = IsoMessageBuilderJ8583()

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

		if (!version.isNullOrEmpty() && version == "1") {
			val pan =  transactionInfo.TRACK_2_DATA.split("F")[0].split("D")[0]

			val data = SubmitTransactionRequestBody(
				description =  response.description,
				responseCode = response.responseCode,
				authCode = response.authCode,
				currencyCode = "566",
				amount = transactionRequest.amount.toString().trimStart('0'),
				masked_pan = IsoUtils.maskPan(pan),
				stan = response.stan,
				transactionRef = response.referenceNumber,
				referenceNumber = response.referenceNumber,
				date = response.transactionDate,
				scripts = "",
				transTYpe = "purchase",
				merchant_code = terminalInfo.merchantId,
				paymentType = "Card",
				terminal_id = terminalInfo.terminalCode,
				transRoute = "nibss",
				agent_transtype = transactionRequest.agentTransType

			)

			val event = TransEvent(
				name ="SUBTRANS",
				api_key = api_key,
				merchant_id = merchant_id,
				data = data)

			applicationEventPublisher.publishSubmitEvent(event)
		}


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



package com.lovisgod.lightpayiso.data.models

import org.simpleframework.xml.Element
import org.simpleframework.xml.Namespace
import org.simpleframework.xml.NamespaceList
import org.simpleframework.xml.Root


@Root(name = "submitTransactionRequestBody", strict = false)
@NamespaceList(
    Namespace( prefix = "ns2", reference = "http://interswitchng.com"),
    Namespace( prefix = "ns3", reference = "http://tempuri.org/ns.xsd")
)
data class SubmitTransactionRequestBody(

    @field:Element(name = "description", required = false)
    var description: String = "",

    @field:Element(name = "field39", required = false)
    var responseCode: String = "",

    @field:Element(name = "authId", required = false)
    var authCode: String = "",

    @field:Element(name = "referenceNumber", required = false)
    var referenceNumber: String = "",

    @field:Element(name = "masked_pan", required = false)
    var masked_pan: String = "",

    @field:Element(name = "stan", required = false)
    var stan: String = "",

    @field:Element(name = "transactionChannelName", required = false)
    var transactionChannelName: String = "",

    @field:Element(name = "wasReceive", required = false)
    var wasReceive: Boolean = false,

    @field:Element(name = "wasSend", required = false)
    var wasSend: Boolean = false,

    @field:Element(name = "responseMessage", required = false)
    var responseMessage: String = "",

    @field:Element(name = "transactionRef", required = false)
    var transactionRef: String = "",

    @field: Element(name = "scripts", required = false)
    var scripts: String = "",

    @field: Element(name = "responseDescription", required = false)
    var responseDescription: String? = null,

    @field: Element(name = "transactionId", required = false)
    var transactionId: String? = null,

    @field: Element(name = "transtype", required = false)
    var transTYpe: String? = null,

    @field: Element(name = "paymentType", required = false)
    var paymentType: String? = null,
    var terminal_id: String? = null,
    var date: String? = null,
    var merchant_code: String? = null,
    var transRoute: String? = null,
    var agent_transtype: String? = null,
    var currencyCode: String? = null,
    var amount: String = ""

   )



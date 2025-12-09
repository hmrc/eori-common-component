/*
 * Copyright 2025 HM Revenue & Customs
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package util

import play.api.libs.json._
import play.api.mvc._
import play.api.test.FakeRequest
import play.mvc.Http.HeaderNames._
import play.mvc.Http.MimeTypes
import uk.gov.hmrc.customs.managesubscription.domain.SubscriptionCompleteStatus.SubscriptionCompleteStatus
import uk.gov.hmrc.customs.managesubscription.domain.protocol.Eori
import uk.gov.hmrc.customs.managesubscription.domain.{Journey, _}

object TestData {
  val formBundleId           = "dummy-bundle-id-1"
  val sapNumber              = "0123456789"
  val taxPayerId: TaxPayerId = TaxPayerId(sapNumber)
  val eori: Eori             = Eori("GB0123456789")

  val emailVerificationTimestamp = "timestamp"
  val safeId                     = "SAFEID"

  def pruneField(from: JsValue, fieldName: String): JsValue =
    from.transform((__ \ fieldName).json.prune).get

  val emulatedServiceFailure = new RuntimeException("Emulated service failure")

  val MdtpBearerToken: String = "Bearer ValidBearerToken"
  val AcceptHmrcJson: String  = "application/vnd.hmrc.1.0+xml"

  val Register: Journey = Journey.Register
  val EnrolmentKey      = "HMRC-ATAR-ORG"
  val ServiceName       = "Advance Tariff Rulings"
  val FullName          = "Full Name"
  val Email             = "john.doe@digital.hmrc.gov.uk"
  val OrgName           = "Test Company Name"
  val CompletionDate    = "5 May 2017"

  val recipientDetails: RecipientDetails =
    RecipientDetails(Register, EnrolmentKey, ServiceName, Email, FullName, Some(OrgName), Some(CompletionDate), None)

  val NoHeaders: Map[String, String] = Map[String, String]()

  object SubscriptionResult {

    import RequestHeaders._

    case class RequestModel(url: Option[String], state: Option[String], errorResponse: Option[String])

    def mkRequest(model: RequestModel): FakeRequest[AnyContentAsJson] = mkRequest(mkJson(model))

    private def mkJson(model: RequestModel) = Json.toJson(model)(Json.writes[RequestModel])

    def mkRequest(jsonBody: JsValue): FakeRequest[AnyContentAsJson] =
      FakeRequest()
        .withHeaders(ACCEPT_HEADER, CONTENT_TYPE_HEADER)
        .withJsonBody(jsonBody)

    val successSubscriptionComplete: SubscriptionComplete =
      subscriptionComplete(SubscriptionCompleteStatus.SUCCEEDED, None)

    private val errorResponseString = "error description, terrible things have happened"

    val failedSubscriptionComplete: SubscriptionComplete =
      subscriptionComplete(SubscriptionCompleteStatus.ERROR, Some(errorResponseString))

    lazy val url = s"http://test:8080/tax-enrolments/subscriptions/$formBundleId"

    val validSucceededModel: RequestModel =
      RequestModel(url = Some(url), state = Some("SUCCEEDED"), errorResponse = None)

    val validErrorModel: RequestModel =
      RequestModel(url = Some(url), state = Some("ERROR"), errorResponse = Some(errorResponseString))

    val validSucceededJsonBody: JsValue = mkJson(validSucceededModel)
    val validErrorJsonBody: JsValue     = mkJson(validErrorModel)

    val validSucceededRequest: Request[AnyContent] = mkRequest(validSucceededJsonBody)
    val validErrorRequest: Request[AnyContent]     = mkRequest(validErrorJsonBody)

    val stateField = "state"

    private def subscriptionComplete(status: SubscriptionCompleteStatus, errorResponse: Option[String]) =
      SubscriptionComplete(url = url, state = status, errorResponse = errorResponse)

  }

  object HandleSubscription {

    import RequestHeaders._

    val validHeaders: Map[String, String] = Map(AUTHORISATION_HEADER, CONTENT_TYPE_HEADER, ACCEPT_HEADER)

    val handleSubscriptionRequest: HandleSubscriptionRequest = HandleSubscriptionRequest(
      formBundleId,
      recipientDetails,
      sapNumber,
      Some(eori.value),
      emailVerificationTimestamp,
      safeId
    )

    val validRequestJsonString: String =
      s"""
        |{
        |	"formBundleId": "$formBundleId",
        |	"recipientDetails": {
        |   "journey": "$Register",
        |   "enrolmentKey": "$EnrolmentKey",
        |   "serviceName": "$ServiceName",
        |   "recipientFullName": "$FullName",
        |	  "recipientEmailAddress": "$Email",
        |   "orgName": "$OrgName",
        |   "completionDate": "$CompletionDate"
        |	},
        |	"sapNumber": "$sapNumber",
        | "eori": "${eori.value}",
        | "emailVerificationTimestamp": "$emailVerificationTimestamp",
        | "safeId": "$safeId"
        |}
      """.stripMargin

    val validRequestJson: JsValue = Json.parse(validRequestJsonString)

    val invalidRequestJson: JsValue = Json.parse("""{"invalid": "request"}""")

    val validRequest: FakeRequest[AnyContentAsJson] = request()

    val invalidRequest: FakeRequest[AnyContentAsJson] = request(body = invalidRequestJson)

    lazy val InvalidContentTypeHeaderRequest: FakeRequest[AnyContentAsJson] =
      request(headers = validHeaders + CONTENT_TYPE_HEADER_INVALID)

    lazy val MissingContentTypeHeaderRequest: FakeRequest[AnyContentAsJson] = FakeRequest()
      .withHeaders(ACCEPT_HEADER)
      .withJsonBody(validRequestJson)

    lazy val MissingAcceptHeaderRequest: FakeRequest[AnyContentAsJson] = FakeRequest()
      .withHeaders(CONTENT_TYPE_HEADER)
      .withJsonBody(validRequestJson)

    lazy val InvalidAcceptHeaderRequest: FakeRequest[AnyContentAsJson] = FakeRequest()
      .withHeaders(CONTENT_TYPE_HEADER, ACCEPT_HEADER_INVALID)
      .withJsonBody(validRequestJson)

    val errorUnsupportedMediaType: JsValue = Json.parse("""
        |{
        |  "code":"UNSUPPORTED_MEDIA_TYPE",
        |  "message":"The content type header is missing or invalid"
        |}""".stripMargin)

    val errorAcceptHeaderInvalid: JsValue = Json.parse("""
        |{
        |  "code":"ACCEPT_HEADER_INVALID",
        |  "message":"The accept header is missing or invalid"
        |}""".stripMargin)

    val errorPayloadInvalid: JsValue = Json.parse("""
        |{
        |  "code":"BAD_REQUEST",
        |  "message":"Invalid payload"
        |}""".stripMargin)

    val errorUnauthorized: JsValue = Json.parse("""
        |{
        |  "code":"UNAUTHORIZED",
        |  "message":"Bearer token is missing or not authorized"
        |}""".stripMargin)

    def request(
      body: JsValue = validRequestJson,
      path: String = "/handle-subscription",
      headers: Map[String, String] = validHeaders
    ): FakeRequest[AnyContentAsJson] =
      FakeRequest(method = "POST", path = path)
        .withHeaders(headers.toSeq: _*)
        .withJsonBody(body)

    def requestWithTextBody(
      path: String = "/handle-subscription",
      headers: Map[String, String] = validHeaders
    ): FakeRequest[AnyContentAsText] =
      FakeRequest(method = "POST", path = path)
        .withHeaders(headers.toSeq: _*)
        .withTextBody("<testTextBody></testTextBody>")

  }

  object TaxEnrolment {

    val validRequestJsonString: String =
      s"""
         |{
         |  "serviceName": "HMRC-CUS-ORG",
         |  "callback": "http://callback.url/$formBundleId",
         |  "etmpId": "$sapNumber"
         |}
        """.stripMargin

    val validRequestJson: JsValue = Json.parse(validRequestJsonString)
  }

}

object RequestHeaders {

  import TestData._

  val CONTENT_TYPE_HEADER: (String, String) = CONTENT_TYPE -> MimeTypes.JSON

  val CONTENT_TYPE_HEADER_INVALID: (String, String) = CONTENT_TYPE -> MimeTypes.XML

  val ACCEPT_HEADER: (String, String) = ACCEPT -> "application/vnd.hmrc.1.0+json"

  val ACCEPT_HEADER_INVALID: (String, String) = ACCEPT -> AcceptHmrcJson

  val AUTHORISATION_HEADER: (String, String) = AUTHORIZATION -> MdtpBearerToken

  val AUTHORISATION_HEADER_INVALID: (String, String) = AUTHORIZATION -> "INVALID BEARER TOKEN"

}

/*
 * Copyright 2020 HM Revenue & Customs
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

package unit.connectors

import org.mockito.ArgumentMatchers._
import org.mockito.Mockito._
import play.api.libs.json.{JsString, Json}
import uk.gov.hmrc.customs.managesubscription.audit.Auditable
import uk.gov.hmrc.customs.managesubscription.connectors.SubscriptionDisplayConnector
import uk.gov.hmrc.http.{HeaderCarrier, HttpResponse}
import uk.gov.hmrc.http.HttpClient
import util.BaseSpec
import util.RequestHeaders.{ACCEPT_HEADER, AUTHORISATION_HEADER, CONTENT_TYPE_HEADER}

import scala.concurrent.Future
import scala.concurrent.ExecutionContext.Implicits.global

class SubscriptionDisplayConnectorSpec extends BaseSpec {

  private val validHeaders: Seq[(String, String)] = Seq(AUTHORISATION_HEADER, CONTENT_TYPE_HEADER, ACCEPT_HEADER)
  private val mockHttp                            = mock[HttpClient]
  private val mockAuditable                       = mock[Auditable]
  implicit val hc: HeaderCarrier                  = HeaderCarrier().withExtraHeaders(validHeaders: _*)

  private val testConnector = new SubscriptionDisplayConnector(appConfig, mockHttp, mockAuditable)

  "SubscriptionDisplayConnector" should {
    "return EORINo when a request to subscription display is successful" in {
      val responseBody = Json.parse("""{"subscriptionDisplayResponse": {"responseDetail": {"EORINo": "123456789"}}}""")
      when(mockHttp.doGet(any(), any())(any(), any())).thenReturn(
        Future.successful(HttpResponse(status = 200, json = responseBody, headers = Map.empty))
      )
      doNothing().when(mockAuditable).sendDataEvent(any(), any(), any(), any())(any[HeaderCarrier])
      await(testConnector.callSubscriptionDisplay(Seq(("queryparam", "value")))) shouldBe Some("123456789")
    }

    "return None when no EORINo received from subscription display" in {
      val responseBody = Json.parse("""{"subscriptionDisplayResponse": {}}""")
      when(mockHttp.doGet(any(), any())(any(), any())).thenReturn(
        Future.successful(HttpResponse(status = 200, json = responseBody, headers = Map.empty))
      )
      doNothing().when(mockAuditable).sendDataEvent(any(), any(), any(), any())(any[HeaderCarrier])
      await(testConnector.callSubscriptionDisplay(Seq(("queryparam", "value")))) shouldBe None
    }

    "return None when a request to subscription display is failed" in {
      when(mockHttp.doGet(any(), any())(any(), any())).thenReturn(
        Future.successful(HttpResponse(status = 400, json = JsString("error message"), headers = Map.empty))
      )
      doNothing().when(mockAuditable).sendDataEvent(any(), any(), any(), any())(any[HeaderCarrier])
      await(testConnector.callSubscriptionDisplay(Nil)) shouldBe None
    }
  }
}

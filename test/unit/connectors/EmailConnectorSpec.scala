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

package unit.connectors

import org.mockito.ArgumentMatchers.any
import play.api.test.Helpers.await
import uk.gov.hmrc.customs.managesubscription.audit.Auditable
import uk.gov.hmrc.customs.managesubscription.connectors.EmailConnector
import uk.gov.hmrc.customs.managesubscription.services.dto.Email
import uk.gov.hmrc.http.client.{HttpClientV2, RequestBuilder}
import uk.gov.hmrc.http.{HeaderCarrier, HttpResponse}
import util.BaseSpec

import scala.concurrent.{ExecutionContext, Future}

class EmailConnectorSpec extends BaseSpec {
  val mockHttp: HttpClientV2        = mock[HttpClientV2]
  val mockAuditable: Auditable      = mock[Auditable]
  implicit val hc: HeaderCarrier    = HeaderCarrier()
  implicit val ec: ExecutionContext = ExecutionContext.Implicits.global
  val testConnector                 = new EmailConnector(appConfig, mockHttp, mockAuditable)

  "EmailConnector" should {

    "successfully send a email request to Email service and return the OK response" in {
      val mockRequestBuilder = mock[RequestBuilder]
      when(mockHttp.post(any())(any())).thenReturn(mockRequestBuilder)
      when(mockRequestBuilder.withBody(any())(any, any, any)).thenReturn(mockRequestBuilder)
      when(mockRequestBuilder.setHeader((any, any))).thenReturn(mockRequestBuilder)
      when(mockRequestBuilder.execute[HttpResponse](any, any)).thenReturn(Future.successful(HttpResponse(200, "")))

      val result = await(testConnector.sendEmail(Email(List("toEmail"), "templateId", Map.empty)))
      result.status shouldBe 200
    }

    "successfully send a email request to Email service and return the ACCEPTED response" in {
      val mockRequestBuilder = mock[RequestBuilder]
      when(mockHttp.post(any())(any())).thenReturn(mockRequestBuilder)
      when(mockRequestBuilder.withBody(any())(any, any, any)).thenReturn(mockRequestBuilder)
      when(mockRequestBuilder.setHeader(any)).thenReturn(mockRequestBuilder)
      when(mockRequestBuilder.execute[HttpResponse](any, any)).thenReturn(Future.successful(HttpResponse(202, "")))

      val result = await(testConnector.sendEmail(Email(List("toEmail"), "templateId", Map.empty)))
      result.status shouldBe 202
    }

    "return the failure response from Email service" in {
      val mockRequestBuilder = mock[RequestBuilder]
      when(mockHttp.post(any())(any())).thenReturn(mockRequestBuilder)
      when(mockRequestBuilder.withBody(any())(any, any, any)).thenReturn(mockRequestBuilder)
      when(mockRequestBuilder.setHeader(any)).thenReturn(mockRequestBuilder)
      when(mockRequestBuilder.execute[HttpResponse](any, any)).thenReturn(Future.successful(HttpResponse(400, "")))

      val result = await(testConnector.sendEmail(Email(List(""), "", Map.empty)))
      result.status shouldBe 400
    }
  }
}

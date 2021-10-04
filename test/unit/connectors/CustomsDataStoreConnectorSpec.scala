/*
 * Copyright 2021 HM Revenue & Customs
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
import play.api.http.Status.NO_CONTENT
import uk.gov.hmrc.customs.managesubscription.audit.Auditable
import uk.gov.hmrc.customs.managesubscription.connectors.CustomsDataStoreConnector
import uk.gov.hmrc.customs.managesubscription.domain.DataStoreRequest
import uk.gov.hmrc.http.{HeaderCarrier, HttpClient, HttpResponse}
import util.BaseSpec

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future

class CustomsDataStoreConnectorSpec extends BaseSpec {

  val mockHttp      = mock[HttpClient]
  val mockAuditable = mock[Auditable]
  implicit val hc   = new HeaderCarrier()

  val testConnector = new CustomsDataStoreConnector(appConfig, mockHttp, mockAuditable)

  "CustomsDataStoreConnector" should {
    "successfully send a query request to customs data store and return the OK response" in {
      when(mockHttp.POST[DataStoreRequest, HttpResponse](any(), any(), any())(any(), any(), any(), any())).thenReturn(
        Future.successful(HttpResponse(204, ""))
      )
      doNothing().when(mockAuditable).sendDataEvent(any(), any(), any(), any())(any[HeaderCarrier])
      val result = await(testConnector.updateDataStore(DataStoreRequest("eori", "address", "timestamp")))
      result.status shouldBe NO_CONTENT
    }

    "return the failure response from customs data store" in {
      when(mockHttp.POST[DataStoreRequest, HttpResponse](any(), any(), any())(any(), any(), any(), any())).thenReturn(
        Future.successful(HttpResponse(500, "InternalServerError"))
      )
      doNothing().when(mockAuditable).sendDataEvent(any(), any(), any(), any())(any[HeaderCarrier])
      val result = await(testConnector.updateDataStore(DataStoreRequest("", "", "")))
      result.status shouldBe 500
    }
  }
}

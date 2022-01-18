/*
 * Copyright 2022 HM Revenue & Customs
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

package integration

import play.api.Application
import play.api.inject.guice.GuiceApplicationBuilder
import play.api.libs.json.Json
import uk.gov.hmrc.customs.managesubscription.connectors.EmailConnector
import uk.gov.hmrc.customs.managesubscription.services.dto.Email
import uk.gov.hmrc.http.HeaderCarrier
import util.ExternalServicesConfig._
import util.MDTPEmailService

class EmailConnectorSpec extends IntegrationTestsWithDbSpec with MDTPEmailService {

  implicit override lazy val app: Application = new GuiceApplicationBuilder().configure(
    Map("microservice.services.email.host" -> Host, "microservice.services.email.port" -> Port)
  ).build()

  override def beforeAll {
    startMockServer()
    mdtpEmailServiceIsRunning()
  }

  override def afterAll {
    stopMockServer()
  }

  private def emailConnector = app.injector.instanceOf[EmailConnector]

  private val testEmail = Email(
    to = List("john.doe@digital.hmrc.gov.uk", "john.doe2@digital.hmrc.gov.uk"),
    templateId = "test-template-id",
    parameters = Map("param_key" -> "param_value"),
    force = true
  )

  "EmailConnector" should {

    "call email service with correct content" in {
      await(emailConnector.sendEmail(testEmail)(HeaderCarrier()))

      Json.parse(requestMadeToMDTPEmailService()) shouldBe Json.parse(
        """
          |{
          |"to": ["john.doe@digital.hmrc.gov.uk", "john.doe2@digital.hmrc.gov.uk"],
          |"templateId": "test-template-id",
          |"parameters": {"param_key": "param_value"},
          |"force": true
          |}
        """.stripMargin
      )
    }
  }
}

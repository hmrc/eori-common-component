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

package unit.controllers

import play.api.mvc.Results._
import play.api.mvc.{Action, AnyContent}
import play.api.test.{FakeRequest, NoMaterializer}
import play.api.test.Helpers.stubPlayBodyParsers
import play.mvc.Http.HeaderNames.CONTENT_TYPE
import uk.gov.hmrc.customs.managesubscription.controllers.ErrorResponse._
import uk.gov.hmrc.customs.managesubscription.controllers.MessagingHeaderValidator
import uk.gov.hmrc.play.test.UnitSpec
import util.AsyncTest
import util.RequestHeaders._
import util.TestData.HandleSubscription.validHeaders

import scala.concurrent.ExecutionContext.global

class MessagingHeaderValidatorSpec extends UnitSpec with AsyncTest {

  private val validator = new MessagingHeaderValidator(stubPlayBodyParsers(NoMaterializer))(global)
  val expectedResult    = Ok("as expected")

  val action: Action[AnyContent] = validator async {
    expectedResult
  }

  private def requestWithHeaders(headers: Map[String, String]) =
    FakeRequest().withHeaders(headers.toSeq: _*)

  "HeaderValidatorAction" should {
    "return processing result when request headers contain valid values" in {
      eventuallyTest(action(requestWithHeaders(validHeaders))) { actual =>
        actual shouldBe expectedResult
      }
    }

    "return Error result when the ContentType header does not exist" in {
      eventuallyTest(action(requestWithHeaders(validHeaders - CONTENT_TYPE))) { actual =>
        actual shouldBe ErrorContentTypeHeaderInvalid.JsonResult
      }
    }

    "return Error result when ContentType header does not contain expected value" in {
      eventuallyTest(action(requestWithHeaders(validHeaders + CONTENT_TYPE_HEADER_INVALID))) { actual =>
        actual shouldBe ErrorContentTypeHeaderInvalid.JsonResult
      }
    }
  }
}

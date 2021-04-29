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

package unit.controllers

import akka.stream.testkit.NoMaterializer
import org.scalatest.prop.TableDrivenPropertyChecks
import play.api.mvc.Results.Ok
import play.api.mvc._
import play.api.test.FakeRequest
import play.api.test.Helpers.stubPlayBodyParsers
import play.mvc.Http.HeaderNames._
import uk.gov.hmrc.customs.managesubscription.controllers.DigitalHeaderValidator
import uk.gov.hmrc.customs.managesubscription.controllers.ErrorResponse._
import util.UnitSpec
import util.RequestHeaders._
import util.TestData.HandleSubscription.validHeaders
import util.TestData._

import scala.concurrent.ExecutionContext.global

class DigitalHeaderValidationSpec extends UnitSpec with TableDrivenPropertyChecks {

  private val validator      = new DigitalHeaderValidator(stubPlayBodyParsers(NoMaterializer))(global)
  private val expectedResult = Ok("as expected")

  val action: Action[AnyContent] = validator async {
    expectedResult
  }

  val headersTable = Table(
    ("description", "Headers", "Expected response"),
    ("return OK result for valid headers", validHeaders, expectedResult),
    (
      "return ErrorContentTypeHeaderInvalid result for content type header missing",
      validHeaders - CONTENT_TYPE,
      ErrorContentTypeHeaderInvalid.JsonResult
    ),
    (
      "return ErrorContentTypeHeaderInvalid result for content type header invalid",
      validHeaders + CONTENT_TYPE_HEADER_INVALID,
      ErrorContentTypeHeaderInvalid.JsonResult
    ),
    (
      "return ErrorAcceptHeaderInvalid result for accept header missing",
      validHeaders - ACCEPT,
      ErrorAcceptHeaderInvalid.JsonResult
    ),
    (
      "return ErrorAcceptHeaderInvalid result for accept header invalid",
      validHeaders + ACCEPT_HEADER_INVALID,
      ErrorAcceptHeaderInvalid.JsonResult
    ),
    (
      "return ErrorUnauthorized result when bearer token is missing",
      validHeaders - AUTHORIZATION,
      ErrorUnauthorized.JsonResult
    ),
    (
      "return ErrorUnauthorized result when bearer token is invalid",
      validHeaders + AUTHORISATION_HEADER_INVALID,
      ErrorUnauthorized.JsonResult
    ),
    ("return ErrorAcceptHeaderInvalid result for all headers missing", NoHeaders, ErrorAcceptHeaderInvalid.JsonResult)
  )

  private def requestWithHeaders(headers: Map[String, String]) =
    FakeRequest().withHeaders(headers.toSeq: _*)

  "HeaderValidatorAction" should {
    forAll(headersTable) { (description, headers, response) =>
      s"$description" in {
        await(action.apply(requestWithHeaders(headers))) shouldBe response
      }
    }
  }
}

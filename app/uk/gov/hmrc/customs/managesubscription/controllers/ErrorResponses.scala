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

package uk.gov.hmrc.customs.managesubscription.controllers

import play.api.http.ContentTypes
import play.api.libs.json._
import play.api.mvc.Result
import play.api.mvc.Results._
import play.mvc.Http.Status._

trait HttpStatusCodeShortDescriptions {
  // 4XX
  val BadRequestCode           = "BAD_REQUEST"
  val NotAcceptableCode        = "ACCEPT_HEADER_INVALID"
  val UnsupportedMediaTypeCode = "UNSUPPORTED_MEDIA_TYPE"
  // 5XX

}

case class ResponseContents(code: String, message: String)

object ResponseContents {
  implicit val writes: Writes[ResponseContents] = Json.writes[ResponseContents]
}

case class ErrorResponse(httpStatusCode: Int, errorCode: String, message: String, content: ResponseContents*)
    extends Error {
  private lazy val errorContent = JsObject(Seq("code" -> JsString(errorCode), "message" -> JsString(message)))

  private lazy val responseJson: JsValue =
    if (content.isEmpty) errorContent else errorContent + ("errors" -> Json.toJson(content))

  lazy val JsonResult: Result = Status(httpStatusCode)(responseJson).as(ContentTypes.JSON)

}

object ErrorResponse extends HttpStatusCodeShortDescriptions {

  private def errorBadRequest(errorMessage: String, errorCode: String = BadRequestCode): ErrorResponse =
    ErrorResponse(BAD_REQUEST, errorCode, errorMessage)

  val ErrorGenericBadRequest: ErrorResponse = errorBadRequest("Bad Request")

  val ErrorInvalidPayload: ErrorResponse = errorBadRequest("Invalid payload")

  val ErrorAcceptHeaderInvalid: ErrorResponse =
    ErrorResponse(NOT_ACCEPTABLE, NotAcceptableCode, "The accept header is missing or invalid")

  val ErrorContentTypeHeaderInvalid: ErrorResponse =
    ErrorResponse(UNSUPPORTED_MEDIA_TYPE, UnsupportedMediaTypeCode, "The content type header is missing or invalid")

}

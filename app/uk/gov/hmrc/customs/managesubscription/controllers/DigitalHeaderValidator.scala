/*
 * Copyright 2023 HM Revenue & Customs
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

import javax.inject.Inject
import play.api.http.HeaderNames._
import play.api.mvc._
import play.mvc.Http.MimeTypes
import uk.gov.hmrc.customs.managesubscription.controllers.ErrorResponse._

import scala.concurrent.{ExecutionContext, Future}

class DigitalHeaderValidator @Inject() (bodyParsers: PlayBodyParsers)(implicit
  override val executionContext: ExecutionContext
) extends ActionBuilder[Request, AnyContent] {

  override val parser: BodyParser[AnyContent] = bodyParsers.anyContent

  private val BearerTokenRegex = "^Bearer .*"

  def invokeBlock[A](request: Request[A], block: Request[A] => Future[Result]): Future[Result] = {
    val headers = request.headers

    if (!accept(headers)) Future.successful(ErrorAcceptHeaderInvalid.JsonResult)
    else if (!contentType(headers)) Future.successful(ErrorContentTypeHeaderInvalid.JsonResult)
    else if (!bearerToken(headers)) Future.successful(ErrorUnauthorized.JsonResult)
    else block(request)
  }

  private val accept: Headers => Boolean = _.get(ACCEPT).fold(false)(_ == "application/vnd.hmrc.1.0+json")

  private val contentType: Headers => Boolean = _.get(CONTENT_TYPE).fold(false)(_ == MimeTypes.JSON)

  private val bearerToken: Headers => Boolean = _.get(AUTHORIZATION).fold(false)(_.matches(BearerTokenRegex))
}

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

package uk.gov.hmrc.customs.managesubscription.controllers

import javax.inject.Inject
import play.api.mvc._
import play.mvc.Http.HeaderNames.CONTENT_TYPE
import play.mvc.Http.MimeTypes
import uk.gov.hmrc.customs.managesubscription.controllers.ErrorResponse._

import scala.concurrent.{ExecutionContext, Future}

class MessagingHeaderValidator @Inject()(bodyParsers: PlayBodyParsers)(
  implicit override val executionContext: ExecutionContext
) extends ActionBuilder[Request, AnyContent] {

  override val parser: BodyParser[AnyContent] = bodyParsers.anyContent

  def invokeBlock[A](request: Request[A], block: Request[A] => Future[Result]): Future[Result] = {

    val contentTypeValid = request.headers.get(CONTENT_TYPE).exists(_.startsWith(MimeTypes.JSON))

    if (contentTypeValid) block(request)
    else Future.successful(ErrorContentTypeHeaderInvalid.JsonResult)
  }
}

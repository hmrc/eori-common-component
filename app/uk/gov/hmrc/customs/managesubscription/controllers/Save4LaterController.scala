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

package uk.gov.hmrc.customs.managesubscription.controllers

import eu.europa.ec.taxud.vies.services.checkvat.CheckVatTestService
import eu.europa.ec.taxud.vies.services.checkvat.types.CheckVat
import javax.inject.{Inject, Singleton}
import play.api.mvc.{Action, AnyContent, ControllerComponents}
import uk.gov.hmrc.auth.core.AuthProvider.GovernmentGateway
import uk.gov.hmrc.auth.core.{AuthProviders, AuthorisedFunctions}
import uk.gov.hmrc.customs.managesubscription.connectors.MicroserviceAuthConnector
import uk.gov.hmrc.customs.managesubscription.repository.Save4LaterRepository
import uk.gov.hmrc.play.bootstrap.backend.controller.BackendController

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future

import javax.xml.ws.Holder

@Singleton
class Save4LaterController @Inject() (
  save4LaterRepository: Save4LaterRepository,
  cc: ControllerComponents,
  override val authConnector: MicroserviceAuthConnector,
  checkVatTestService: CheckVatTestService
) extends BackendController(cc) with AuthorisedFunctions {


  def test() = {
    checkVatTestService.getCheckVatPort(new TestHandler()).checkVat(new Holder("FR"), new Holder("200"), new Holder(null), new Holder(null), new Holder(null), new Holder(null))
  }

  def put(id: String, key: String): Action[AnyContent] = Action.async { implicit request =>
    test()
    authorised(AuthProviders(GovernmentGateway)) {
      request.body.asJson.fold(ifEmpty = Future.successful(BadRequest)) { js =>
        save4LaterRepository.save(id, key, js).map(_ => Created)
      }
    }
  }

  def get(id: String, key: String): Action[AnyContent] = Action.async { implicit request =>
    authorised(AuthProviders(GovernmentGateway)) {
      save4LaterRepository.findByIdAndKey(id, key).map {
        case Some(js) => Ok(js)
        case None     => NotFound(s"key:$key | id:$id")
      }
    }
  }

  def removeKeyById(id: String, key: String): Action[AnyContent] = Action.async { implicit request =>
    authorised(AuthProviders(GovernmentGateway)) {
      save4LaterRepository.removeKeyById(id, key).map {
        case true  => NoContent
        case false => NotFound(s"key:$key | id:$id")
      }
    }
  }

  def delete(id: String): Action[AnyContent] = Action.async { implicit request =>
    authorised(AuthProviders(GovernmentGateway)) {
      save4LaterRepository.remove(id).map {
        case true  => NoContent
        case false => NotFound(s"id:$id")
      }
    }
  }

}

import javax.xml.ws.handler.MessageContext
import javax.xml.ws.handler.soap.{SOAPHandler, SOAPMessageContext}

class TestHandler() extends SOAPHandler[SOAPMessageContext] {
  def getHeaders = null

  println("Creating Test Handler")

  def handleMessage(context: SOAPMessageContext) = {
    val outbound = context.get(MessageContext.MESSAGE_OUTBOUND_PROPERTY)
      .asInstanceOf[java.lang.Boolean]
    val message = context.getMessage

    if (outbound) {
      println(s"Sending message:")

      message.writeTo(System.out)
    } else {
      val responseCode = context.get(MessageContext.HTTP_RESPONSE_CODE)
      println("Message: " + context.getMessage)
      println(s"Received $responseCode response:")
      message.writeTo(System.out)
    }
    println()
    true
  }

  def close(context: MessageContext) = ()

  def handleFault(context: SOAPMessageContext) = {
    println(s"Received fault:")
    context.getMessage.writeTo(System.out)
    println()
    true
  }
}

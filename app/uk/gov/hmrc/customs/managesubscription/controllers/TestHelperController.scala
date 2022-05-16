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

package uk.gov.hmrc.customs.managesubscription.controllers

import play.api.libs.json.{Json, OFormat}
import play.api.mvc.ControllerComponents
import uk.gov.hmrc.customs.managesubscription.domain.HandleSubscriptionRequest
import uk.gov.hmrc.customs.managesubscription.domain.protocol.TaxEnrolmentsRequest
import uk.gov.hmrc.customs.managesubscription.services.PayloadCache
import uk.gov.hmrc.play.bootstrap.backend.controller.BackendController

import javax.inject.{Inject, Singleton}

@Singleton
class TestHelperController @Inject() (cc: ControllerComponents) extends BackendController(cc) {

  def payloads(key: String) = Action {

    val payloadKey = key match {
      case "subscriber_call" => PayloadCache.SubscriberCall
    }

    val payload: TaxEnrolmentsRequest = payloadKey match {
      case PayloadCache.SubscriberCall =>
        val someData = PayloadCache.payloads.get(PayloadCache.SubscriberCall)
          .getOrElse(throw new RuntimeException("Ive lost my data!!! : ("))
        someData match {
          case TaxEnrolmentsRequest(a, b, c, d) => TaxEnrolmentsRequest(a, b, c, d)
        }
      case _ => throw new RuntimeException("Something bad has happened")
    }

    Ok(Json.toJson(payload))
  }

}

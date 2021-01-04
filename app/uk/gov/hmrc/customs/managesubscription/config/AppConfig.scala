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

package uk.gov.hmrc.customs.managesubscription.config

import com.google.inject.Inject
import play.api.Configuration
import uk.gov.hmrc.play.bootstrap.config.ServicesConfig

class AppConfig @Inject() (config: Configuration, servicesConfig: ServicesConfig) {

  private val subscriptionDisplayBaseUrl: String = servicesConfig.baseUrl("subscription-display")

  val subscriptionDisplayContext: String = config.get[String]("microservice.services.subscription-display.context")
  val subscriptionDisplayUrl: String     = s"$subscriptionDisplayBaseUrl$subscriptionDisplayContext"

  val subscriptionDisplayBearerToken: String =
    config.get[String]("microservice.services.subscription-display.bearer-token")

  private val emailServiceBaseUrl: String = servicesConfig.baseUrl("email")

  val emailServiceContext: String = config.get[String]("microservice.services.email.context")
  val emailServiceUrl: String     = s"$emailServiceBaseUrl$emailServiceContext"

  val emailRegisterSuccessTemplateId: String =
    config.get[String]("microservice.services.email.registerSuccessTemplateId")

  val emailRegisterNotSuccessTemplateId: String =
    config.get[String]("microservice.services.email.registerNotSuccessTemplateId")

  val emailSubscribeSuccessTemplateId: String =
    config.get[String]("microservice.services.email.subscribeSuccessTemplateId")

  val emailSubscribeNotSuccessTemplateId: String =
    config.get[String]("microservice.services.email.subscribeNotSuccessTemplateId")

  val emailRCMTemplateId: String =
    config.get[String]("microservice.services.email.rcmTemplateId")

  val rcmEmailAddress: String =
    config.get[String]("microservice.services.email.rcmEmailAddress")

  private val taxEnrolmentsBaseUrl: String = servicesConfig.baseUrl("tax-enrolments")

  val taxEnrolmentsContext: String     = config.get[String]("microservice.services.tax-enrolments.context")
  val taxEnrolmentsUrl: String         = s"$taxEnrolmentsBaseUrl$taxEnrolmentsContext"
  val taxEnrolmentsCallbackUrl: String = config.get[String]("microservice.services.tax-enrolments.callback-url")
}

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

package base

import org.scalatest.{BeforeAndAfter, BeforeAndAfterAll}
import org.scalatestplus.play.guice.GuiceOneAppPerSuite
import play.api.Application
import play.api.inject.bind
import play.api.inject.guice.GuiceApplicationBuilder
import uk.gov.hmrc.mongo.MongoComponent
import uk.gov.hmrc.mongo.test.MongoSupport
import util.ExternalServicesConfig.{Host, Port}
import util.UnitSpec

trait IntegrationTestsWithDbSpec
    extends UnitSpec with BeforeAndAfter with BeforeAndAfterAll with GuiceOneAppPerSuite with MongoSupport {

  override implicit lazy val app: Application = new GuiceApplicationBuilder()
    .configure(
      Map(
        "microservice.services.tax-enrolments.host"          -> Host,
        "microservice.services.tax-enrolments.port"          -> Port,
        "microservice.services.tax-enrolments.context"       -> "/tax-enrolments/subscriptions",
        "microservice.services.customs-data-store.host"      -> Host,
        "microservice.services.customs-data-store.port"      -> Port,
        "microservice.services.customs-data-store.context"   -> "/customs-data-store/update-email",
        "microservice.services.subscription-display.host"    -> Host,
        "microservice.services.subscription-display.port"    -> Port,
        "microservice.services.subscription-display.context" -> "/subscriptions/subscriptiondisplay/v1",
        "microservice.services.integration-framework.host"   -> Host,
        "microservice.services.integration-framework.port"   -> Port,
        "auditing.enabled"                                   -> false,
        "auditing.consumer.baseUri.host"                     -> Host,
        "auditing.consumer.baseUri.port"                     -> Port
      )
    )
    .overrides(bind[MongoComponent].toInstance(mongoComponent))
    .build()

  override def beforeAll(): Unit =
    dropDatabase()

}

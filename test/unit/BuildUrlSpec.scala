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

package unit

import org.scalatest.matchers.should.Matchers
import org.scalatest.wordspec.AnyWordSpec
import org.scalatestplus.play.guice.GuiceOneAppPerSuite
import uk.gov.hmrc.customs.managesubscription.BuildUrl

class BuildUrlSpec extends AnyWordSpec with Matchers with GuiceOneAppPerSuite {

  val buildUrl = app.injector.instanceOf[BuildUrl]

  "BuildUrl" should {

    "provide a url for a existing service name" in {
      buildUrl("tax-enrolments") shouldBe "http://localhost:6754/tax-enrolments/subscriptions"
    }

    "throw an exception for a non-existing service" in {
      a[RuntimeException] should be thrownBy {
        buildUrl("something-we-dont-have")
      }
    }
  }
}

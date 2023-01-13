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

package unit.services

import org.scalatest.flatspec.AnyFlatSpecLike
import org.scalatest.matchers.should.Matchers
import uk.gov.hmrc.customs.managesubscription.connectors.Instrumentable
import uk.gov.hmrc.customs.managesubscription.services.PayloadCache

class InstrumentableSpec extends AnyFlatSpecLike with Matchers {

  "Instrumentable" should "add payloads to cache" in {
    val subject = new Instrumentable() {}
    subject.sampleData(PayloadCache.SubscriberCall, "hello")
    subject.sampleData(PayloadCache.SubscriptionCreate, 12)

    subject.cache.get(PayloadCache.SubscriberCall) shouldBe Some("hello")
    subject.cache.get(PayloadCache.SubscriptionCreate) shouldBe Some(12)
  }
}

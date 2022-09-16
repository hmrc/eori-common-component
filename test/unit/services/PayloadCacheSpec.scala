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

package unit.services

import org.scalatest.flatspec.AnyFlatSpecLike
import org.scalatest.matchers.should.Matchers
import uk.gov.hmrc.customs.managesubscription.services.PayloadCache

class PayloadCacheSpec extends AnyFlatSpecLike with Matchers {

  "Cache" should "store SubscriberCall data" in {
    val subject = PayloadCache.payloads

    subject.put(PayloadCache.SubscriberCall, "subscriber_call")
    subject.getOrElse(PayloadCache.SubscriberCall, "") shouldBe "subscriber_call"
  }

  "Cache" should "store SubscriptionCreate data" in {
    val subject = PayloadCache.payloads

    subject.put(PayloadCache.SubscriptionCreate, 14.7)
    subject.getOrElse(PayloadCache.SubscriptionCreate, 0.0) shouldBe 14.7
  }

  "Cache" should "store BusinessMatch data" in {
    val subject = PayloadCache.payloads

    subject.put(PayloadCache.BusinessMatch, 'a')
    subject.getOrElse(PayloadCache.BusinessMatch, '_') shouldBe 'a'
  }

}

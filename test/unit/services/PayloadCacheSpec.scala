package unit.services

import org.scalatest.{FlatSpec, Matchers}
import uk.gov.hmrc.customs.managesubscription.services.PayloadCache

class PayloadCacheSpec extends FlatSpec with Matchers {

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

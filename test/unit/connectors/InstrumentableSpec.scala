package unit.services

import org.scalatest.{FlatSpec, Matchers}
import uk.gov.hmrc.customs.managesubscription.connectors.Instrumentable
import uk.gov.hmrc.customs.managesubscription.services.PayloadCache

class InstrumentableSpec extends FlatSpec with Matchers {

  "Instrumentable" should "add payloads to cache" in {
    val subject = new Instrumentable() {}
    subject.sampleData(PayloadCache.SubscriberCall, "hello")
    subject.sampleData(PayloadCache.SubscriptionCreate, 12)

    subject.cache.get(PayloadCache.SubscriberCall) shouldBe Some("hello")
    subject.cache.get(PayloadCache.SubscriptionCreate) shouldBe Some(12)
  }
}

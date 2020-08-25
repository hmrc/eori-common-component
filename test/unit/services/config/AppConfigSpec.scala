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

package unit.services.config

import util.BaseSpec

class AppConfigSpec extends BaseSpec {

  "AppConfig" should {
    "have subscriptionDisplayContext defined" in {
      appConfig.subscriptionDisplayContext shouldBe "/subscriptions/subscriptiondisplay/v1"
    }

    "have subscriptionDisplayUrl defined" in {
      appConfig.subscriptionDisplayUrl shouldBe "http://localhost:9477/subscriptions/subscriptiondisplay/v1"
    }

    "have subscriptionDisplayBearerToken defined" in {
      appConfig.subscriptionDisplayBearerToken shouldBe "bearer_token_must_be_set_in_app-config-xxx"
    }

    "have customDataStoreContext defined" in {
      appConfig.customDataStoreContext shouldBe "/customs-data-store/graphql"
    }

    "have customDataStoreUrl defined" in {
      appConfig.customDataStoreUrl shouldBe "http://localhost:9893/customs-data-store/graphql"
    }

    "have customDataStoreToken defined" in {
      appConfig.customDataStoreToken shouldBe "secret-token"
    }

    "have emailServiceContext defined" in {
      appConfig.emailServiceContext shouldBe "/hmrc/email"
    }

    "have emailServiceUrl defined" in {
      appConfig.emailServiceUrl shouldBe "http://localhost:8300/hmrc/email"
    }

    "have emailGyeSuccessTemplateId defined" in {
      appConfig.emailGyeSuccessTemplateId shouldBe "customs_registration_successful"
    }

    "have emailGyeNotSuccessTemplateId defined" in {
      appConfig.emailGyeNotSuccessTemplateId shouldBe "customs_registration_not_successful"
    }

    "have emailMigrateSuccessTemplateId defined" in {
      appConfig.emailMigrateSuccessTemplateId shouldBe "customs_migrate_successful"
    }

    "have emailMigrateNotSuccessTemplateId defined" in {
      appConfig.emailMigrateNotSuccessTemplateId shouldBe "customs_migrate_not_successful"
    }

    "have taxEnrolmentsContext defined" in {
      appConfig.taxEnrolmentsContext shouldBe "/tax-enrolments/subscriptions"
    }

    "have taxEnrolmentsUrl defined" in {
      appConfig.taxEnrolmentsUrl shouldBe "http://localhost:9477/tax-enrolments/subscriptions"
    }

    "have taxEnrolmentsCallbackUrl defined" in {
      appConfig.taxEnrolmentsCallbackUrl shouldBe "http://callback.url"
    }

    "have taxEnrolmentsServiceName defined" in {
      appConfig.taxEnrolmentsServiceName shouldBe "HMRC-CUS-ORG"
    }
  }
}

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

package util

import org.mockito.MockitoSugar
import org.scalatest.BeforeAndAfterEach
import play.api.{Configuration, Environment}
import uk.gov.hmrc.customs.managesubscription.config.AppConfig
import uk.gov.hmrc.play.bootstrap.config.ServicesConfig

class BaseSpec extends UnitSpec with MockitoSugar with BeforeAndAfterEach {

  val env: Environment = Environment.simple()

  private val config = Configuration.load(env)

  private val serviceConfig = new ServicesConfig(config)

  implicit val appConfig: AppConfig = new AppConfig(config, serviceConfig)
}

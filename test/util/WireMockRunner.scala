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

import com.github.tomakehurst.wiremock.WireMockServer
import com.github.tomakehurst.wiremock.client.WireMock
import com.github.tomakehurst.wiremock.common.ConsoleNotifier
import com.github.tomakehurst.wiremock.core.WireMockConfiguration._

trait WireMockRunner {

  import Constants._

  lazy val wireMockUrl    = s"http://$wireMockHost:$wireMockPort"
  lazy val wireMockServer = new WireMockServer(wireMockConfig().port(wireMockPort).notifier(new ConsoleNotifier(false)))

  def startMockServer(): Unit = {
    if (!wireMockServer.isRunning) wireMockServer.start()
    WireMock.configureFor(wireMockHost, wireMockPort)
  }

  def resetMockServer(): Unit =
    WireMock.reset()

  def stopMockServer(): Unit =
    wireMockServer.stop()

}

object Constants {
  lazy val wireMockPort = 11111
  lazy val wireMockHost = "localhost"
}

import com.typesafe.sbt.packager.MappingsHelper._
import play.core.PlayVersion
import sbt.Keys._
import sbt._
import uk.gov.hmrc.DefaultBuildSettings.{defaultSettings, targetJvm}
import uk.gov.hmrc.SbtArtifactory
import uk.gov.hmrc.sbtdistributables.SbtDistributablesPlugin
import uk.gov.hmrc.sbtdistributables.SbtDistributablesPlugin._

import scala.language.postfixOps

mappings in Universal ++= directory(baseDirectory.value / "public")
// my understanding is publishing processed changed when we moved to the open and
// now it is done in production mode (was in dev previously). hence, we encounter the problem accessing "public" folder
// see https://stackoverflow.com/questions/36906106/reading-files-from-public-folder-in-play-framework-in-production

name := "eori-common-component"

PlayKeys.devSettings := Seq("play.server.http.port" -> "9829")

majorVersion := 0

targetJvm := "jvm-1.8"

scalaVersion := "2.12.12"

resolvers += Resolver.bintrayRepo("hmrc", "releases")

lazy val microservice = (project in file("."))
  .enablePlugins(play.sbt.PlayScala, SbtAutoBuildPlugin, SbtGitVersioning, SbtDistributablesPlugin, SbtArtifactory)
  .disablePlugins(sbt.plugins.JUnitXmlReportPlugin)
  .settings(
    commonSettings,
    scoverageSettings
  )

lazy val commonSettings: Seq[Setting[_]] = publishingSettings ++ defaultSettings()

lazy val scoverageSettings: Seq[Setting[_]] = Seq(
  coverageExcludedPackages := "<empty>;Reverse.*;uk.gov.hmrc.customs.managesubscription.config.*;.*(BuildInfo|Routes).*;.*ConfigModule.*;.*ConfigValidationNelAdaptor.*;.*ErrorResponse.*",
  coverageMinimum := 95,
  coverageFailOnMinimum := true,
  coverageHighlighting := true,
  parallelExecution in Test := false
)

scalastyleConfig := baseDirectory.value / "project" / "scalastyle-config.xml"

javaOptions in Test += "-Dlogger.resource=logback-test.xml"

libraryDependencies ++= Seq(
  ws exclude("org.apache.httpcomponents", "httpclient") exclude("org.apache.httpcomponents", "httpcore"),
  "uk.gov.hmrc" %% "bootstrap-play-26" % "1.14.0",
  "uk.gov.hmrc" %% "http-caching-client" % "9.1.0-play-26",
  "uk.gov.hmrc" %% "mongo-caching" % "6.15.0-play-26",
  "com.typesafe.play" %% "play-json-joda" % "2.6.10",
  "uk.gov.hmrc" %% "logback-json-logger" % "4.8.0",


  "com.typesafe.play" %% "play-test" % PlayVersion.current % "test",
  "org.scalatestplus.play" %% "scalatestplus-play" % "3.1.2" % "test",
  "com.github.tomakehurst" % "wiremock-standalone" % "2.23.2" % "test",
  "uk.gov.hmrc" %% "bootstrap-play-26" % "1.3.0" % "test" classifier "tests",
  "org.mockito" % "mockito-core" % "2.23.0" % "test",
  "org.scalatest" %% "scalatest" % "3.0.4" % "test",
  "uk.gov.hmrc" %% "hmrctest" % "3.8.0-play-26" % "test",
  "uk.gov.hmrc" %% "reactivemongo-test" % "4.21.0-play-26" % Test
)

import com.typesafe.sbt.packager.MappingsHelper._
import play.core.PlayVersion
import sbt.Keys._
import sbt._
import uk.gov.hmrc.DefaultBuildSettings.{defaultSettings, targetJvm}
import uk.gov.hmrc.sbtdistributables.SbtDistributablesPlugin
import uk.gov.hmrc.sbtdistributables.SbtDistributablesPlugin._

import scala.language.postfixOps

mappings in Universal ++= directory(baseDirectory.value / "public")
// my understanding is publishing processed changed when we moved to the open and
// now it is done in production mode (was in dev previously). hence, we encounter the problem accessing "public" folder
// see https://stackoverflow.com/questions/36906106/reading-files-from-public-folder-in-play-framework-in-production

name := "eori-common-component"

PlayKeys.devSettings := Seq("play.server.http.port" -> "6752")

majorVersion := 0

targetJvm := "jvm-1.8"

scalaVersion := "2.12.12"

Test / fork := false

lazy val microservice = (project in file("."))
  .enablePlugins(play.sbt.PlayScala, SbtDistributablesPlugin)
  .disablePlugins(sbt.plugins.JUnitXmlReportPlugin)
  .settings(
    commonSettings,
    scoverageSettings,
    silencerSettings
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
  "uk.gov.hmrc" %% "bootstrap-backend-play-28" % "5.18.0",
  "uk.gov.hmrc" %% "http-caching-client" % "9.5.0-play-28",
  "uk.gov.hmrc.mongo" %% "hmrc-mongo-play-28" % "0.62.0",
  "com.typesafe.play" %% "play-json-joda" % "2.8.1",
  "uk.gov.hmrc" %% "logback-json-logger" % "5.1.0",
  "com.typesafe.play" %% "play-test" % PlayVersion.current % "test",
  "org.scalatestplus.play" %% "scalatestplus-play" % "5.0.0" % "test",
  "com.github.tomakehurst" % "wiremock-standalone" % "2.23.2" % "test",
  "org.mockito" % "mockito-core" % "3.0.0" % "test",
  "org.scalatest" %% "scalatest" % "3.0.8" % "test",
  "org.pegdown" % "pegdown" % "1.6.0",
  "uk.gov.hmrc.mongo" %% "hmrc-mongo-test-play-27" % "0.56.0" % Test
)

lazy val silencerSettings: Seq[Setting[_]] = {
  val silencerVersion = "1.7.0"
  Seq(
    libraryDependencies ++= Seq(compilerPlugin("com.github.ghik" % "silencer-plugin" % silencerVersion cross CrossVersion.full)),
    // silence all warnings on autogenerated files
    scalacOptions += "-P:silencer:pathFilters=target/.*",
    // Make sure you only exclude warnings for the project directories, i.e. make builds reproducible
    scalacOptions += s"-P:silencer:sourceRoots=${baseDirectory.value.getCanonicalPath}"
  )
}
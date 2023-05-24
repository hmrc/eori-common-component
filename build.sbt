import com.typesafe.sbt.packager.MappingsHelper._
import play.core.PlayVersion
import sbt.Keys._
import sbt._
import uk.gov.hmrc.DefaultBuildSettings.{defaultSettings, targetJvm}
import uk.gov.hmrc.sbtdistributables.SbtDistributablesPlugin
import uk.gov.hmrc.sbtdistributables.SbtDistributablesPlugin._

import scala.language.postfixOps

Universal / mappings ++= directory(baseDirectory.value / "public")
// my understanding is publishing processed changed when we moved to the open and
// now it is done in production mode (was in dev previously). hence, we encounter the problem accessing "public" folder
// see https://stackoverflow.com/questions/36906106/reading-files-from-public-folder-in-play-framework-in-production

name := "eori-common-component"

PlayKeys.devSettings := Seq("play.server.http.port" -> "6752")

majorVersion := 0

targetJvm := "jvm-11"

scalaVersion := "2.13.8"

Test / fork := false

lazy val microservice = (project in file("."))
  .enablePlugins(play.sbt.PlayScala, SbtDistributablesPlugin)
  .disablePlugins(sbt.plugins.JUnitXmlReportPlugin)
  .settings(commonSettings, scoverageSettings, silencerSettings)

lazy val commonSettings: Seq[Setting[_]] = publishingSettings ++ defaultSettings()

lazy val scoverageSettings: Seq[Setting[_]] = Seq(
  coverageExcludedPackages := "<empty>;Reverse.*;uk.gov.hmrc.customs.managesubscription.config.*;.*(BuildInfo|Routes).*;.*ConfigModule.*;.*ConfigValidationNelAdaptor.*;.*ErrorResponse.*",
  coverageMinimumStmtTotal := 93, // TODO Increase to 95%
  coverageFailOnMinimum := false,
  coverageHighlighting := true,
  Test / parallelExecution := false
)

scalastyleConfig := baseDirectory.value / "project" / "scalastyle-config.xml"

Test / javaOptions += "-Dlogger.resource=logback-test.xml"

libraryDependencies ++= Seq(
  ws exclude ("org.apache.httpcomponents", "httpclient") exclude ("org.apache.httpcomponents", "httpcore"),
  "uk.gov.hmrc"            %% "bootstrap-backend-play-28" % "7.15.0",
  "uk.gov.hmrc"            %% "http-caching-client"       % "10.0.0-play-28",
  "uk.gov.hmrc.mongo"      %% "hmrc-mongo-play-28"        % "1.1.0",
  "com.typesafe.play"      %% "play-json-joda"            % "2.9.4",
  "com.typesafe.play"      %% "play-test"                 % PlayVersion.current % "test",
  "org.scalatestplus.play" %% "scalatestplus-play"        % "5.1.0"             % "test",
  "com.github.tomakehurst"  % "wiremock-standalone"       % "2.27.2"            % "test",
  "org.pegdown"             % "pegdown"                   % "1.6.0",
  "uk.gov.hmrc.mongo"      %% "hmrc-mongo-test-play-28"   % "1.1.0"            % Test,
  "org.mockito"            %% "mockito-scala"             % "1.17.14"            % Test,
  "org.scalatest"          %% "scalatest"                 % "3.2.15"             % Test,
  "com.vladsch.flexmark"    % "flexmark-all"              % "0.64.0"             % "test",
  "uk.gov.hmrc"       %% "bootstrap-test-play-28"    % "7.15.0" % "test",
)

lazy val silencerSettings: Seq[Setting[_]] = {
  val silencerVersion = "1.7.12"
  Seq(
    libraryDependencies ++= Seq(
      compilerPlugin("com.github.ghik" % "silencer-plugin" % silencerVersion cross CrossVersion.full)
    ),
    // silence all warnings on autogenerated files
    scalacOptions += "-P:silencer:pathFilters=target/.*",
    // Make sure you only exclude warnings for the project directories, i.e. make builds reproducible
    scalacOptions += s"-P:silencer:sourceRoots=${baseDirectory.value.getCanonicalPath}"
  )
}

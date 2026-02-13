import com.typesafe.sbt.packager.MappingsHelper.*
import sbt.*
import sbt.Keys.*
import uk.gov.hmrc.DefaultBuildSettings
import uk.gov.hmrc.DefaultBuildSettings.defaultSettings
import uk.gov.hmrc.sbtdistributables.SbtDistributablesPlugin

import scala.language.postfixOps

Universal / mappings ++= directory(baseDirectory.value / "public")
// my understanding is publishing processed changed when we moved to the open and
// now it is done in production mode (was in dev previously). hence, we encounter the problem accessing "public" folder
// see https://stackoverflow.com/questions/36906106/reading-files-from-public-folder-in-play-framework-in-production

name := "eori-common-component"

PlayKeys.devSettings := Seq("play.server.http.port" -> "6752")

ThisBuild / majorVersion := 0

ThisBuild / scalaVersion := "3.3.7"

Test / fork := false

lazy val microservice = (project in file("."))
  .enablePlugins(play.sbt.PlayScala, SbtDistributablesPlugin)
  .disablePlugins(sbt.plugins.JUnitXmlReportPlugin)
  .settings(commonSettings, scoverageSettings, excludeDependencies += ExclusionRule("org.lz4", "lz4-java"))

lazy val commonSettings: Seq[Setting[_]] = defaultSettings()

lazy val scoverageSettings: Seq[Setting[_]] = Seq(
  coverageExcludedPackages := "<empty>;Reverse.*;uk.gov.hmrc.customs.managesubscription.config.*;.*(BuildInfo|Routes).*;.*ConfigModule.*;.*ConfigValidationNelAdaptor.*;.*ErrorResponse.*",
  coverageMinimumStmtTotal := 90,
  coverageFailOnMinimum    := false,
  coverageHighlighting     := true,
  Test / parallelExecution := false
)

scalastyleConfig := baseDirectory.value / "project" / "scalastyle-config.xml"

Test / javaOptions += "-Dlogger.resource=logback-test.xml"

libraryDependencies ++= AppDependencies.compile ++ AppDependencies.test

lazy val it = project
  .enablePlugins(PlayScala)
  .dependsOn(microservice % "test->test") // the "test->test" allows reusing test code and test dependencies
  .settings(libraryDependencies ++= AppDependencies.test)

import play.core.PlayVersion
import play.sbt.PlayImport._
import sbt.Keys.libraryDependencies
import sbt._

object AppDependencies {

  val bootstrapVersion = "7.19.0"

  val compile = Seq(
    "uk.gov.hmrc"             %% "bootstrap-backend-play-28"      % bootstrapVersion,
    "uk.gov.hmrc"             %% "http-caching-client"            % "10.0.0-play-28",
    "uk.gov.hmrc.mongo"       %% "hmrc-mongo-play-28"             % "1.1.0",
    "com.typesafe.play"       %% "play-json-joda"                 % "2.9.4",
    "uk.gov.hmrc"             %% "internal-auth-client-play-28"   % "1.3.0"
  )

  val test = Seq(
    "com.typesafe.play"       %% "play-test"                      % PlayVersion.current  % "test,it",
    "org.scalatestplus.play"  %% "scalatestplus-play"             % "5.1.0"              % "test,it",
    "com.github.tomakehurst"  %  "wiremock-standalone"            % "2.27.2"             % "test,it",
    "org.pegdown"             %  "pegdown"                        % "1.6.0"              % "test,it",
    "uk.gov.hmrc.mongo"       %% "hmrc-mongo-test-play-28"        % "1.1.0"              % "test,it",
    "org.mockito"             %% "mockito-scala"                  % "1.17.14"            % "test,it",
    "org.scalatest"           %% "scalatest"                      % "3.2.15"             % "test,it",
    "com.vladsch.flexmark"    %  "flexmark-all"                   % "0.64.0"             % "test,it",
    "uk.gov.hmrc"             %% "bootstrap-test-play-28"         % bootstrapVersion     % "test,it"
  )
}

//Seq(ws exclude ("org.apache.httpcomponents", "httpclient") exclude ("org.apache.httpcomponents", "httpcore"))

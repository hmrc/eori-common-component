import play.core.PlayVersion
import sbt.*

object AppDependencies {

  val mongoDbVersion   = "1.7.0"
  val bootstrapVersion = "8.4.0"
  val playVersion      = 30

  val compile: Seq[ModuleID] = Seq(
    "uk.gov.hmrc"       %% s"bootstrap-backend-play-$playVersion"    % bootstrapVersion,
    "uk.gov.hmrc.mongo" %% s"hmrc-mongo-play-$playVersion"           % mongoDbVersion,
    "uk.gov.hmrc"       %% s"internal-auth-client-play-$playVersion" % "1.10.0",
    "org.typelevel"     %% "cats-core"                               % "2.10.0"
  )

  val test: Seq[ModuleID] = Seq(
    "org.playframework"      %% "play-test"                          % PlayVersion.current % "test",
    "org.scalatestplus.play" %% "scalatestplus-play"                 % "7.0.1"             % "test",
    "uk.gov.hmrc.mongo"      %% s"hmrc-mongo-test-play-$playVersion" % mongoDbVersion      % "test",
    "org.mockito"            %% "mockito-scala"                      % "1.17.30"           % "test",
    "org.scalatest"          %% "scalatest"                          % "3.2.17"            % "test",
    "com.vladsch.flexmark"    % "flexmark-all"                       % "0.64.8"            % "test",
    "uk.gov.hmrc"            %% s"bootstrap-test-play-$playVersion"  % bootstrapVersion    % "test"
  )

}

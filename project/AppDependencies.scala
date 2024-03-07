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
    "org.playframework"      %% "play-test"                          % PlayVersion.current,
    "org.scalatestplus.play" %% "scalatestplus-play"                 % "7.0.1",
    "uk.gov.hmrc.mongo"      %% s"hmrc-mongo-test-play-$playVersion" % mongoDbVersion,
    "org.mockito"            %% "mockito-scala"                      % "1.17.30",
    "org.scalatest"          %% "scalatest"                          % "3.2.17",
    "com.vladsch.flexmark"    % "flexmark-all"                       % "0.64.8",
    "uk.gov.hmrc"            %% s"bootstrap-test-play-$playVersion"  % bootstrapVersion
  ).map(_ % Test)

}

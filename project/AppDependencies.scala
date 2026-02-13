import play.core.PlayVersion
import sbt.*

object AppDependencies {

  val mongoDbVersion   = "2.12.0"
  val bootstrapVersion = "10.5.0"
  val playVersion      = 30

  val compile: Seq[ModuleID] = Seq(
    "uk.gov.hmrc"       %% s"bootstrap-backend-play-$playVersion"    % bootstrapVersion exclude("org.apache.commons", "commons-lang3"),
    "org.apache.commons" % "commons-lang3"                           % "3.18.0",
    "uk.gov.hmrc.mongo" %% s"hmrc-mongo-play-$playVersion"           % mongoDbVersion,
    "uk.gov.hmrc"       %% s"internal-auth-client-play-$playVersion" % "4.3.0",
    "org.typelevel"     %% "cats-core"                               % "2.10.0",
    "at.yawk.lz4"        %  "lz4-java"                               % "1.10.3",
    "ch.qos.logback"     % "logback-core"                            % "1.5.21",

  )

  val test: Seq[ModuleID] = Seq(
    "org.playframework"      %% "play-test"                          % PlayVersion.current,
    "org.scalatestplus.play" %% "scalatestplus-play"                 % "7.0.1",
    "uk.gov.hmrc.mongo"      %% s"hmrc-mongo-test-play-$playVersion" % mongoDbVersion,
    "org.scalatestplus"      %% "mockito-4-11"                       % "3.2.17.0",
    "uk.gov.hmrc"            %% s"bootstrap-test-play-$playVersion"  % bootstrapVersion
  ).map(_ % Test)

}

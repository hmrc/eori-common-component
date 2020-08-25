resolvers += Resolver.url("HMRC Sbt Plugin Releases", url("https://dl.bintray.com/hmrc/sbt-plugin-releases"))(Resolver.ivyStylePatterns)

resolvers += "HMRC Releases" at "https://dl.bintray.com/hmrc/releases"

// sbt-auto-build after upgrade has an error with LICENSE file
// Private repositories shouldn't have LICENSE files, this upagrade will be done after cloning repository to be public
// In that case there won't be issue here
// addSbtPlugin("uk.gov.hmrc" % "sbt-auto-build" % "2.9.0")
addSbtPlugin("uk.gov.hmrc" % "sbt-auto-build" % "2.2.0")

addSbtPlugin("uk.gov.hmrc" % "sbt-git-versioning" % "2.1.0")

addSbtPlugin("uk.gov.hmrc" % "sbt-artifactory" % "1.5.0")

addSbtPlugin("uk.gov.hmrc" % "sbt-distributables" % "2.0.0")

addSbtPlugin("com.typesafe.play" % "sbt-plugin" % "2.6.23")

addSbtPlugin("org.scoverage" % "sbt-scoverage" % "1.5.1")

addSbtPlugin("com.typesafe.sbt" % "sbt-web" % "1.4.3")

addSbtPlugin("org.scalastyle" % "scalastyle-sbt-plugin" % "1.0.0")
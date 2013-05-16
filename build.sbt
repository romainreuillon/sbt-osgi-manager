name := "sbt-osgi-manager"

organization := "sbt.osgi.manager"

version := "0.0.1.0-SNAPSHOT"

scalacOptions ++= Seq("-unchecked", "-deprecation", "-Xcheckinit", "-Xfatal-warnings")

sbtPlugin := true

libraryDependencies ++= {
  val mavenVersion = "3.0.5"
  val mavenWagonVersion = "2.4"
  Seq(
    "biz.aQute" % "bndlib" % "2.0.0.20130123-133441",
    "org.apache.felix" % "org.apache.felix.resolver" % "1.0.0",
    "org.apache.maven" % "maven-aether-provider" % mavenVersion,
    "org.apache.maven" % "maven-artifact" % mavenVersion,
    "org.apache.maven" % "maven-compat" % mavenVersion,
    "org.apache.maven" % "maven-core" % mavenVersion,
    "org.apache.maven" % "maven-plugin-api" % mavenVersion,
    "org.apache.maven" % "maven-embedder" % mavenVersion, // provide org.apache.maven.cli.MavenCli
    "org.apache.maven.wagon" % "wagon-http" % mavenWagonVersion, // HTTP connector for remore repositories
    "org.apache.maven.wagon" % "wagon-file" % mavenWagonVersion, // File connector for local repositories
    "org.eclipse.tycho" % "tycho-core" % "0.17.0",
    "org.eclipse.tycho" % "tycho-p2-facade" % "0.17.0", // Tycho p2 Resolver Component
    "org.osgi" % "org.osgi.core" % "5.0.0",
    "org.osgi" % "org.osgi.enterprise" % "5.0.0",
    "org.sonatype.aether" % "aether-connector-wagon" % "1.13.1"
  )
}

import Dependencies._

ThisBuild / scalaVersion := "2.12.8"
ThisBuild / version := "0.1.0-SNAPSHOT"
ThisBuild / organization := "com.example"
ThisBuild / organizationName := "example"

resolvers +=
  "Sonatype OSS Snapshots" at "https://oss.sonatype.org/content/repositories/snapshots"

lazy val root = (project in file("."))
  .settings(
    name := "tuapse",
    libraryDependencies += scalaTest % Test,
    libraryDependencies += "com.github.zella" % "rx-process2" % "0.1.0-RC3",
    libraryDependencies += "com.github.davidmoten" % "rxjava2-extras" % "0.1.33",
    libraryDependencies += "io.reactivex.rxjava2" % "rxjava" % "2.2.8",
    libraryDependencies += "com.fasterxml.jackson.core" % "jackson-databind" % "2.9.8",
    libraryDependencies += "com.fasterxml.jackson.core" % "jackson-core" % "2.9.8",
    libraryDependencies += "commons-io" % "commons-io" % "2.6",
    libraryDependencies += "org.elasticsearch.client" % "transport" % "7.0.1",
    libraryDependencies += "org.elasticsearch.client" % "elasticsearch-rest-high-level-client" % "7.0.1",

    libraryDependencies += "ch.qos.logback" % "logback-classic" % "1.2.3",

    //    libraryDependencies += "org.xerial" % "sqlite-jdbc" % "3.27.2.1",
    libraryDependencies += "junit" % "junit" % "4.11" % Test,
    libraryDependencies += "com.novocode" % "junit-interface" % "0.11" % Test
  )

// META-INF discarding
assemblyMergeStrategy in assembly := {
    case PathList("META-INF", "MANIFEST.MF") => MergeStrategy.discard
    case x => MergeStrategy.last
}

mainClass in assembly := Some("org.zella.tuapse.Runner")

assemblyOutputPath in assembly := file("build/assembly.jar")


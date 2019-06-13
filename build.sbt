import Dependencies._

ThisBuild / scalaVersion := "2.12.8"
ThisBuild / version := "0.1.0-SNAPSHOT"
ThisBuild / organization := "org.zella"
ThisBuild / organizationName := "zella"

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
    //es 7.1.1 uses lucene 8.0.0, some problem use latest lucene on classpath TODO
    libraryDependencies += "org.elasticsearch.client" % "transport" % "7.1.1",
    libraryDependencies += "org.elasticsearch.client" % "elasticsearch-rest-high-level-client" % "7.1.1",
    libraryDependencies += "org.apache.lucene" % "lucene-queryparser" % "8.1.1",
    libraryDependencies += "org.apache.lucene" % "lucene-analyzers-common" % "8.1.1",
    libraryDependencies += "org.apache.lucene" % "lucene-core" % "8.0.0",
    libraryDependencies += "org.apache.lucene" % "lucene-queries" % "8.0.0",
    libraryDependencies += "org.apache.lucene" % "lucene-highlighter" % "8.0.0",
    libraryDependencies += "io.vertx" % "vertx-rx-java2" % "3.7.1",
    libraryDependencies += "io.vertx" % "vertx-web" % "3.7.1",
    libraryDependencies += "io.vertx" % "vertx-web-templ-handlebars" % "3.7.1",
    libraryDependencies += "ch.qos.logback" % "logback-classic" % "1.2.3",
    libraryDependencies += "com.google.guava" % "guava" % "27.1-jre",
    libraryDependencies += "org.apache.commons" % "commons-lang3" % "3.9",
    libraryDependencies += "junit" % "junit" % "4.11" % Test,
    libraryDependencies += "com.novocode" % "junit-interface" % "0.11" % Test,
    libraryDependencies += "com.google.truth" % "truth" % "0.44" % Test,
    libraryDependencies += "org.testcontainers" % "testcontainers" % "1.11.3" % Test,
    libraryDependencies += "org.testcontainers" % "elasticsearch" % "1.11.3" % Test
  )

assemblyMergeStrategy in assembly := {
  case x if x.contains("io.netty.versions.properties") => MergeStrategy.discard
  case x if x.contains("libjnidispatch.so") => MergeStrategy.last
  case x =>
    val oldStrategy = (assemblyMergeStrategy in assembly).value
    oldStrategy(x)
}

mainClass in assembly := Some("org.zella.tuapse.Runner")

assemblyOutputPath in assembly := file("build/assembly.jar")

test in assembly := {}

crossPaths := false

autoScalaLibrary := false


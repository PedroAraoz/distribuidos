
/**
 * Importing from IDEA
 * Bug: "HelloProto is already defined as object HelloProto"
 *
 * 1) Open Module Settings (select scalapb-demo in the tree and press F4)
 * 2) From Sources, remove one of:
 *
 *    target/scala_2.12/src_managed/main
 *    target/scala_2.12/src_managed/main/scalapb   <- [remove]
 *
 */
scalaVersion := "2.12.13"

val circeVersion = "0.12.3"

// Snapshot repository required for etcd 0.5.5-SNAPSHOT
resolvers +=
  "Sonatype OSS Snapshots" at "https://oss.sonatype.org/content/repositories/snapshots"

/**
 * Docker Support
 */

enablePlugins(JavaAppPackaging)
dockerExposedPorts ++= Seq(50000, 50001)

/**
 * Docker Usage:
 *
 * Generate Image:
 *
 *    sbt docker:publishLocal
 *
 * List scripts (one script for each main class):
 *
 *    docker run --entrypoint "/bin/ls"  -it scalapb-demo:0.1.0-SNAPSHOT /opt/docker/bin
 *
 * Run app:
 *
 *    docker run -p 50000:50000 --entrypoint "/opt/docker/bin/hello-world-server" -it scalapb-demo:0.1.0-SNAPSHOT
 *    docker run -p 50000:50000 --entrypoint "/opt/docker/bin/etcd-demo" -it scalapb-demo:0.1.0-SNAPSHOT
 */

libraryDependencies ++= Seq(
  "io.circe" %% "circe-core",
  "io.circe" %% "circe-generic",
  "io.circe" %% "circe-parser"
).map(_ % circeVersion)

libraryDependencies ++= Seq(
  "org.scalaj" %% "scalaj-http" % "2.4.2",
  "com.lihaoyi" %% "os-lib" % "0.7.3",
  "com.lihaoyi" %% "upickle" % "0.9.5",
  "com.fasterxml.jackson.module" %% "jackson-module-scala" % "2.12.1",
  "io.grpc" % "grpc-netty" % scalapb.compiler.Version.grpcJavaVersion,
  "com.thesamet.scalapb" %% "scalapb-runtime-grpc" % scalapb.compiler.Version.scalapbVersion
)

//libraryDependencies += "io.etcd" % "jetcd-core" % "0.5.4"
libraryDependencies += "io.etcd" % "jetcd-core" % "0.5.5-SNAPSHOT"

libraryDependencies += "org.slf4j" % "slf4j-api" % "1.7.30"
libraryDependencies += "org.slf4j" % "slf4j-simple" % "1.7.30"

Compile / PB.targets := Seq(
  scalapb.gen() -> (Compile / sourceManaged).value / "scalapb"
)


// (optional) If you need scalapb/scalapb.proto or anything from
// google/protobuf/*.proto
libraryDependencies ++= Seq(
  "com.thesamet.scalapb" %% "scalapb-runtime" % scalapb.compiler.Version.scalapbVersion % "protobuf"
)

libraryDependencies += "com.thesamet.scalapb" %% "scalapb-json4s" % "0.11.0"
libraryDependencies += "io.monix" %% "shade" % "1.10.0"
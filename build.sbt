//
// Define dependency versions
//
lazy val akkaVer = "2.4.0"
lazy val logbackVer = "1.1.3"
lazy val scalaTestVer = "2.2.4"

//
// Define dependencies
//
lazy val akkaSlf4j = "com.typesafe.akka"        %% "akka-slf4j"                    % akkaVer
lazy val logback   = "ch.qos.logback"           %  "logback-classic"               % logbackVer
lazy val scalaTest = "org.scalatest"            %% "scalatest"                     % scalaTestVer

//
// Shared settings
//
lazy val commonSettings = Seq(
  organization := "hyperion",
  version := "1.0.0-SNAPSHOT",
  description := "",
  scalaVersion := "2.11.7",
  scalacOptions ++= Seq(
    "-unchecked",
    "-deprecation",
    "-language:_",
    "-target:jvm-1.8",
    "-encoding", "UTF-8"
  ),
  licenses := Seq(
    ("MIT License", url("http://www.opensource.org/licenses/mit-license.php"))
  ),
  scmInfo := Some(
    ScmInfo(
      url("https://github.com/mthmulders/hyperion"),
      "scm:git:git@github.com:mthmulders/hyperion.git",
      Some("scm:git:git@github.com:mthmulders/hyperion.git")
    )
  )
)

//
// Per-module settings
//

lazy val meterAgent = (project in file("meter-agent"))
  .settings(commonSettings: _*)
  .settings(Seq(
    name := "hyperion-meter-agent",
    libraryDependencies ++= Seq(
      akkaSlf4j,
      logback,
      scalaTest % "test"
    )
  ))

lazy val root = (project in file("."))
  .settings(commonSettings: _*)
  .aggregate(meterAgent)



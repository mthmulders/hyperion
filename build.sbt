//
// Define dependency versions
//
lazy val akkaVer = "2.4.0"
lazy val flowVer = "2.3.0"
lazy val logbackVer = "1.1.3"
lazy val parserCombVer = "1.0.4"
lazy val scalaTestVer = "2.2.4"

//
// Define dependencies
//
lazy val akkaActor   = "com.typesafe.akka"        %% "akka-actor"                    % akkaVer
lazy val akkaSlf4j   = "com.typesafe.akka"        %% "akka-slf4j"                    % akkaVer
lazy val akkaTestKit = "com.typesafe.akka"        %% "akka-testkit"                  % akkaVer
lazy val flow        = "com.github.jodersky"      %% "flow"                          % flowVer
lazy val flowNative  = "com.github.jodersky"      %  "flow-native"                   % flowVer
lazy val logback     = "ch.qos.logback"           %  "logback-classic"               % logbackVer
lazy val parserComb  = "org.scala-lang.modules"   %% "scala-parser-combinators"      % parserCombVer
lazy val scalaTest   = "org.scalatest"            %% "scalatest"                     % scalaTestVer

//
// Shared settings
//
lazy val commonSettings = Seq(
  organization := "hyperion",
  version := "1.0.0-SNAPSHOT",
  description := "",
  scalaVersion := "2.11.7",
  test in assembly := {},
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

lazy val meteragent = (project in file("meter-agent"))
  .settings(commonSettings: _*)
  .settings(Seq(
    name := "hyperion-meter-agent",
    assemblyJarName in assembly := "hyperion-meter-agent.jar",
    libraryDependencies ++= Seq(
      akkaActor,
      akkaSlf4j,
      akkaTestKit % "test",
      flow,
      flowNative,
      logback,
      parserComb,
      scalaTest % "test"
    )
  ))

lazy val root = (project in file("."))
  .settings(commonSettings: _*)
  .aggregate(meteragent)



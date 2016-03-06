//
// Define dependency versions
//
lazy val akkaVer = "2.4.0"
lazy val flowVer = "2.4.1"
lazy val logbackVer = "1.1.3"
lazy val mockitoVer = "1.10.19"
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
lazy val mockito     = "org.mockito"              %  "mockito-core"                  % mockitoVer
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

lazy val testSupport = (project in file("test-support"))
  .settings(commonSettings: _*)
  .settings(Seq(
    name := "hyperion-test-support",
    libraryDependencies ++= Seq(
      akkaActor,
      akkaSlf4j,
      akkaTestKit % "test",
      logback,
      mockito % "test",
      scalaTest % "test"
    )
  )
)

lazy val meterAgent = (project in file("meter-agent"))
  .enablePlugins(JavaServerAppPackaging)
  .settings(commonSettings: _*)
  .settings(Seq(
    name := "hyperion-meter-agent",
    resolvers += Resolver.bintrayRepo("jodersky", "maven"),
    libraryDependencies ++= Seq(
      akkaActor,
      akkaSlf4j,
      akkaTestKit % "test",
      flow,
      flowNative,
      logback,
      mockito % "test",
      parserComb,
      scalaTest % "test"
    ),
    packageName in Linux := "hyperion-meter-agent",
    maintainer in Linux := "Maarten Mulders",
    packageSummary in Linux := "Hyperion meter agent",
    packageDescription in Linux := "The Hyperion meter agent that connects the Smart Meter to Hyperion",
    mappings in Universal += {
      sourceDirectory.value / "main" / "deb" / "application.conf" -> "conf/meter-agent.conf"
    },
    daemonUser in Linux := "hyperion",
    daemonGroup in Linux := "hyperion",
    serverLoading in Debian := com.typesafe.sbt.packager.archetypes.ServerLoader.SystemV,
    debianPackageDependencies in Debian ++= Seq("oracle-java8-jdk"),
    bashScriptExtraDefines += """addJava "-Dconfig.file=${app_home}/../conf/meter-agent.conf""""
  )
).dependsOn(testSupport % "test->test")

// TODO [MM] This user should be in a secondary group, 'tty'.
// Maybe this can be done with maintainerScripts in Debian; see
// http://www.scala-sbt.org/sbt-native-packager/formats/debian.html

lazy val core = (project in file("core"))
  .settings(commonSettings: _*)
  .settings(Seq(
    name := "hyperion-core",
    libraryDependencies ++= Seq(
      akkaActor,
      akkaSlf4j,
      akkaTestKit % "test",
      logback
    )
  )
).dependsOn(testSupport % "test->test")

lazy val root = (project in file("."))
  .settings(commonSettings: _*)
  .aggregate(meterAgent, core)

//
// Define dependency versions
//
lazy val akkaVer = "2.4.0"
lazy val flowVer = "2.4.1"
lazy val logbackVer = "1.1.3"
lazy val mockitoVer = "1.10.19"
lazy val parserCombVer = "1.0.4"
lazy val scalaTestVer = "2.2.4"
lazy val sprayVer = "1.3.2"
lazy val sprayWsVer = "0.1.4"

//
// Define dependencies
//
lazy val akkaActor   = "com.typesafe.akka"        %% "akka-actor"                    % akkaVer
lazy val akkaSlf4j   = "com.typesafe.akka"        %% "akka-slf4j"                    % akkaVer
lazy val akkaRemote  = "com.typesafe.akka"        %% "akka-remote"                   % akkaVer
lazy val akkaTestKit = "com.typesafe.akka"        %% "akka-testkit"                  % akkaVer
lazy val flow        = "com.github.jodersky"      %% "flow"                          % flowVer
lazy val flowNative  = "com.github.jodersky"      %  "flow-native"                   % flowVer
lazy val logback     = "ch.qos.logback"           %  "logback-classic"               % logbackVer
lazy val mockito     = "org.mockito"              %  "mockito-core"                  % mockitoVer
lazy val parserComb  = "org.scala-lang.modules"   %% "scala-parser-combinators"      % parserCombVer
lazy val scalaTest   = "org.scalatest"            %% "scalatest"                     % scalaTestVer
lazy val sprayCan    = "io.spray"                 %% "spray-can"                     % sprayVer
lazy val sprayHttpx  = "io.spray"                 %% "spray-httpx"                   % sprayVer
lazy val sprayJson   = "io.spray"                 %% "spray-json"                    % sprayVer
lazy val sprayWS     = "com.wandoulabs.akka"      %% "spray-websocket"               % sprayWsVer


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

lazy val common = (project in file("common"))
  .settings(commonSettings: _*)
  .settings(Seq(
    name := "hyperion-common"
  )
)

lazy val testSupport = (project in file("test-support"))
  .settings(commonSettings: _*)
  .settings(Seq(
    name := "hyperion-test-support",
    libraryDependencies ++= Seq(
      akkaActor,
      akkaSlf4j,
      akkaRemote,
      akkaTestKit % "test",
      logback,
      mockito % "test",
      scalaTest % "test"
    )
  )
).dependsOn(common)

lazy val meterAgent = (project in file("meter-agent"))
  .enablePlugins(JavaServerAppPackaging)
  .settings(commonSettings: _*)
  .settings(Seq(
    name := "hyperion-meter-agent",
    resolvers += Resolver.bintrayRepo("jodersky", "maven"),
    libraryDependencies ++= Seq(
      akkaActor,
      akkaSlf4j,
      akkaRemote,
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
    bashScriptExtraDefines += """addJava "-Dconfig.file=${app_home}/../conf/meter-agent.conf"""",
    maintainerScripts in Debian := maintainerScriptsAppend((maintainerScripts in Debian).value)(
      DebianConstants.Postinst -> "usermod -a -G dialout hyperion"
    )
  )
).dependsOn(common, testSupport % "test->test")

lazy val core = (project in file("core"))
  .enablePlugins(JavaServerAppPackaging)
  .settings(commonSettings: _*)
  .settings(Seq(
    name := "hyperion-core",
    libraryDependencies ++= Seq(
      akkaActor,
      akkaSlf4j,
      akkaRemote,
      akkaTestKit % "test",
      logback,
      sprayCan,
      sprayHttpx,
      sprayJson,
      sprayWS
    ),
    packageName in Linux := "hyperion-core",
    maintainer in Linux := "Maarten Mulders",
    packageSummary in Linux := "Hyperion core",
    packageDescription in Linux := "The Hyperion core that collects and stores data",
    mappings in Universal += {
      sourceDirectory.value / "main" / "deb" / "application.conf" -> "conf/core.conf"
    },
    daemonUser in Linux := "hyperion",
    daemonGroup in Linux := "hyperion",
    serverLoading in Debian := com.typesafe.sbt.packager.archetypes.ServerLoader.SystemV,
    debianPackageDependencies in Debian ++= Seq("oracle-java8-jdk"),
    bashScriptExtraDefines += """addJava "-Dconfig.file=${app_home}/../conf/core.conf""""
  )
).dependsOn(common, testSupport % "test->test")

lazy val web = (project in file("web"))
  .enablePlugins(SbtNativePackager)
  .settings(Seq(
    packageName in Linux := "hyperion-web",
    maintainer in Linux := "Maarten Mulders",
    packageSummary in Linux := "Hyperion Web frontend",
    packageDescription in Linux := "The Hyperion Web frontend that displays data",
    mappings in Universal += {
      sourceDirectory.value / "build" -> "/var/www/hyperion"
    }
  )
)

lazy val root = (project in file("."))
  .settings(commonSettings: _*)
  .aggregate(core, meterAgent, web)

//
// Define dependency versions
//
lazy val akkaVer = "2.4.4"
lazy val flowVer = "2.4.1"
lazy val logbackVer = "1.1.3"
lazy val mockitoVer = "1.10.19"
lazy val mysqlVer = "5.1.39"
lazy val parserCombVer = "1.0.4"
lazy val scalaTestVer = "2.2.6"
lazy val slickVer = "3.1.1"
lazy val sprayVer = "1.3.2"
lazy val sprayWsVer = "0.1.4"

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
lazy val mysql       = "mysql"                    %  "mysql-connector-java"          % mysqlVer
lazy val parserComb  = "org.scala-lang.modules"   %% "scala-parser-combinators"      % parserCombVer
lazy val scalaTest   = "org.scalatest"            %% "scalatest"                     % scalaTestVer
lazy val slick       = "com.typesafe.slick"       %% "slick"                         % slickVer
lazy val sprayCan    = "io.spray"                 %% "spray-can"                     % sprayVer
lazy val sprayHttpx  = "io.spray"                 %% "spray-httpx"                   % sprayVer
lazy val sprayJson   = "io.spray"                 %% "spray-json"                    % sprayVer
lazy val sprayWS     = "com.wandoulabs.akka"      %% "spray-websocket"               % sprayWsVer


//
// Shared settings
//
lazy val commonSettings = Seq(
  organization := "hyperion",
  version := "2.0.0-SNAPSHOT",
  description := "Hyperion",
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

lazy val app = (project in file("app"))
  .enablePlugins(JavaServerAppPackaging)
  .settings(commonSettings: _*)
  .settings(Seq(
    name := "hyperion",
    resolvers += Resolver.bintrayRepo("jodersky", "maven"),
    libraryDependencies ++= Seq(
      akkaActor,
      akkaSlf4j,
      akkaTestKit % "test",
      flow,
      flowNative,
      logback,
      mockito % "test",
      mysql,
      parserComb,
      scalaTest % "test",
      slick,
      sprayCan,
      sprayHttpx,
      sprayJson,
      sprayWS
    ),
    packageName in Linux := "hyperion",
    maintainer in Linux := "Maarten Mulders",
    packageSummary in Linux := "Hyperion",
    packageDescription in Linux := "The Hyperion system that shows realtime data from a Smart Meter",
    mappings in Universal += {
      sourceDirectory.value / "main" / "deb" / "application.conf" -> "conf/hyperion.conf"
    },
    daemonUser in Linux := "hyperion",
    daemonGroup in Linux := "hyperion",
    serverLoading in Debian := com.typesafe.sbt.packager.archetypes.ServerLoader.SystemV,
    debianPackageDependencies in Debian ++= Seq("oracle-java8-jdk"),
    bashScriptExtraDefines += """addJava "-Dconfig.file=${app_home}/../conf/hyperion.conf"""",
    maintainerScripts in Debian := maintainerScriptsAppend((maintainerScripts in Debian).value)(
      DebianConstants.Postinst -> "usermod -a -G dialout hyperion"
    )
  )
).dependsOn(common)

lazy val testSupport = (project in file("test-support"))
  .settings(commonSettings: _*)
  .settings(Seq(
    name := "hyperion-test-support",
    libraryDependencies ++= Seq(
      akkaActor,
      akkaSlf4j,
      logback
    )
  )
).dependsOn(app % "test->test")

lazy val root = (project in file("."))
  .settings(commonSettings: _*)
  .aggregate(app, common, testSupport)

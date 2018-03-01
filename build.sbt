//
// Define dependency versions
//
val akkaVer = "2.5.4"
val akkaHttpVer = "10.0.10"
val akkaSerialVer = "4.1.1"
val logbackVer = "1.2.3"
val parserCombVer = "1.0.7"
val postgresqlVer = "42.1.4"
val restAssuredVer = "3.0.7"
val scalaMockVer = "3.6.0"
val scalaTestVer = "3.0.5"
val slickVer = "3.2.1"
val sprayJsonVer = "1.3.3"
val sprayWsVer = "0.1.4"

//
// Define dependencies
//
val akkaActor        = "com.typesafe.akka"      %% "akka-actor"                  % akkaVer
val akkaHttp         = "com.typesafe.akka"      %% "akka-http"                   % akkaHttpVer
val akkaHttpJson     = "com.typesafe.akka"      %% "akka-http-spray-json"        % akkaHttpVer
val akkaHttpTest     = "com.typesafe.akka"      %% "akka-http-testkit"           % akkaHttpVer
val akkaSerial       = "ch.jodersky"            %% "akka-serial-core"            % akkaSerialVer
val akkaSerialNative = "ch.jodersky"            %  "akka-serial-native"          % akkaSerialVer
val akkaStream       = "com.typesafe.akka"      %% "akka-stream"                 % akkaVer
val akkaSlf4j        = "com.typesafe.akka"      %% "akka-slf4j"                  % akkaVer
val akkaTestKit      = "com.typesafe.akka"      %% "akka-testkit"                % akkaVer
val logback          = "ch.qos.logback"         %  "logback-classic"             % logbackVer
val parserComb       = "org.scala-lang.modules" %% "scala-parser-combinators"    % parserCombVer
val postgresql       = "org.postgresql"         %  "postgresql"                  % postgresqlVer
val restAssured      = "io.rest-assured"        %  "rest-assured"                % restAssuredVer
val restAssuredScala = "io.rest-assured"        %  "scala-support"               % restAssuredVer
val scalaMock        = "org.scalamock"          %% "scalamock-scalatest-support" % scalaMockVer
val scalaTest        = "org.scalatest"          %% "scalatest"                   % scalaTestVer
val slick            = "com.typesafe.slick"     %% "slick"                       % slickVer
val sprayJson        = "io.spray"               %% "spray-json"                  % sprayJsonVer


//
// Shared settings
//
val commonSettings = Seq(
  organization := "hyperion",
  version := "2.1.0-SNAPSHOT",
  description := "Hyperion",
  scalaVersion := "2.12.3",
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

val app = (project in file("app"))
  .configs(IntegrationTest)
  .enablePlugins(JavaServerAppPackaging, SystemVPlugin)
  .enablePlugins(BuildInfoPlugin)
  .settings(commonSettings: _*)
  .settings(Defaults.itSettings)
  .settings(Seq(
    name := "hyperion",
    libraryDependencies ++= Seq(
      akkaActor,
      akkaHttp,
      akkaHttpJson,
      akkaHttpTest % "test",
      akkaSerial,
      akkaSerialNative,
      akkaStream,
      akkaSlf4j,
      akkaTestKit % "test",
      logback,
      parserComb,
      postgresql,
      restAssured % "it",
      restAssuredScala % "it",
      scalaMock % "test",
      scalaTest % "test,it",
      slick
    ),
    buildInfoOptions += BuildInfoOption.BuildTime,
    buildInfoPackage := "hyperion",
    packageName in Linux := "hyperion",
    maintainer in Linux := "Maarten Mulders",
    packageSummary in Linux := "Hyperion",
    packageDescription in Linux := "The Hyperion system that shows realtime data from a Smart Meter",
    mappings in Universal += {
      sourceDirectory.value / "main" / "deb" / "environment.conf" -> "conf/hyperion.conf"
    },
    daemonUser in Linux := "hyperion",
    daemonGroup in Linux := "hyperion",
    debianPackageDependencies in Debian ++= Seq("oracle-java8-jdk"),
    bashScriptExtraDefines += """addJava "-Dconfig.file=${app_home}/../conf/hyperion.conf"""",
    maintainerScripts in Debian := maintainerScriptsAppend((maintainerScripts in Debian).value)(
      DebianConstants.Postinst -> "usermod -a -G dialout hyperion"
    )
  )
)

val testApp = (project in file("test-app"))
  .settings(commonSettings: _*)
  .settings(Seq(
    name := "hyperion-test-app"
  )
).dependsOn(app % "compile->test")

val root = (project in file("."))
  .settings(commonSettings: _*)
  .aggregate(app, testApp)

//
// Define dependency versions
//
val akkaVer = "2.6.14"
val akkaHttpVer = "10.2.4"
val akkaSerialVer = "4.2.0"
val logbackVer = "1.2.3"
val parserCombVer = "2.0.0"
val postgresqlVer = "42.2.20"
val restAssuredVer = "4.3.3"
val scalaMockVer = "5.1.0"
val scalaTestVer = "3.2.9"
val slickVer = "3.3.3"
val sprayJsonVer = "1.3.6"

//
// Define dependencies
//
val akkaActor         = "com.typesafe.akka"      %% "akka-actor"                  % akkaVer
val akkaHttp          = "com.typesafe.akka"      %% "akka-http"                   % akkaHttpVer
val akkaHttpJson      = "com.typesafe.akka"      %% "akka-http-spray-json"        % akkaHttpVer
val akkaHttpTest      = "com.typesafe.akka"      %% "akka-http-testkit"           % akkaHttpVer
val akkaSerial        = "ch.jodersky"            %% "akka-serial-core"            % akkaSerialVer
val akkaSerialNative  = "ch.jodersky"            %  "akka-serial-native"          % akkaSerialVer
val akkaStream        = "com.typesafe.akka"      %% "akka-stream"                 % akkaVer
val akkaStreamTestkit = "com.typesafe.akka"      %% "akka-stream-testkit"         % akkaVer
val akkaSlf4j         = "com.typesafe.akka"      %% "akka-slf4j"                  % akkaVer
val akkaTestKit       = "com.typesafe.akka"      %% "akka-testkit"                % akkaVer
val logback           = "ch.qos.logback"         %  "logback-classic"             % logbackVer
val parserComb        = "org.scala-lang.modules" %% "scala-parser-combinators"    % parserCombVer
val postgresql        = "org.postgresql"         %  "postgresql"                  % postgresqlVer
val restAssured       = "io.rest-assured"        %  "rest-assured"                % restAssuredVer
val restAssuredScala  = "io.rest-assured"        %  "scala-support"               % restAssuredVer
val scalaMock         = "org.scalamock"          %% "scalamock"                   % scalaMockVer
val scalaTest         = "org.scalatest"          %% "scalatest"                   % scalaTestVer
val slick             = "com.typesafe.slick"     %% "slick"                       % slickVer
val slickHikari       = "com.typesafe.slick"     %% "slick-hikaricp"              % slickVer
val sprayJson         = "io.spray"               %% "spray-json"                  % sprayJsonVer

//
// Shared settings
//
val commonSettings = Seq(
  organization := "hyperion",
  version := "2.1.3-SNAPSHOT",
  description := "Hyperion",
  scalaVersion := "2.13.5",
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
  resolvers += Resolver.sonatypeRepo("snapshots"),
  scmInfo := Some(
    ScmInfo(
      url("https://github.com/mthmulders/hyperion"),
      "scm:git:git@github.com:mthmulders/hyperion.git",
      Some("scm:git:git@github.com:mthmulders/hyperion.git")
    )
  ),
  ThisBuild / scapegoatVersion := "1.4.8",
)
val sonarSettings = Seq(
  sonarProperties ++= Map(
    "sonar.host.url" -> "https://sonarcloud.io",
    "sonar.modules" -> "app,test-app",
    "sonar.projectKey" -> "mthmulders_hyperion",
    "sonar.organization" -> "mthmulders-github",

    "sonar.sourceEncoding" -> "UTF-8",
    "sonar.scala.version" -> "2.13.5",

    "app.sonar.scala.coverage.reportPaths" -> "target/scala-2.13/scoverage-report/scoverage.xml",
    "app.sonar.scala.scapegoat.reportPaths" -> "target/scala-2.13/scapegoat-report/scapegoat-scalastyle.xml",
    "app.sonar.sources" -> "src/main/scala",

    "test-app.sonar.scala.coverage.reportPaths" -> "target/scala-2.13/scoverage-report/scoverage.xml",
    "test-app.sonar.scala.scapegoat.reportPaths" -> "target/scala-2.13/scapegoat-report/scapegoat-scalastyle.xml",
    "test-app.sonar.sources" -> "src/main/scala",
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
      akkaStreamTestkit,
      akkaSlf4j,
      akkaTestKit % "test",
      logback,
      parserComb,
      postgresql,
      scalaMock % "test",
      scalaTest % "test",
      slick,
      slickHikari,
      sprayJson
    ),
    buildInfoOptions += BuildInfoOption.BuildTime,
    buildInfoPackage := "hyperion",
    Linux / packageName := "hyperion",
    Linux / maintainer := "Maarten Mulders",
    Linux / packageSummary := "Hyperion",
    Linux / packageDescription := "The Hyperion system that shows realtime data from a Smart Meter",
    Universal / mappings += {
      sourceDirectory.value / "main" / "deb" / "environment.conf" -> "conf/hyperion.conf"
    },
    Linux / daemonUser := "hyperion",
    Linux / daemonGroup := "hyperion",
    Debian / debianPackageDependencies ++= Seq("java8-runtime-headless"),
    bashScriptExtraDefines += """addJava "-Dconfig.file=${app_home}/../conf/hyperion.conf"""",
    Debian / maintainerScripts := maintainerScriptsAppend((Debian / maintainerScripts).value)(
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

val integrationTest = (project in file("integration-test"))
  .settings(commonSettings: _*)
  .settings(Defaults.itSettings)
  .settings(Seq(
    name := "hyperion-integration-tests",
    libraryDependencies ++= Seq(
      restAssured % "test",
      restAssuredScala % "test",
      scalaTest % "test",
    )
  )
).dependsOn(app % "test->test")

val root = (project in file("."))
  .settings(commonSettings: _*)
  .settings(Seq(
    name := "hyperion-parent"
  ))
  .settings(sonarSettings)
  .settings(sonarScan / aggregate := false)
  .aggregate(app, integrationTest, testApp)

import uk.gov.hmrc.DefaultBuildSettings
import uk.gov.hmrc.DefaultBuildSettings.{defaultSettings, scalaSettings}

val appName = "business-customer"

ThisBuild / majorVersion := 2
ThisBuild / scalaVersion := "2.13.16"

lazy val appDependencies : Seq[ModuleID] = AppDependencies()
lazy val plugins : Seq[Plugins] = Seq(play.sbt.PlayScala, SbtDistributablesPlugin)

lazy val scoverageSettings: Seq[Def.Setting[?]] = {
  import scoverage.ScoverageKeys
  Seq(
    ScoverageKeys.coverageExcludedPackages := "<empty>;Reverse.*;app.Routes.*;prod.*;uk.gov.hmrc.*;testOnlyDoNotUseInAppConf.*;forms.*;config.*;",
    ScoverageKeys.coverageMinimumStmtTotal := 80,
    ScoverageKeys.coverageFailOnMinimum := true,
    ScoverageKeys.coverageHighlighting := true
  )
}

lazy val microservice = Project(appName, file("."))
  .enablePlugins(plugins *)
  .settings(
    scoverageSettings,
    scalaSettings,
    defaultSettings(),
    libraryDependencies ++= appDependencies,
    retrieveManaged := true,
    routesGenerator := InjectedRoutesGenerator,
    scalacOptions += "-Wconf:src=routes/.*:s"
  )
  .settings(
    resolvers += Resolver.typesafeRepo("releases")
  )
  .disablePlugins(JUnitXmlReportPlugin)

lazy val it = project
  .enablePlugins(PlayScala)
  .dependsOn(microservice % "test->test") // the "test->test" allows reusing test code and test dependencies
  .settings(DefaultBuildSettings.itSettings())
  .settings(libraryDependencies ++= AppDependencies.itDependencies)
import play.routes.compiler.InjectedRoutesGenerator
import play.sbt.routes.RoutesKeys.routesGenerator
import sbt.Keys._
import sbt._
import uk.gov.hmrc.DefaultBuildSettings.{addTestReportOption, defaultSettings, scalaSettings}
import uk.gov.hmrc.sbtdistributables.SbtDistributablesPlugin
import uk.gov.hmrc.sbtdistributables.SbtDistributablesPlugin._
import uk.gov.hmrc.versioning.SbtGitVersioning
import uk.gov.hmrc.versioning.SbtGitVersioning.autoImport.majorVersion
import uk.gov.hmrc.{SbtArtifactory, SbtAutoBuildPlugin}

trait MicroService {
  import TestPhases.oneForkedJvmPerTest

  val appName = "business-customer"

  val appDependencies : Seq[ModuleID]
  lazy val plugins : Seq[Plugins] = Seq(play.sbt.PlayScala, SbtAutoBuildPlugin, SbtGitVersioning, SbtDistributablesPlugin, SbtArtifactory)

  lazy val scoverageSettings: Seq[Def.Setting[_]] = {
    import scoverage.ScoverageKeys
    Seq(
      ScoverageKeys.coverageExcludedPackages := "<empty>;Reverse.*;app.Routes.*;prod.*;uk.gov.hmrc.*;testOnlyDoNotUseInAppConf.*;forms.*;config.*;",
      ScoverageKeys.coverageMinimum := 80,
      ScoverageKeys.coverageFailOnMinimum := true,
      ScoverageKeys.coverageHighlighting := true
    )
  }

  lazy val microservice = Project(appName, file("."))
    .enablePlugins(plugins : _*)
    .configs(IntegrationTest)
    .settings(
      addTestReportOption(IntegrationTest, "int-test-reports"),
      inConfig(IntegrationTest)(Defaults.itSettings),
      scoverageSettings,
      scalaSettings,
      publishingSettings,
      defaultSettings(),
      majorVersion := 2,
      libraryDependencies ++= appDependencies,
      retrieveManaged := true,
      routesGenerator := InjectedRoutesGenerator,
      Keys.fork                  in IntegrationTest :=  false,
      unmanagedSourceDirectories in IntegrationTest :=  (baseDirectory in IntegrationTest)(base => Seq(base / "it")).value,
      testGrouping               in IntegrationTest :=  oneForkedJvmPerTest((definedTests in IntegrationTest).value),
      parallelExecution in IntegrationTest := false
    )
    .settings(
      resolvers += Resolver.bintrayRepo("hmrc", "releases"),
      resolvers += Resolver.typesafeRepo("releases"),
      resolvers += Resolver.jcenterRepo
    )
}
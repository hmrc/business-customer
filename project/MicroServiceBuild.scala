import play.core.PlayVersion
import sbt._

object MicroServiceBuild extends Build with MicroService {

  override val appName = "business-customer"

  override lazy val appDependencies: Seq[ModuleID] = AppSpecificDependencies.all
  
  trait TestDependencies {
    lazy val scope: String = "it,test"
    val test : Seq[ModuleID]
  }

  object AppSpecificDependencies extends TestDependencies {
    import play.sbt.PlayImport.ws

    val compile: Seq[ModuleID] = Seq(
      ws,
      "uk.gov.hmrc" %% "bootstrap-play-26" % "0.39.0",
      "uk.gov.hmrc" %% "domain"            % "5.6.0-play-26"
    )
    val test: Seq[ModuleID] = Seq(
      "org.scalatest"           %% "scalatest"          % "3.0.5"             % scope,
      "org.scalatestplus.play"  %% "scalatestplus-play" % "3.1.2"             % scope,
      "org.pegdown"              % "pegdown"            % "1.6.0"             % scope,
      "com.typesafe.play"       %% "play-test"          % PlayVersion.current % scope,
      "org.mockito"              % "mockito-all"        % "1.10.19"           % scope,
      "com.github.tomakehurst"   % "wiremock-jre8"      % "2.23.2"            % IntegrationTest withSources()
    )
    val all = compile ++ test
  }

}



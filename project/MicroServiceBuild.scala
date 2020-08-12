import play.core.PlayVersion
import sbt._

trait TestDependencies {
  lazy val scope: String = "it,test"
  val test : Seq[ModuleID]
}

object AppSpecificDependencies extends TestDependencies {
  import play.sbt.PlayImport.ws

  val compile: Seq[ModuleID] = Seq(
    ws,
    "uk.gov.hmrc" %% "bootstrap-backend-play-27" % "2.24.0",
    "uk.gov.hmrc" %% "domain"                    % "5.9.0-play-27"
  )
  val test: Seq[ModuleID] = Seq(
    "org.scalatest"           %% "scalatest"          % "3.0.8"             % scope,
    "org.scalatestplus.play"  %% "scalatestplus-play" % "4.0.3"             % scope,
    "org.pegdown"              % "pegdown"            % "1.6.0"             % scope,
    "com.typesafe.play"       %% "play-test"          % PlayVersion.current % scope,
    "org.mockito"              % "mockito-core"       % "3.3.3"             % scope,
    "com.github.tomakehurst"   % "wiremock-jre8"      % "2.26.3"            % IntegrationTest withSources()
  )

  def apply() = compile ++ test
}



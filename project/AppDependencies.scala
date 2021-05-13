import play.core.PlayVersion
import sbt._

trait TestDependencies {
  lazy val scope: String = "it,test"
  val test : Seq[ModuleID]
}

object AppDependencies extends TestDependencies {
  import play.sbt.PlayImport.ws

  val compile: Seq[ModuleID] = Seq(
    ws,
    "uk.gov.hmrc" %% "bootstrap-backend-play-27" % "5.2.0",
    "uk.gov.hmrc" %% "domain"                    % "5.11.0-play-27",
    compilerPlugin("com.github.ghik" % "silencer-plugin" % "1.7.1" cross CrossVersion.full),
    "com.github.ghik" % "silencer-lib" % "1.7.1" % Provided cross CrossVersion.full
  )
  
  val test: Seq[ModuleID] = Seq(
    "org.scalatestplus.play"  %% "scalatestplus-play" % "4.0.3"             % scope,
    "org.pegdown"              % "pegdown"            % "1.6.0"             % scope,
    "com.typesafe.play"       %% "play-test"          % PlayVersion.current % scope,
    "org.mockito"              % "mockito-core"       % "3.10.0"            % scope,
    "com.github.tomakehurst"   % "wiremock-jre8"      % "2.26.3"            % IntegrationTest withSources()
  )

  def apply() = compile ++ test
}



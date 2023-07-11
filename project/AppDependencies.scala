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
    "uk.gov.hmrc" %% "bootstrap-backend-play-28" % "7.19.0",
    "uk.gov.hmrc" %% "domain"                    % "8.3.0-play-28"
  )
  
  val test: Seq[ModuleID] = Seq(
    "uk.gov.hmrc"                   %% "bootstrap-test-play-28"     % "7.19.0"            % scope,
    "com.typesafe.play"             %% "play-test"                  % PlayVersion.current % scope,
    "org.mockito"                   %  "mockito-core"               % "5.4.0"             % scope,
    "org.scalatestplus"             %% "scalatestplus-mockito"      % "1.0.0-M2"          % scope,
    "com.fasterxml.jackson.module"  %% "jackson-module-scala"       % "2.15.2"            % scope,
    "com.github.tomakehurst"        %  "wiremock-jre8"              % "2.35.0"            % IntegrationTest withSources()
  )

  def apply() = compile ++ test
}



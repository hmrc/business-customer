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
    "uk.gov.hmrc" %% "bootstrap-backend-play-28" % "5.18.0",
    "uk.gov.hmrc" %% "domain"                    % "6.2.0-play-28",
    compilerPlugin("com.github.ghik" % "silencer-plugin" % "1.7.7" cross CrossVersion.full),
    "com.github.ghik" % "silencer-lib" % "1.7.7" % Provided cross CrossVersion.full
  )
  
  val test: Seq[ModuleID] = Seq(
    "uk.gov.hmrc"                   %% "bootstrap-test-play-28"     % "5.18.0"            % scope,
    "org.pegdown"                   %  "pegdown"                    % "1.6.0"             % scope,
    "com.typesafe.play"             %% "play-test"                  % PlayVersion.current % scope,
    "org.mockito"                   %  "mockito-core"               % "4.2.0"             % scope,
    "org.scalatestplus"             %% "scalatestplus-mockito"      % "1.0.0-M2"          % scope,
    "com.fasterxml.jackson.module"  %% "jackson-module-scala"       % "2.13.1"            % scope,
    "com.github.tomakehurst"        %  "wiremock-jre8"              % "2.32.0"            % IntegrationTest withSources()
  )

  def apply() = compile ++ test
}



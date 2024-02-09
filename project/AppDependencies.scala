import sbt._

trait TestDependencies {
  lazy val scope: String = "it,test"
  val test : Seq[ModuleID]
}

object AppDependencies extends TestDependencies {
  import play.sbt.PlayImport.ws

  private val bootstrapPlayVersion = "8.4.0"

  val compile: Seq[ModuleID] = Seq(
    ws,
    "uk.gov.hmrc" %% "bootstrap-backend-play-30" % bootstrapPlayVersion,
    "uk.gov.hmrc" %% "domain-play-30"            % "9.0.0"
  )
  
  val test: Seq[ModuleID] = Seq(
    "uk.gov.hmrc"                  %% "bootstrap-test-play-30" % bootstrapPlayVersion % "test",
    "org.mockito"                  %  "mockito-core"           % "5.10.0"             % "test",
    "org.scalatestplus"            %% "scalatestplus-mockito"  % "1.0.0-M2"           % "test",
    "com.fasterxml.jackson.module" %% "jackson-module-scala"   % "2.16.1"             % "test"
  )

  val itDependencies: Seq[ModuleID] = Seq(
    "org.wiremock" % "wiremock" % "3.3.1" % Test
  )

  def apply(): Seq[sbt.ModuleID] = compile ++ test
}



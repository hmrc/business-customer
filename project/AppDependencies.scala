import sbt._

trait TestDependencies {
  lazy val scope: String = "it,test"
  val test : Seq[ModuleID]
}

object AppDependencies extends TestDependencies {
  import play.sbt.PlayImport.ws

  private val bootstrapPlayVersion = "9.1.0"

  val compile: Seq[ModuleID] = Seq(
    ws,
    "uk.gov.hmrc" %% "bootstrap-backend-play-30" % bootstrapPlayVersion,
    "uk.gov.hmrc" %% "domain-play-30"            % "10.0.0"
  )
  
  val test: Seq[ModuleID] = Seq(
    "uk.gov.hmrc"                  %% "bootstrap-test-play-30" % bootstrapPlayVersion % "test"
  )

  val itDependencies: Seq[ModuleID] = Seq()

  def apply(): Seq[sbt.ModuleID] = compile ++ test
}



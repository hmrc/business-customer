import sbt._

object MicroServiceBuild extends Build with MicroService {

  override val appName = "business-customer"

  override lazy val appDependencies: Seq[ModuleID] = appSpecificDependencies.all

  private val playMicroserviceBootstrap = "10.4.0"
  private val hmrcTestVersion = "3.6.0-play-25"
  private val domainVersion = "5.3.0"
  private val scalaTestPlusVersion = "2.0.1"
  
  object appSpecificDependencies{
    import play.sbt.PlayImport.ws

    val compile = Seq(
      ws,
      "uk.gov.hmrc" %% "microservice-bootstrap" % playMicroserviceBootstrap,
      "uk.gov.hmrc" %% "domain" % domainVersion    )
    val test = Seq(
      "uk.gov.hmrc" %% "hmrctest" % hmrcTestVersion % "test",
      "org.mockito" % "mockito-all" % "1.10.19" % "test",
      "org.scalatestplus.play" %% "scalatestplus-play" % scalaTestPlusVersion % "test"
    )
    val all = compile ++ test
  }

}



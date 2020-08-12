
package helpers.application

import connectors.{DefaultEtmpConnector, DefaultGovernmentGatewayAdminConnector, DefaultTaxEnrolmentsConnector, EtmpConnector, GovernmentGatewayAdminConnector, TaxEnrolmentsConnector}
import helpers.wiremock.WireMockConfig
import metrics.{DefaultServiceMetrics, ServiceMetrics}
import org.scalatest.TestSuite
import org.scalatestplus.play.guice.GuiceOneServerPerSuite
import play.api.Application
import play.api.inject.guice.GuiceApplicationBuilder
import play.api.libs.ws.{WSClient, WSRequest}
import uk.gov.hmrc.http.HttpClient
import uk.gov.hmrc.play.bootstrap.http.DefaultHttpClient
import play.api.inject.{bind => playBind}

trait IntegrationApplication extends GuiceOneServerPerSuite with WireMockConfig {
  self: TestSuite =>

  val currentAppBaseUrl: String = "business-customer"
  val testAppUrl: String = s"http://localhost:$port"

  lazy val ws: WSClient = app.injector.instanceOf[WSClient]

  override lazy val app: Application = new GuiceApplicationBuilder()
    .overrides(playBind[EtmpConnector].to[DefaultEtmpConnector])
    .overrides(playBind[GovernmentGatewayAdminConnector].to[DefaultGovernmentGatewayAdminConnector])
    .overrides(playBind[TaxEnrolmentsConnector].to[DefaultTaxEnrolmentsConnector])
    .overrides(playBind[ServiceMetrics].to[DefaultServiceMetrics])
    .overrides(playBind[HttpClient].to[DefaultHttpClient])
    .configure(
      Map(
      "application.router" -> "testOnlyDoNotUseInAppConf.Routes",
      "microservice.metrics.graphite.host" -> "localhost",
      "microservice.metrics.graphite.port" -> 2003,
      "microservice.metrics.graphite.prefix" -> "play.business-customer.",
      "microservice.metrics.graphite.enabled" -> true,
      "microservice.services.etmp-hod.host" -> wireMockHost,
      "microservice.services.etmp-hod.port" -> wireMockPort,
      "metrics.name" -> "business-customer",
      "metrics.rateUnit" -> "SECONDS",
      "metrics.durationUnit" -> "SECONDS",
      "metrics.showSamples" -> true,
      "metrics.jvm" -> true,
      "metrics.enabled" -> true
      )
  ).build()

  def makeRequest(uri: String): WSRequest = ws.url(s"http://localhost:$port/$uri")
}

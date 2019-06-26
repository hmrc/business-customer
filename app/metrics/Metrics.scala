/*
 * Copyright 2019 HM Revenue & Customs
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package metrics

import com.codahale.metrics.MetricRegistry
import com.codahale.metrics.Timer.Context
import com.kenshoo.play.metrics.Metrics
import javax.inject.Inject
import metrics.MetricsEnum.MetricsEnum

class DefaultServiceMetrics @Inject()(val metrics: Metrics) extends ServiceMetrics
trait ServiceMetrics {
  val metrics: Metrics
  val registry: MetricRegistry = metrics.defaultRegistry
  val timers = Map(
    MetricsEnum.GG_ADMIN_ADD_KNOWN_FACTS -> registry.timer("gga-add-known-facts-agent-response-timer"),
    MetricsEnum.EMAC_ADMIN_ADD_KNOWN_FACTS -> registry.timer("emac-add-known-facts-agent-response-timer"),
    MetricsEnum.ETMP_REGISTER_BUSINESS_PARTNER -> registry.timer("etmp-create-business-partner-response-timer"),
    MetricsEnum.ETMP_UPDATE_REGISTRATION_DETAILS -> registry.timer("etmp-update-registration-details-response-timer")
  )

  val successCounters = Map(
    MetricsEnum.GG_ADMIN_ADD_KNOWN_FACTS -> registry.counter("gga-add-known-facts-agent-success-counter"),
    MetricsEnum.EMAC_ADMIN_ADD_KNOWN_FACTS -> registry.counter("emac-add-known-facts-agent-success-counter"),
    MetricsEnum.ETMP_REGISTER_BUSINESS_PARTNER -> registry.counter("etmp-create-business-partner-success-counter"),
    MetricsEnum.ETMP_UPDATE_REGISTRATION_DETAILS -> registry.counter("etmp-update-registration-details-success-counter")
  )

  val failedCounters = Map(
    MetricsEnum.GG_ADMIN_ADD_KNOWN_FACTS -> registry.counter("gga-add-known-facts-agent-failed-counter"),
    MetricsEnum.EMAC_ADMIN_ADD_KNOWN_FACTS -> registry.counter("emac-add-known-facts-agent-failed-counter"),
    MetricsEnum.ETMP_REGISTER_BUSINESS_PARTNER -> registry.counter("etmp-create-business-partner-failed-counter"),
    MetricsEnum.ETMP_UPDATE_REGISTRATION_DETAILS -> registry.counter("etmp-update-registration-details-failed-counter")
  )

  def startTimer(api: MetricsEnum): Context = timers(api).time()
  def incrementSuccessCounter(api: MetricsEnum): Unit = successCounters(api).inc()
  def incrementFailedCounter(api: MetricsEnum): Unit = failedCounters(api).inc()
}

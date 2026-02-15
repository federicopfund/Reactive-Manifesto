package services

import akka.actor.typed.ActorSystem
import akka.actor.typed.scaladsl.AskPattern._
import akka.util.Timeout
import core._

import javax.inject._
import scala.concurrent.{ExecutionContext, Future}
import scala.concurrent.duration._

/**
 * Adaptador reactivo para analytics y métricas.
 *
 * Encapsula la comunicación con el AnalyticsEngine (Akka Typed Actor).
 * Usa Tell (fire-and-forget) para tracking y Ask para consultar métricas.
 */
@Singleton
class ReactiveAnalyticsAdapter @Inject()(
  system: ActorSystem[AnalyticsCommand]
)(implicit ec: ExecutionContext) {

  implicit val timeout: Timeout = 3.seconds
  implicit val scheduler: akka.actor.typed.Scheduler = system.scheduler

  /** Fire-and-forget: track a generic event */
  def trackEvent(eventType: String, userId: Option[Long] = None, metadata: Map[String, String] = Map.empty): Unit = {
    system ! TrackEvent(eventType, userId, metadata)
  }

  /** Fire-and-forget: track a page view */
  def trackPageView(path: String, userId: Option[Long] = None, referrer: Option[String] = None): Unit = {
    system ! TrackPageView(path, userId, referrer)
  }

  /** Fire-and-forget: track a publication view */
  def trackPublicationView(publicationId: Long, userId: Option[Long] = None): Unit = {
    system ! TrackPublicationView(publicationId, userId)
  }

  /** Request-response: get current metrics snapshot */
  def getMetrics: Future[AnalyticsResponse] = {
    system.ask[AnalyticsResponse](replyTo => GetMetrics(replyTo))
  }

  /** Fire-and-forget: reset all metrics */
  def resetMetrics(): Unit = {
    system ! ResetMetrics
  }
}

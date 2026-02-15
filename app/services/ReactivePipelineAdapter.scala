package services

import akka.actor.typed.ActorSystem
import akka.actor.typed.scaladsl.AskPattern._
import akka.util.Timeout
import core._
import scala.concurrent.{ExecutionContext, Future}
import scala.concurrent.duration._

/**
 * ReactivePipelineAdapter — Puente entre Controllers y el Saga Orchestrator.
 *
 * Expone el pipeline de publicaciones como un servicio con Future[T],
 * encapsulando el flujo completo: moderation → creation → side effects.
 *
 * Patrones:
 *   - Ask: enviar contenido al pipeline y esperar resultado de la saga
 *   - Ask: consultar métricas del pipeline
 */
class ReactivePipelineAdapter(system: ActorSystem[PipelineCommand])(implicit ec: ExecutionContext) {

  private implicit val timeout: Timeout = Timeout(30.seconds) // Saga may take longer
  private implicit val scheduler: akka.actor.typed.Scheduler = system.scheduler

  /**
   * Process a new publication through the full pipeline:
   * Moderation → Creation → Notification → Gamification → Analytics
   */
  def processPublication(
    userId: Long,
    username: String,
    userEmail: Option[String],
    title: String,
    content: String,
    excerpt: Option[String],
    coverImage: Option[String],
    category: String,
    tags: Option[String]
  ): Future[PipelineResponse] =
    system.ask[PipelineResponse](ref => ProcessNewPublication(
      userId, username, userEmail, title, content, excerpt, coverImage, category, tags, ref
    ))

  /** Get pipeline throughput and latency metrics */
  def getMetrics(): Future[PipelineMetricsSnapshot] =
    system.ask[PipelineResponse](ref => GetPipelineMetrics(ref)).map {
      case m: PipelineMetricsSnapshot => m
      case other => throw new IllegalStateException(s"Unexpected pipeline response: $other")
    }
}

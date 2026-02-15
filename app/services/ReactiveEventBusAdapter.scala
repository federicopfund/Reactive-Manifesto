package services

import akka.actor.typed.ActorSystem
import akka.actor.typed.scaladsl.AskPattern._
import akka.util.Timeout
import core._
import scala.concurrent.{ExecutionContext, Future}
import scala.concurrent.duration._

/**
 * ReactiveEventBusAdapter — Puente entre Controllers y el EventBus.
 *
 * Permite publicar eventos de dominio y consultar métricas del bus
 * desde la capa HTTP sin acoplarse al actor system directamente.
 *
 * Patrones:
 *   - Tell: publicar eventos (fire-and-forget)
 *   - Ask: consultar métricas del bus
 */
class ReactiveEventBusAdapter(system: ActorSystem[EventBusCommand])(implicit ec: ExecutionContext) {

  private implicit val timeout: Timeout = Timeout(3.seconds)
  private implicit val scheduler: akka.actor.typed.Scheduler = system.scheduler

  /** Publish a domain event to all subscribers (fire-and-forget) */
  def publish(event: DomainEvent): Unit =
    system ! PublishEvent(event)

  /** Get event bus metrics */
  def getMetrics(): Future[EventBusMetrics] =
    system.ask[EventBusResponse](ref => GetEventBusMetrics(ref)).map {
      case m: EventBusMetrics => m
    }
}

package core

import akka.actor.typed.{ActorRef, Behavior}
import akka.actor.typed.scaladsl.Behaviors
import java.time.Instant

/**
 * EventBusEngine — Bus de eventos de dominio (Pub/Sub reactivo).
 *
 * Implementa el patrón Publish-Subscribe para comunicación inter-agente
 * completamente desacoplada. Los agentes publican DomainEvents y el bus
 * los enruta a suscriptores filtrados por topic.
 *
 * Características avanzadas:
 *   - Topic-based filtering: suscripción por tipo ("publication", "content.*") o wildcard "*"
 *   - DeathWatch: auto-cleanup cuando un suscriptor termina (tolerancia a fallos)
 *   - Dead Letter tracking: eventos sin suscriptores se contabilizan para monitoreo
 *   - Audit log: historial circular de los últimos N eventos publicados
 *   - Métricas: contadores de eventos publicados, dead letters, suscriptores
 *
 * Principios Reactivos:
 *   - Message-Driven: pub/sub puro basado en mensajes tipados
 *   - Resilient: tolerante a fallos de suscriptores vía DeathWatch
 *   - Elastic: O(1) publish, O(n) broadcast
 *   - Responsive: fire-and-forget publish, zero-latency impact
 *
 *   ┌───────────────────────────────────────────────────┐
 *   │               EventBusEngine                      │
 *   │                                                   │
 *   │   PublishEvent ──▶ Route by topic ──▶ Subscribers │
 *   │                                                   │
 *   │   Subscribe ──▶ Register + DeathWatch             │
 *   │   Unsubscribe ──▶ Remove                          │
 *   │                                                   │
 *   │   Metrics: published / dead letters / subscribers  │
 *   └───────────────────────────────────────────────────┘
 */

// ── Commands ──
sealed trait EventBusCommand

case class PublishEvent(event: DomainEvent) extends EventBusCommand

case class SubscribeToEvents(
  subscriber: ActorRef[DomainEvent],
  topics: Set[String] // "publication", "content", "badge", "*" for all
) extends EventBusCommand

case class UnsubscribeFromEvents(
  subscriber: ActorRef[DomainEvent]
) extends EventBusCommand

case class GetEventBusMetrics(
  replyTo: ActorRef[EventBusResponse]
) extends EventBusCommand

private case class SubscriberTerminated(
  subscriber: ActorRef[DomainEvent]
) extends EventBusCommand

// ── Responses ──
sealed trait EventBusResponse

case class EventBusMetrics(
  totalEventsPublished: Long,
  subscriberCount: Int,
  deadLetterCount: Long,
  recentEvents: List[String],
  since: Instant
) extends EventBusResponse


object EventBusEngine {

  private case class State(
    subscribers: Map[ActorRef[DomainEvent], Set[String]] = Map.empty,
    totalPublished: Long = 0,
    deadLetters: Long = 0,
    recentEvents: List[String] = Nil,
    since: Instant = Instant.now()
  )

  def apply(): Behavior[EventBusCommand] =
    Behaviors.setup { context =>
      context.log.info("[EventBus] Domain Event Bus started — Pub/Sub ready")
      active(State())
    }

  private def active(state: State): Behavior[EventBusCommand] = {
    Behaviors.receive { (context, message) =>
      message match {

        // ═══════════════════════════════════════
        // PUBLISH: Route event to matching subscribers
        // ═══════════════════════════════════════
        case PublishEvent(event) =>
          val topic = event.eventType.split("\\.").headOption.getOrElse("")
          val matched = state.subscribers.filter { case (_, topics) =>
            topics.contains("*") || topics.contains(event.eventType) || topics.contains(topic)
          }

          if (matched.isEmpty) {
            context.log.debug(s"[EventBus] Dead letter: ${event.eventType} [${event.correlationId}] (no subscribers)")
            active(state.copy(
              totalPublished = state.totalPublished + 1,
              deadLetters = state.deadLetters + 1,
              recentEvents = (s"⚠ ${event.eventType}" :: state.recentEvents).take(50)
            ))
          } else {
            context.log.info(s"[EventBus] Publishing ${event.eventType} [${event.correlationId}] → ${matched.size} subscribers")
            matched.keys.foreach { ref =>
              ref ! event
            }
            active(state.copy(
              totalPublished = state.totalPublished + 1,
              recentEvents = (event.eventType :: state.recentEvents).take(50)
            ))
          }

        // ═══════════════════════════════════════
        // SUBSCRIBE: Register subscriber + DeathWatch
        // ═══════════════════════════════════════
        case SubscribeToEvents(subscriber, topics) =>
          context.log.info(s"[EventBus] +subscriber ${subscriber.path.name} → topics: ${topics.mkString(", ")}")
          context.watchWith(subscriber, SubscriberTerminated(subscriber))
          active(state.copy(
            subscribers = state.subscribers + (subscriber -> topics)
          ))

        // ═══════════════════════════════════════
        // UNSUBSCRIBE: Explicit removal
        // ═══════════════════════════════════════
        case UnsubscribeFromEvents(subscriber) =>
          context.log.info(s"[EventBus] -subscriber ${subscriber.path.name}")
          active(state.copy(subscribers = state.subscribers - subscriber))

        // ═══════════════════════════════════════
        // DEATH WATCH: Auto-cleanup on subscriber crash
        // ═══════════════════════════════════════
        case SubscriberTerminated(subscriber) =>
          context.log.warn(s"[EventBus] Subscriber terminated: ${subscriber.path.name} — auto-removed (Resilient)")
          active(state.copy(subscribers = state.subscribers - subscriber))

        // ═══════════════════════════════════════
        // METRICS: Observable bus state
        // ═══════════════════════════════════════
        case GetEventBusMetrics(replyTo) =>
          replyTo ! EventBusMetrics(
            totalEventsPublished = state.totalPublished,
            subscriberCount = state.subscribers.size,
            deadLetterCount = state.deadLetters,
            recentEvents = state.recentEvents,
            since = state.since
          )
          Behaviors.same
      }
    }
  }
}

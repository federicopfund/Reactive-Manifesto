package core

import java.time.Instant

/**
 * Domain Events — Vocabulario compartido entre agentes reactivos.
 *
 * Los eventos de dominio representan hechos inmutables que ya ocurrieron.
 * Cada evento lleva suficiente contexto para que cualquier suscriptor
 * reaccione sin necesidad de consultar otros servicios.
 *
 * Características:
 *   - correlationId: trazabilidad end-to-end a través de todos los agentes
 *   - timestamp: orden causal de los eventos
 *   - eventType: topic para filtrado en el EventBus (Pub/Sub)
 *
 * Patrón: Event-Driven Architecture
 * Principio Reactivo: Message-Driven con desacoplamiento total
 */
sealed trait DomainEvent {
  def eventType: String
  def timestamp: Instant
  def correlationId: String
}

// ── Publication Events ──

case class PublicationSubmittedEvent(
  publicationId: Long,
  userId: Long,
  username: String,
  title: String,
  content: String,
  category: String,
  correlationId: String = java.util.UUID.randomUUID().toString.take(8),
  timestamp: Instant = Instant.now()
) extends DomainEvent {
  val eventType = "publication.submitted"
}

case class PublicationApprovedEvent(
  publicationId: Long,
  userId: Long,
  adminUsername: String,
  correlationId: String = java.util.UUID.randomUUID().toString.take(8),
  timestamp: Instant = Instant.now()
) extends DomainEvent {
  val eventType = "publication.approved"
}

case class PublicationRejectedEvent(
  publicationId: Long,
  userId: Long,
  adminUsername: String,
  reason: String,
  correlationId: String = java.util.UUID.randomUUID().toString.take(8),
  timestamp: Instant = Instant.now()
) extends DomainEvent {
  val eventType = "publication.rejected"
}

// ── Moderation Events ──

case class ContentModeratedEvent(
  contentId: Long,
  contentType: String,
  verdict: String,
  score: Double,
  flags: List[String],
  correlationId: String = java.util.UUID.randomUUID().toString.take(8),
  timestamp: Instant = Instant.now()
) extends DomainEvent {
  val eventType = "content.moderated"
}

// ── Gamification Events ──

case class BadgeEarnedEvent(
  userId: Long,
  badgeKey: String,
  triggerType: String,
  correlationId: String = java.util.UUID.randomUUID().toString.take(8),
  timestamp: Instant = Instant.now()
) extends DomainEvent {
  val eventType = "badge.earned"
}

// ── User Events ──

case class UserActionEvent(
  userId: Long,
  action: String,
  metadata: Map[String, String],
  correlationId: String = java.util.UUID.randomUUID().toString.take(8),
  timestamp: Instant = Instant.now()
) extends DomainEvent {
  val eventType = "user.action"
}

// ── Notification Events ──

case class NotificationDeliveredEvent(
  userId: Long,
  channel: String,
  notificationType: String,
  correlationId: String = java.util.UUID.randomUUID().toString.take(8),
  timestamp: Instant = Instant.now()
) extends DomainEvent {
  val eventType = "notification.delivered"
}

// ── System Events ──

case class CircuitBreakerStateChangedEvent(
  service: String,
  oldState: String,
  newState: String,
  correlationId: String = java.util.UUID.randomUUID().toString.take(8),
  timestamp: Instant = Instant.now()
) extends DomainEvent {
  val eventType = "system.circuit_breaker"
}

case class PipelineCompletedEvent(
  publicationId: Long,
  userId: Long,
  verdict: String,
  processingTimeMs: Long,
  correlationId: String = java.util.UUID.randomUUID().toString.take(8),
  timestamp: Instant = Instant.now()
) extends DomainEvent {
  val eventType = "pipeline.completed"
}

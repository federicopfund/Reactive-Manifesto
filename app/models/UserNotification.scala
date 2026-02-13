package models

import java.time.Instant

/**
 * Notificación para el usuario.
 *
 * Tipos:
 *  - feedback_sent    : El admin envió un feedback sobre una publicación
 *  - publication_approved : Publicación aprobada
 *  - publication_rejected : Publicación rechazada
 */
object NotificationType extends Enumeration {
  type NotificationType = Value
  val FeedbackSent          = Value("feedback_sent")
  val PublicationApproved   = Value("publication_approved")
  val PublicationRejected   = Value("publication_rejected")

  def icon(nt: String): String = nt match {
    case "feedback_sent"          => "💬"
    case "publication_approved"   => "✅"
    case "publication_rejected"   => "❌"
    case _                        => "🔔"
  }
}

case class UserNotification(
  id: Option[Long] = None,
  userId: Long,
  notificationType: String,
  title: String,
  message: String,
  publicationId: Option[Long] = None,
  feedbackId: Option[Long] = None,
  isRead: Boolean = false,
  createdAt: Instant = Instant.now()
)

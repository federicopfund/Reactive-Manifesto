package models

import java.time.Instant

/**
 * Estados de una publicación:
 * - draft: Borrador, no visible
 * - pending: Enviado para revisión
 * - approved: Aprobado por admin, visible públicamente
 * - rejected: Rechazado por admin
 */
object PublicationStatus extends Enumeration {
  type PublicationStatus = Value
  val Draft = Value("draft")
  val Pending = Value("pending")
  val Approved = Value("approved")
  val Rejected = Value("rejected")
}

case class Publication(
  id: Option[Long] = None,
  userId: Long,
  title: String,
  slug: String,
  content: String,
  excerpt: Option[String] = None,
  coverImage: Option[String] = None,
  category: String,
  tags: Option[String] = None, // Almacenado como CSV: "scala,akka,reactive"
  status: String = PublicationStatus.Draft.toString,
  viewCount: Int = 0,
  createdAt: Instant = Instant.now(),
  updatedAt: Instant = Instant.now(),
  publishedAt: Option[Instant] = None,
  reviewedBy: Option[Long] = None,
  reviewedAt: Option[Instant] = None,
  rejectionReason: Option[String] = None,
  adminNotes: Option[String] = None
)

case class PublicationWithAuthor(
  publication: Publication,
  authorUsername: String,
  authorFullName: String,
  reviewerUsername: Option[String] = None
)

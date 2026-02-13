package repositories

import javax.inject.{Inject, Singleton}
import models.{PublicationFeedback, PublicationFeedbackWithAdmin}
import java.time.Instant
import scala.concurrent.{ExecutionContext, Future}
import play.api.db.slick.DatabaseConfigProvider
import slick.jdbc.JdbcProfile

@Singleton
class PublicationFeedbackRepository @Inject()(
  dbConfigProvider: DatabaseConfigProvider
)(implicit ec: ExecutionContext) {

  private val dbConfig = dbConfigProvider.get[JdbcProfile]
  import dbConfig._
  import profile.api._

  private class FeedbackTable(tag: Tag) extends Table[PublicationFeedback](tag, "publication_feedback") {
    def id            = column[Long]("id", O.PrimaryKey, O.AutoInc)
    def publicationId = column[Long]("publication_id")
    def adminId       = column[Long]("admin_id")
    def feedbackType  = column[String]("feedback_type")
    def message       = column[String]("message")
    def sentToUser    = column[Boolean]("sent_to_user")
    def createdAt     = column[Instant]("created_at")

    def * = (
      id.?, publicationId, adminId, feedbackType, message, sentToUser, createdAt
    ).mapTo[PublicationFeedback]
  }

  private val feedbacks = TableQuery[FeedbackTable]

  /** Crear un nuevo feedback */
  def create(feedback: PublicationFeedback): Future[Long] = {
    val insertQuery = feedbacks returning feedbacks.map(_.id)
    db.run(insertQuery += feedback)
  }

  /** Obtener todos los feedbacks de una publicación (para admin) */
  def findByPublicationId(publicationId: Long): Future[List[PublicationFeedbackWithAdmin]] = {
    import slick.jdbc.GetResult
    implicit val gr: GetResult[PublicationFeedbackWithAdmin] = GetResult { r =>
      PublicationFeedbackWithAdmin(
        feedback = PublicationFeedback(
          id = Some(r.nextLong()),
          publicationId = r.nextLong(),
          adminId = r.nextLong(),
          feedbackType = r.nextString(),
          message = r.nextString(),
          sentToUser = r.nextBoolean(),
          createdAt = r.nextTimestamp().toInstant
        ),
        adminUsername = r.nextString()
      )
    }

    val query = sql"""
      SELECT f.id, f.publication_id, f.admin_id, f.feedback_type, f.message,
             f.sent_to_user, f.created_at, u.username
      FROM publication_feedback f
      JOIN users u ON f.admin_id = u.id
      WHERE f.publication_id = $publicationId
      ORDER BY f.created_at DESC
    """.as[PublicationFeedbackWithAdmin]

    db.run(query).map(_.toList)
  }

  /** Obtener solo feedbacks enviados al usuario (sent_to_user = true) */
  def findVisibleByPublicationId(publicationId: Long): Future[List[PublicationFeedbackWithAdmin]] = {
    import slick.jdbc.GetResult
    implicit val gr: GetResult[PublicationFeedbackWithAdmin] = GetResult { r =>
      PublicationFeedbackWithAdmin(
        feedback = PublicationFeedback(
          id = Some(r.nextLong()),
          publicationId = r.nextLong(),
          adminId = r.nextLong(),
          feedbackType = r.nextString(),
          message = r.nextString(),
          sentToUser = r.nextBoolean(),
          createdAt = r.nextTimestamp().toInstant
        ),
        adminUsername = r.nextString()
      )
    }

    val query = sql"""
      SELECT f.id, f.publication_id, f.admin_id, f.feedback_type, f.message,
             f.sent_to_user, f.created_at, u.username
      FROM publication_feedback f
      JOIN users u ON f.admin_id = u.id
      WHERE f.publication_id = $publicationId AND f.sent_to_user = true
      ORDER BY f.created_at DESC
    """.as[PublicationFeedbackWithAdmin]

    db.run(query).map(_.toList)
  }

  /** Marcar un feedback como enviado al usuario */
  def markAsSent(feedbackId: Long): Future[Boolean] = {
    val query = feedbacks
      .filter(_.id === feedbackId)
      .map(_.sentToUser)
      .update(true)
    db.run(query).map(_ > 0)
  }

  /** Contar feedbacks visibles para una publicación */
  def countVisibleByPublicationId(publicationId: Long): Future[Int] = {
    val query = feedbacks
      .filter(f => f.publicationId === publicationId && f.sentToUser === true)
      .length
    db.run(query.result)
  }

  /** Contar feedbacks visibles para múltiples publicaciones de un usuario */
  def countVisibleByPublicationIds(pubIds: Seq[Long]): Future[Map[Long, Int]] = {
    if (pubIds.isEmpty) return Future.successful(Map.empty)

    val query = feedbacks
      .filter(f => f.publicationId.inSet(pubIds) && f.sentToUser === true)
      .groupBy(_.publicationId)
      .map { case (pubId, group) => (pubId, group.length) }

    db.run(query.result).map(_.toMap)
  }

  /** Eliminar un feedback */
  def delete(feedbackId: Long): Future[Boolean] = {
    db.run(feedbacks.filter(_.id === feedbackId).delete).map(_ > 0)
  }
}

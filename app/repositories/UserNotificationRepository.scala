package repositories

import javax.inject.{Inject, Singleton}
import models.UserNotification
import java.time.Instant
import scala.concurrent.{ExecutionContext, Future}
import play.api.db.slick.DatabaseConfigProvider
import slick.jdbc.JdbcProfile

@Singleton
class UserNotificationRepository @Inject()(
  dbConfigProvider: DatabaseConfigProvider
)(implicit ec: ExecutionContext) {

  private val dbConfig = dbConfigProvider.get[JdbcProfile]
  import dbConfig._
  import profile.api._

  private class NotificationsTable(tag: Tag) extends Table[UserNotification](tag, "user_notifications") {
    def id               = column[Long]("id", O.PrimaryKey, O.AutoInc)
    def userId           = column[Long]("user_id")
    def notificationType = column[String]("notification_type")
    def title            = column[String]("title")
    def message          = column[String]("message")
    def publicationId    = column[Option[Long]]("publication_id")
    def feedbackId       = column[Option[Long]]("feedback_id")
    def isRead           = column[Boolean]("is_read")
    def createdAt        = column[Instant]("created_at")

    def * = (
      id.?, userId, notificationType, title, message,
      publicationId, feedbackId, isRead, createdAt
    ).mapTo[UserNotification]
  }

  private val notifications = TableQuery[NotificationsTable]

  /** Crear notificación */
  def create(notification: UserNotification): Future[Long] = {
    val q = notifications returning notifications.map(_.id)
    db.run(q += notification)
  }

  /** Obtener notificaciones de un usuario (más recientes primero) */
  def findByUserId(userId: Long, limit: Int = 30): Future[List[UserNotification]] = {
    val q = notifications
      .filter(_.userId === userId)
      .sortBy(_.createdAt.desc)
      .take(limit)
    db.run(q.result).map(_.toList)
  }

  /** Contar no leídas */
  def countUnread(userId: Long): Future[Int] = {
    val q = notifications
      .filter(n => n.userId === userId && n.isRead === false)
      .length
    db.run(q.result)
  }

  /** Marcar una como leída */
  def markAsRead(notificationId: Long, userId: Long): Future[Boolean] = {
    val q = notifications
      .filter(n => n.id === notificationId && n.userId === userId)
      .map(_.isRead)
      .update(true)
    db.run(q).map(_ > 0)
  }

  /** Marcar todas como leídas */
  def markAllAsRead(userId: Long): Future[Int] = {
    val q = notifications
      .filter(n => n.userId === userId && n.isRead === false)
      .map(_.isRead)
      .update(true)
    db.run(q)
  }
}

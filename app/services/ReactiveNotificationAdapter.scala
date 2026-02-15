package services

import akka.actor.typed.ActorSystem
import akka.actor.typed.scaladsl.AskPattern._
import akka.util.Timeout
import core._

import javax.inject._
import scala.concurrent.{ExecutionContext, Future}
import scala.concurrent.duration._

/**
 * Adaptador reactivo para el hub de notificaciones.
 *
 * Encapsula la comunicación con el NotificationEngine (Akka Typed Actor).
 * Soporta notificaciones multi-canal (in-app + email) y bulk.
 */
@Singleton
class ReactiveNotificationAdapter @Inject()(
  system: ActorSystem[NotificationCommand]
)(implicit ec: ExecutionContext) {

  implicit val timeout: Timeout = 5.seconds
  implicit val scheduler: akka.actor.typed.Scheduler = system.scheduler

  /** Fire-and-forget notification */
  def notify(
    userId: Long,
    userEmail: Option[String],
    notificationType: String,
    title: String,
    message: String,
    publicationId: Option[Long] = None,
    channels: Set[String] = Set("inapp")
  ): Unit = {
    system ! SendNotification(userId, userEmail, notificationType, title, message, publicationId, channels, None)
  }

  /** Request-response notification */
  def notifyAsync(
    userId: Long,
    userEmail: Option[String],
    notificationType: String,
    title: String,
    message: String,
    publicationId: Option[Long] = None,
    channels: Set[String] = Set("inapp")
  ): Future[NotificationResponse] = {
    system.ask[NotificationResponse] { replyTo =>
      SendNotification(userId, userEmail, notificationType, title, message, publicationId, channels, Some(replyTo))
    }
  }

  /** Bulk in-app notification */
  def notifyBulk(
    userIds: Seq[Long],
    notificationType: String,
    title: String,
    message: String,
    publicationId: Option[Long] = None
  ): Unit = {
    system ! SendBulkNotification(userIds, notificationType, title, message, publicationId)
  }
}

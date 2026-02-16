package services

import akka.actor.typed.ActorSystem
import akka.actor.typed.scaladsl.AskPattern._
import akka.util.Timeout
import core._

import javax.inject._
import scala.concurrent.{ExecutionContext, Future}
import scala.concurrent.duration._

/**
 * Adaptador reactivo para el sistema de publicaciones.
 *
 * Encapsula la comunicación con el PublicationEngine (Akka Typed Actor)
 * mediante el patrón Ask.
 */
@Singleton
class ReactivePublicationAdapter @Inject()(
  system: ActorSystem[PublicationCommand]
)(implicit ec: ExecutionContext) {

  implicit val timeout: Timeout = 10.seconds
  implicit val scheduler: akka.actor.typed.Scheduler = system.scheduler

  def createPublication(
    userId: Long,
    username: String,
    title: String,
    content: String,
    excerpt: Option[String],
    coverImage: Option[String],
    category: String,
    tags: Option[String]
  ): Future[PublicationResponse] = {
    system.ask[PublicationResponse] { replyTo =>
      CreatePublication(userId, username, title, content, excerpt, coverImage, category, tags, replyTo)
    }
  }

  def approvePublication(publicationId: Long, adminId: Long, adminUsername: String): Future[PublicationResponse] = {
    system.ask[PublicationResponse] { replyTo =>
      ApprovePublication(publicationId, adminId, adminUsername, replyTo)
    }
  }

  def rejectPublication(publicationId: Long, adminId: Long, adminUsername: String, reason: String): Future[PublicationResponse] = {
    system.ask[PublicationResponse] { replyTo =>
      RejectPublication(publicationId, adminId, adminUsername, reason, replyTo)
    }
  }
}

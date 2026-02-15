package services

import akka.actor.typed.ActorSystem
import akka.actor.typed.scaladsl.AskPattern._
import akka.util.Timeout
import core._

import javax.inject._
import scala.concurrent.{ExecutionContext, Future}
import scala.concurrent.duration._

/**
 *ccc
 *
 * Encapsula la comunicación con el ModerationEngine (Akka Typed Actor).
 * Siempre usa Ask pattern ya que el caller necesita el veredicto.
 */
@Singleton
class ReactiveModerationAdapter @Inject()(
  system: ActorSystem[ModerationCommand]
)(implicit ec: ExecutionContext) {

  implicit val timeout: Timeout = 5.seconds
  implicit val scheduler: akka.actor.typed.Scheduler = system.scheduler

  def moderate(
    contentId: Long,
    contentType: String,
    authorId: Long,
    title: Option[String],
    content: String
  ): Future[ModerationResponse] = {
    system.ask[ModerationResponse] { replyTo =>
      ModerateContent(contentId, contentType, authorId, title, content, replyTo)
    }
  }

  def updateBlocklist(words: Set[String]): Unit = {
    system ! UpdateBlocklist(words)
  }
}

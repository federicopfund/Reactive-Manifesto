package services

import akka.actor.typed.ActorSystem
import akka.actor.typed.scaladsl.AskPattern._
import akka.util.Timeout
import core._

import javax.inject._
import scala.concurrent.{ExecutionContext, Future}
import scala.concurrent.duration._

/**
 * Adaptador reactivo para el sistema de mensajería privada.
 *
 * Encapsula la comunicación con el MessageEngine (Akka Typed Actor)
 * mediante el patrón Ask, proporcionando una interfaz Future-based
 * para los controllers.
 *
 * Principio Reactive: Message-Driven — toda comunicación es asíncrona
 * y basada en mensajes tipados.
 */
@Singleton
class ReactiveMessageAdapter @Inject()(
  system: ActorSystem[MessageCommand]
)(implicit ec: ExecutionContext) {

  implicit val timeout: Timeout = 5.seconds
  implicit val scheduler: akka.actor.typed.Scheduler = system.scheduler

  def sendMessage(
    senderId: Long,
    senderUsername: String,
    receiverId: Long,
    publicationId: Option[Long],
    publicationTitle: Option[String],
    subject: String,
    content: String
  ): Future[MessageResponse] = {
    system.ask[MessageResponse] { replyTo =>
      SendPrivateMessage(senderId, senderUsername, receiverId, publicationId, publicationTitle, subject, content, replyTo)
    }
  }
}

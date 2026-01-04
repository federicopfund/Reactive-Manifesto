package services

import akka.actor.typed.ActorSystem
import akka.actor.typed.Scheduler
import akka.actor.typed.scaladsl.AskPattern._
import akka.util.Timeout
import core._

import scala.concurrent.{ExecutionContext, Future}
import scala.concurrent.duration._

final class ReactiveContactAdapter(system: ActorSystem[ContactCommand]) {

  private implicit val timeout: Timeout = 3.seconds
  private implicit val scheduler: Scheduler = system.scheduler


  def submit(name: String, email: String, message: String)(implicit ec: ExecutionContext): Future[Either[String, Unit]] =
    system.ask[ContactResponse](replyTo =>
        SubmitContact(name, email, message, replyTo)
      )
      .map {
        case ContactAccepted         => Right(())
        case ContactRejected(reason) => Left(reason)
      }
}

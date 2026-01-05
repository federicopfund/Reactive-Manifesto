package services

import akka.actor.typed.ActorSystem
import akka.actor.typed.scaladsl.AskPattern._
import akka.util.Timeout
import core._

import javax.inject._
import scala.concurrent.{ExecutionContext, Future}
import scala.concurrent.duration._

@Singleton
class ReactiveContactAdapter @Inject()(system: ActorSystem[ContactCommand])(implicit ec: ExecutionContext) {
  
  implicit val timeout: Timeout = 5.seconds
  implicit val scheduler: akka.actor.typed.Scheduler = system.scheduler

  def submitContact(contact: Contact): Future[ContactResponse] = {
    system.ask[ContactResponse](replyTo => SubmitContact(contact, replyTo))
  }
}

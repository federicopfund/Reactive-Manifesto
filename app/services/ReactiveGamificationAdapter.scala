package services

import akka.actor.typed.ActorSystem
import akka.actor.typed.scaladsl.AskPattern._
import akka.util.Timeout
import core._

import javax.inject._
import scala.concurrent.{ExecutionContext, Future}
import scala.concurrent.duration._

/**
 * Adaptador reactivo para el sistema de gamificación.
 *
 * Encapsula la comunicación con el GamificationEngine (Akka Typed Actor).
 * Soporta tanto Ask (request-response) como Tell (fire-and-forget).
 */
@Singleton
class ReactiveGamificationAdapter @Inject()(
  system: ActorSystem[GamificationCommand]
)(implicit ec: ExecutionContext) {

  implicit val timeout: Timeout = 5.seconds
  implicit val scheduler: akka.actor.typed.Scheduler = system.scheduler

  /** Fire-and-forget: no espera respuesta */
  def checkBadges(userId: Long, triggerType: String, metadata: Map[String, Long]): Unit = {
    system ! CheckBadges(userId, triggerType, metadata, None)
  }

  /** Request-response: espera la lista de badges otorgados */
  def checkBadgesAsync(userId: Long, triggerType: String, metadata: Map[String, Long]): Future[GamificationResponse] = {
    system.ask[GamificationResponse] { replyTo =>
      CheckBadges(userId, triggerType, metadata, Some(replyTo))
    }
  }

  /** Fire-and-forget award */
  def awardBadge(userId: Long, badgeKey: String): Unit = {
    system ! AwardBadge(userId, badgeKey, None)
  }
}

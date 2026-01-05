package services

import akka.actor.typed.ActorSystem
import akka.actor.typed.Scheduler
import akka.actor.typed.scaladsl.AskPattern._
import akka.util.Timeout
import core._

import scala.concurrent.{ExecutionContext, Future}
import scala.concurrent.duration._
import scala.util.{Failure, Success}

/**
 * ReactiveContactAdapter implements the adapter pattern to bridge
 * the synchronous HTTP world with the asynchronous actor system.
 * 
 * Reactive Manifesto principles applied:
 * - RESPONSIVE: Fast timeout handling
 * - RESILIENT: Error recovery and fallback strategies
 * - MESSAGE-DRIVEN: Asynchronous communication with actors
 * - ELASTIC: Non-blocking, can handle variable load
 */
final class ReactiveContactAdapter(system: ActorSystem[ContactCommand]) {

  private implicit val timeout: Timeout = 5.seconds
  private implicit val scheduler: Scheduler = system.scheduler

  /**
   * Submit contact request with backpressure handling
   * Returns Either with error message or success
   */
  def submit(
    name: String, 
    email: String, 
    message: String
  )(implicit ec: ExecutionContext): Future[Either[String, Unit]] = {
    
    // Sanitize inputs before sending to actor system (security)
    val sanitizedName = sanitize(name)
    val sanitizedEmail = sanitize(email)
    val sanitizedMessage = sanitize(message)
    
    system.ask[ContactResponse](replyTo =>
        SubmitContact(sanitizedName, sanitizedEmail, sanitizedMessage, replyTo)
      )
      .map {
        case ContactAccepted => 
          Right(())
        case ContactRejected(reason) => 
          Left(reason)
        case _ => 
          Left("Respuesta inesperada del sistema")
      }
      .recover {
        case _: akka.pattern.AskTimeoutException =>
          Left("El sistema está experimentando alta carga. Por favor, intenta nuevamente.")
        case ex: Exception =>
          system.log.error("Error processing contact", ex)
          Left("Error interno del sistema. Por favor, contacta al administrador.")
      }
  }

  /**
   * Get system statistics for monitoring (Elastic principle)
   */
  def getStats()(implicit ec: ExecutionContext): Future[Option[ContactStatsResponse]] = {
    system.ask[ContactResponse](replyTo => 
        GetContactStats(replyTo)
      )
      .map {
        case stats: ContactStatsResponse => Some(stats)
        case _ => None
      }
      .recover {
        case _ => None
      }
  }

  /**
   * Basic input sanitization to prevent XSS and injection attacks
   * Uses case-insensitive matching and comprehensive escaping
   */
  private def sanitize(input: String): String = {
    input
      .trim
      .replaceAll("(?i)<script[^>]*>", "")  // Remove script tags (case-insensitive)
      .replaceAll("(?i)</script>", "")
      .replaceAll("(?i)<iframe[^>]*>", "")  // Remove iframe tags
      .replaceAll("(?i)</iframe>", "")
      .replaceAll("(?i)javascript:", "")     // Remove javascript: protocol
      .replaceAll("(?i)on\\w+\\s*=", "")     // Remove event handlers
      .replaceAll("<", "&lt;")               // Escape HTML
      .replaceAll(">", "&gt;")
      .replaceAll("\"", "&quot;")
      .replaceAll("'", "&#x27;")
      .take(10000)                           // Hard limit to prevent memory issues
  }

  /**
   * Health check for the contact system
   * Returns true if system is responsive
   */
  def healthCheck()(implicit ec: ExecutionContext): Future[Boolean] = {
    getStats().map(_.isDefined)
      .recover { case _ => false }
  }
}

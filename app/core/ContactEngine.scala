package core

import akka.actor.typed._
import akka.actor.typed.scaladsl._
import java.time.Instant

/**
 * ContactEngine implements the Reactive Manifesto principles:
 * 
 * 1. RESPONSIVE: Fast, consistent response times
 * 2. RESILIENT: Fault-tolerant through supervision
 * 3. ELASTIC: Handles varying load through message-driven architecture
 * 4. MESSAGE-DRIVEN: Asynchronous, non-blocking message passing
 */
object ContactEngine {

  private final case class State(
    received: Long = 0,
    accepted: Long = 0,
    rejected: Long = 0
  )

  def apply(): Behavior[ContactCommand] = stateful(State())

  private def stateful(state: State): Behavior[ContactCommand] =
    Behaviors.receive { (context, command) =>
      command match {

        case SubmitContact(name, email, message, replyTo) =>
          context.log.info(s"Processing contact from: $name <$email>")
          
          // Validation with detailed error messages (Responsive principle)
          validateContact(name, email, message) match {
            case Left(error) =>
              context.log.warn(s"Contact rejected: $error")
              replyTo ! ContactRejected(error)
              stateful(state.copy(
                received = state.received + 1,
                rejected = state.rejected + 1
              ))

            case Right(_) =>
              context.log.info(s"Contact accepted from: $name")
              
              // In production, this would emit event for event sourcing
              // Example: context.system.eventStream.publish(ContactSubmitted(...))
              
              replyTo ! ContactAccepted
              stateful(state.copy(
                received = state.received + 1,
                accepted = state.accepted + 1
              ))
          }

        case GetContactStats(replyTo) =>
          // Elastic principle: Expose metrics for monitoring and scaling decisions
          replyTo ! ContactStatsResponse(
            totalReceived = state.received,
            totalAccepted = state.accepted,
            totalRejected = state.rejected
          )
          Behaviors.same
      }
    }
    .receiveSignal {
      case (context, PostStop) =>
        context.log.info("ContactEngine stopped gracefully")
        Behaviors.same
    }

  /**
   * Validates contact data following domain rules
   * Returns Either with error message or success
   */
  private def validateContact(
    name: String, 
    email: String, 
    message: String
  ): Either[String, Unit] = {
    
    if (name.trim.isEmpty)
      Left("El nombre no puede estar vacío")
    else if (name.trim.length < 2)
      Left("El nombre debe tener al menos 2 caracteres")
    else if (name.trim.length > 100)
      Left("El nombre no puede exceder 100 caracteres")
    else if (message.trim.isEmpty)
      Left("El mensaje no puede estar vacío")
    else if (message.trim.length < 10)
      Left("El mensaje debe tener al menos 10 caracteres")
    else if (message.trim.length > 5000)
      Left("El mensaje no puede exceder 5000 caracteres")
    else if (!isValidEmail(email))
      Left("Formato de email inválido")
    else
      Right(())
  }

  /**
   * Email validation with comprehensive checks
   */
  private def isValidEmail(email: String): Boolean = {
    val emailRegex = """^[a-zA-Z0-9._%+-]+@[a-zA-Z0-9.-]+\.[a-zA-Z]{2,}$""".r
    email.trim match {
      case emailRegex(_*) => true
      case _ => false
    }
  }

  /**
   * Supervision strategy for resilience (Resilient principle)
   * Handles failures gracefully without losing state
   */
  def supervised(): Behavior[ContactCommand] =
    Behaviors.supervise(apply())
      .onFailure[Exception](
        SupervisorStrategy.restart
          .withLimit(maxNrOfRetries = 3, withinTimeRange = scala.concurrent.duration.Duration(1, "minute"))
      )
}

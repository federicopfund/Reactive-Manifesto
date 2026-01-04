package core

import akka.actor.typed._
import akka.actor.typed.scaladsl._

object ContactEngine {

  def apply(): Behavior[ContactCommand] =
    Behaviors.receive { (_, command) =>
      command match {

        case SubmitContact(name, email, message, replyTo) =>
          if (message.trim.isEmpty)
            replyTo ! ContactRejected("El mensaje no puede estar vacío.")
          else if (!email.contains("@"))
            replyTo ! ContactRejected("Email inválido.")
          else {
            // Aquí iría persistencia, email, cola, etc.
            replyTo ! ContactAccepted
          }

          Behaviors.same
      }
    }
}

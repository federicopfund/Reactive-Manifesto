package core

import akka.actor.typed.{ActorRef, Behavior}
import akka.actor.typed.scaladsl.Behaviors

// Contact domain model
case class Contact(name: String, email: String, message: String)

// Commands for the reactive actor
sealed trait ContactCommand
case class SubmitContact(contact: Contact, replyTo: ActorRef[ContactResponse]) extends ContactCommand

// Responses
sealed trait ContactResponse
case class ContactSubmitted(id: String) extends ContactResponse
case class ContactError(message: String) extends ContactResponse

// The reactive contact engine using Akka Typed
object ContactEngine {
  def apply(): Behavior[ContactCommand] = active()

  private def active(): Behavior[ContactCommand] = {
    Behaviors.receive { (context, message) =>
      message match {
        case SubmitContact(contact, replyTo) =>
          // Simulate async processing - in real app would save to database
          context.log.info(s"Processing contact from ${contact.name} (${contact.email})")
          
          // Generate a simple ID
          val id = java.util.UUID.randomUUID().toString
          
          // Reply with success
          replyTo ! ContactSubmitted(id)
          
          Behaviors.same
      }
    }
  }
}

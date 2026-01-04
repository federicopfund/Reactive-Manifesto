package core

import akka.actor.typed.ActorRef

sealed trait ContactCommand

final case class SubmitContact(
  name: String,
  email: String,
  message: String,
  replyTo: ActorRef[ContactResponse]
) extends ContactCommand

sealed trait ContactResponse
final case object ContactAccepted extends ContactResponse
final case class ContactRejected(reason: String) extends ContactResponse

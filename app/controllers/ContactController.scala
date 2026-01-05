package controllers

import play.api.data._
import play.api.data.Forms._
import play.api.libs.json.Json
import play.api.mvc._
import services.ReactiveContactAdapter

import javax.inject._
import scala.concurrent.{ExecutionContext, Future}

/**
 * ContactController handles HTTP requests for the contact form.
 * 
 * Implements Reactive principles:
 * - RESPONSIVE: Non-blocking, async request handling
 * - RESILIENT: Comprehensive error handling
 * - MESSAGE-DRIVEN: Delegates to actor system via adapter
 */
@Singleton
final class ContactController @Inject()(
  cc: ControllerComponents,
  adapter: ReactiveContactAdapter
)(implicit ec: ExecutionContext)
  extends AbstractController(cc) {

  private val contactForm = Form(
    mapping(
      "name" -> nonEmptyText(minLength = 2, maxLength = 100),
      "email" -> email,
      "message" -> nonEmptyText(minLength = 10, maxLength = 5000),
      "subject" -> optional(text(maxLength = 200))
    )(ContactData.apply)(ContactData.unapply)
  )

  /**
   * Display the contact form
   */
  def form: Action[AnyContent] =
    Action { implicit request =>
      Ok(views.html.contactForm(contactForm))
    }

  /**
   * Submit contact form - Async and non-blocking (Responsive principle)
   */
  def submit: Action[AnyContent] =
    Action.async { implicit request =>
      contactForm.bindFromRequest().fold(
        formWithErrors => {
          // Return form with validation errors (Responsive principle)
          Future.successful(
            BadRequest(views.html.contactForm(formWithErrors))
          )
        },
        data => {
          // Delegate to reactive adapter (Message-driven principle)
          adapter.submit(data.name, data.email, data.formatMessage).map {
            case Right(_) =>
              Ok(views.html.contactResult(
                "Mensaje enviado correctamente",
                "Tu mensaje ha sido recibido y será procesado pronto.",
                success = true
              ))
            case Left(error) =>
              BadRequest(
                views.html.contactForm(
                  contactForm.fill(data).withGlobalError(error)
                )
              )
          }.recover {
            // Resilient error handling
            case ex: Exception =>
              InternalServerError(
                views.html.contactResult(
                  "Error del sistema",
                  "Ocurrió un error inesperado. Por favor, intenta más tarde.",
                  success = false
                )
              )
          }
        }
      )
    }

  /**
   * Health check endpoint for monitoring (Elastic principle)
   */
  def health: Action[AnyContent] =
    Action.async { implicit request =>
      adapter.healthCheck().map { isHealthy =>
        if (isHealthy) Ok("OK")
        else ServiceUnavailable("Service unhealthy")
      }
    }

  /**
   * Statistics endpoint for monitoring (Elastic principle)
   */
  def stats: Action[AnyContent] =
    Action.async { implicit request =>
      adapter.getStats().map {
        case Some(stats) =>
          Ok(Json.obj(
            "received" -> stats.totalReceived,
            "accepted" -> stats.totalAccepted,
            "rejected" -> stats.totalRejected
          ))
        case None =>
          ServiceUnavailable("Stats unavailable")
      }
    }
}

/**
 * Contact form data model with enhanced validation
 */
final case class ContactData(
  name: String,
  email: String,
  message: String,
  subject: Option[String]
) {
  /**
   * Format message with optional subject
   */
  def formatMessage: String = {
    subject match {
      case Some(subj) if subj.nonEmpty => 
        s"Asunto: $subj\n\n$message"
      case _ => 
        message
    }
  }
}

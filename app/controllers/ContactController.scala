package controllers

import play.api.data._
import play.api.data.Forms._
import play.api.mvc._
import services.ReactiveContactAdapter

import javax.inject._
import scala.concurrent.{ExecutionContext, Future}

@Singleton
final class ContactController @Inject()(
  cc: ControllerComponents,
  adapter: ReactiveContactAdapter
)(implicit ec: ExecutionContext)
  extends AbstractController(cc) {

  private val contactForm = Form(
    mapping(
      "name"    -> nonEmptyText,
      "email"   -> email,
      "message" -> nonEmptyText
    )(ContactData.apply)(ContactData.unapply)
  )

  def form: Action[AnyContent] =
    Action { implicit request =>
      Ok(views.html.contactForm(contactForm))
    }

  def submit: Action[AnyContent] =
    Action.async { implicit request =>
      contactForm.bindFromRequest().fold(
        errors => Future.successful(BadRequest(views.html.contactForm(errors))),
        data =>
          adapter.submit(data.name, data.email, data.message).map {
            case Right(_) =>
              Ok(views.html.contactResult("Mensaje enviado correctamente."))
            case Left(error) =>
              BadRequest(
                views.html.contactForm(
                  contactForm.withGlobalError(error)
                )
              )
          }
      )
    }
}

final case class ContactData(
  name: String,
  email: String,
  message: String
)

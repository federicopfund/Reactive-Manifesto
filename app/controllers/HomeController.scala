package controllers

import javax.inject._
import play.api._
import play.api.mvc._
import play.api.data._
import play.api.data.Forms._
import services.ReactiveContactAdapter
import core.{Contact, ContactSubmitted, ContactError}
import scala.concurrent.{ExecutionContext, Future}

@Singleton
class HomeController @Inject()(
  val controllerComponents: ControllerComponents,
  adapter: ReactiveContactAdapter
)(implicit ec: ExecutionContext) extends BaseController {

  // Form definition
  case class ContactFormData(name: String, email: String, message: String)

  val contactForm: Form[ContactFormData] = Form(
    mapping(
      "name" -> nonEmptyText,
      "email" -> email,
      "message" -> nonEmptyText(minLength = 10)
    )(ContactFormData.apply)(ContactFormData.unapply)
  )

  def index() = Action { implicit request: Request[AnyContent] =>
    Ok(views.html.index(contactForm))
  }

  def submitContact() = Action.async { implicit request: Request[AnyContent] =>
    contactForm.bindFromRequest().fold(
      formWithErrors => {
        Future.successful(BadRequest(views.html.index(formWithErrors)))
      },
      contactData => {
        val contact = Contact(contactData.name, contactData.email, contactData.message)
        adapter.submitContact(contact).map {
          case ContactSubmitted(id) =>
            Redirect(routes.HomeController.index()).flashing("success" -> s"¡Gracias por tu mensaje! ID: $id")
          case ContactError(msg) =>
            Redirect(routes.HomeController.index()).flashing("error" -> s"Error: $msg")
        }
      }
    )
  }
}

package controllers

import javax.inject._
import play.api._
import play.api.mvc._
import play.api.data._
import play.api.data.Forms._
import play.api.i18n.I18nSupport
import services.ReactiveContactAdapter
import repositories.ContactRepository
import core.{Contact, ContactSubmitted, ContactError}
import actions.{OptionalAuthAction, OptionalAuthRequest}
import scala.concurrent.{ExecutionContext, Future}

// Form data case class (outside controller for Twirl template access)
case class ContactFormData(name: String, email: String, message: String)

@Singleton
class HomeController @Inject()(
  val controllerComponents: ControllerComponents,
  adapter: ReactiveContactAdapter,
  contactRepository: ContactRepository,
  publicationRepository: repositories.PublicationRepository,
  optionalAuth: OptionalAuthAction
)(implicit ec: ExecutionContext) extends BaseController with I18nSupport {

  // Form definition

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

  def publicaciones() = Action.async { implicit request: Request[AnyContent] =>
    // Obtener publicaciones dinámicas aprobadas de usuarios
    publicationRepository.findAllApproved(limit = 20).map { dynamicPublications =>
      Ok(views.html.publicaciones(dynamicPublications))
    }
  }

  def publicacion(slug: String) = Action.async { implicit request: Request[AnyContent] =>
    // Primero buscar en publicaciones dinámicas
    publicationRepository.findBySlug(slug).map {
      case Some(publication) if publication.status == "approved" =>
        // Incrementar contador de vistas
        publicationRepository.incrementViewCount(publication.id.get)
        Ok(views.html.user.publicationPreview(publication, "Invitado"))
      case _ =>
        // Si no se encuentra, buscar en artículos estáticos
        slug match {
          case "akka-actors" => Ok(views.html.articulos.akkaActors())
          case "patrones-resiliencia" => Ok(views.html.articulos.patronesResiliencia())
          case "akka-streams" => Ok(views.html.articulos.akkaStreams())
          case "play-async" => Ok(views.html.articulos.playAsync())
          case "message-passing" => Ok(views.html.articulos.messagePassing())
          case "testing-reactivo" => Ok(views.html.articulos.testingReactivo())
          case _ => NotFound("Publicación no encontrada")
        }
    }
  }

  def portafolio() = optionalAuth { implicit request: OptionalAuthRequest[AnyContent] =>
    // Pasar información de autenticación a la vista
    val isAuthenticated = request.userInfo.isDefined
    val username = request.userInfo.map(_._2)
    Ok(views.html.portafolio(isAuthenticated, username))
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

  // Endpoint opcional para listar contactos (útil para admin)
  def listContacts(page: Int) = Action.async { implicit request: Request[AnyContent] =>
    contactRepository.list(page, pageSize = 20).map { contacts =>
      Ok(play.api.libs.json.Json.toJson(contacts.map { c =>
        play.api.libs.json.Json.obj(
          "id" -> c.id,
          "name" -> c.name,
          "email" -> c.email,
          "message" -> c.message,
          "createdAt" -> c.createdAt.toString,
          "status" -> c.status
        )
      }))
    }
  }

  // Endpoint para obtener estadísticas
  def contactStats() = Action.async { implicit request: Request[AnyContent] =>
    contactRepository.count().map { total =>
      Ok(play.api.libs.json.Json.obj(
        "total" -> total,
        "timestamp" -> java.time.Instant.now().toString
      ))
    }
  }
}

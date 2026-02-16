package controllers

import javax.inject._
import play.api._
import play.api.mvc._
import play.api.data._
import play.api.data.Forms._
import play.api.i18n.I18nSupport
import services.ReactiveContactAdapter
import services.ReactiveAnalyticsAdapter
import repositories.{ContactRepository, ReactionRepository, CommentRepository, BookmarkRepository, NewsletterRepository}
import core.{Contact, ContactSubmitted, ContactError}
import actions.{OptionalAuthAction, OptionalAuthRequest}
import scala.concurrent.{ExecutionContext, Future}

// Form data case class (outside controller for Twirl template access)
case class ContactFormData(name: String, email: String, message: String)

@Singleton
class HomeController @Inject()(
  val controllerComponents: ControllerComponents,
  adapter: ReactiveContactAdapter,
  analyticsAdapter: ReactiveAnalyticsAdapter,
  contactRepository: ContactRepository,
  publicationRepository: repositories.PublicationRepository,
  reactionRepo: ReactionRepository,
  commentRepo: CommentRepository,
  bookmarkRepo: BookmarkRepository,
  newsletterRepo: NewsletterRepository,
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

  def index() = Action.async { implicit request: Request[AnyContent] =>
    analyticsAdapter.trackPageView("/", None, request.headers.get("Referer"))
    publicationRepository.findAllApproved(limit = 6).map { publications =>
      Ok(views.html.index(contactForm, publications))
    }
  }

  def publicaciones() = Action.async { implicit request: Request[AnyContent] =>
    // Obtener publicaciones dinámicas aprobadas de usuarios
    publicationRepository.findAllApproved(limit = 20).map { dynamicPublications =>
      Ok(views.html.publicaciones(dynamicPublications, "", None))
    }
  }

  def publicacion(slug: String) = optionalAuth.async { implicit request: OptionalAuthRequest[AnyContent] =>
    // Primero buscar en publicaciones dinámicas
    publicationRepository.findBySlug(slug).flatMap {
      case Some(publication) if publication.status == "approved" =>
        // Track via AnalyticsEngine (fire-and-forget)
        val userId = request.userInfo.map(_._1)
        publication.id.foreach { pubId =>
          analyticsAdapter.trackPublicationView(pubId, userId)
        }
        analyticsAdapter.trackPageView(s"/publicacion/$slug", userId, request.headers.get("Referer"))
        // Incrementar contador de vistas
        publicationRepository.incrementViewCount(publication.id.get)
        val pubId = publication.id.get
        for {
          reactions <- reactionRepo.countByPublication(pubId)
          userReactions <- userId.map(uid => reactionRepo.getUserReactions(pubId, uid)).getOrElse(Future.successful(Set.empty[String]))
          comments <- commentRepo.findByPublicationId(pubId)
          isBookmarked <- userId.map(uid => bookmarkRepo.isBookmarked(uid, pubId)).getOrElse(Future.successful(false))
        } yield {
          Ok(views.html.user.publicationPreview(
            publication, 
            request.userInfo.map(_._2).getOrElse("Invitado"), 
            List.empty,
            reactions,
            userReactions,
            comments,
            isBookmarked,
            userId
          ))
        }
      case _ =>
        // Si no se encuentra, buscar en artículos estáticos
        Future.successful(slug match {
          case "akka-actors" => Ok(views.html.articulos.akkaActors())
          case "patrones-resiliencia" => Ok(views.html.articulos.patronesResiliencia())
          case "akka-streams" => Ok(views.html.articulos.akkaStreams())
          case "play-async" => Ok(views.html.articulos.playAsync())
          case "message-passing" => Ok(views.html.articulos.messagePassing())
          case "testing-reactivo" => Ok(views.html.articulos.testingReactivo())
          case _ => NotFound("Publicación no encontrada")
        })
    }
  }

  def searchPublicaciones(q: String, category: Option[String]) = Action.async { implicit request: Request[AnyContent] =>
    if (q.trim.isEmpty) {
      Future.successful(Redirect(routes.HomeController.publicaciones()))
    } else {
      publicationRepository.searchApproved(q, category).map { results =>
        Ok(views.html.publicaciones(results, q, category))
      }
    }
  }

  def portafolio() = optionalAuth { implicit request: OptionalAuthRequest[AnyContent] =>
    // Pasar información de autenticación a la vista
    val isAuthenticated = request.userInfo.isDefined
    val username = request.userInfo.map(_._2)
    Ok(views.html.portafolio(isAuthenticated, username))
  }

  def politicaPrivacidad() = Action { implicit request: Request[AnyContent] =>
    Ok(views.html.legal.privacidad())
  }

  def terminosDeUso() = Action { implicit request: Request[AnyContent] =>
    Ok(views.html.legal.terminos())
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
            Redirect(routes.HomeController.index()).flashing("success" -> "¡Mensaje recibido! Gracias por contactarnos, te responderemos lo antes posible.")
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

  // Newsletter subscription
  def subscribeNewsletter() = Action.async { implicit request: Request[AnyContent] =>
    request.body.asFormUrlEncoded.flatMap(_.get("email").flatMap(_.headOption)) match {
      case Some(email) if email.trim.nonEmpty =>
        val ip = request.remoteAddress
        newsletterRepo.subscribe(email, Some(ip)).map {
          case Right(_) =>
            Redirect(routes.HomeController.publicaciones()).flashing(
              "success" -> "¡Suscripción exitosa! Recibirás nuestras novedades."
            )
          case Left(msg) =>
            Redirect(routes.HomeController.publicaciones()).flashing(
              "info" -> msg
            )
        }
      case _ =>
        Future.successful(
          Redirect(routes.HomeController.publicaciones()).flashing(
            "error" -> "Por favor ingresa un email válido."
          )
        )
    }
  }
}

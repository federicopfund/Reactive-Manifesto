package controllers

import javax.inject._
import play.api.mvc._
import play.api.data._
import play.api.data.Forms._
import play.api.libs.json._
import play.api.i18n.I18nSupport
import scala.concurrent.{ExecutionContext, Future}
import repositories.{PublicationRepository, UserRepository, PublicationFeedbackRepository, UserNotificationRepository, ReactionRepository, CommentRepository, BookmarkRepository, BadgeRepository, PrivateMessageRepository}
import models.{Publication, PublicationStatus, NotificationType, PublicationComment, PrivateMessage}
import services.{ReactiveMessageAdapter, ReactiveGamificationAdapter, ReactiveAnalyticsAdapter, ReactiveNotificationAdapter, ReactiveModerationAdapter}
import core.{MessageSent, MessageError, ModerationResult}
import actions.{UserAction, AuthRequest}
import java.time.Instant

case class PublicationFormData(
  title: String,
  content: String,
  excerpt: Option[String],
  coverImage: Option[String],
  category: String,
  tags: Option[String]
)

@Singleton
class UserPublicationController @Inject()(
  cc: ControllerComponents,
  publicationRepo: PublicationRepository,
  userRepo: UserRepository,
  feedbackRepo: PublicationFeedbackRepository,
  notificationRepo: UserNotificationRepository,
  reactionRepo: ReactionRepository,
  commentRepo: CommentRepository,
  bookmarkRepo: BookmarkRepository,
  badgeRepo: BadgeRepository,
  messageRepo: PrivateMessageRepository,
  messageAdapter: ReactiveMessageAdapter,
  gamificationAdapter: ReactiveGamificationAdapter,
  analyticsAdapter: ReactiveAnalyticsAdapter,
  notificationAdapter: ReactiveNotificationAdapter,
  moderationAdapter: ReactiveModerationAdapter,
  userAction: UserAction
)(implicit ec: ExecutionContext) extends AbstractController(cc) with I18nSupport {

  // Definición del formulario
  val publicationForm = Form(
    mapping(
      "title" -> nonEmptyText(minLength = 5, maxLength = 200),
      "content" -> nonEmptyText(minLength = 50),
      "excerpt" -> optional(text(maxLength = 500)),
      "coverImage" -> optional(text),
      "category" -> nonEmptyText,
      "tags" -> optional(text)
    )(PublicationFormData.apply)(PublicationFormData.unapply)
  )

  /**
   * Dashboard del usuario - Ver todas sus publicaciones
   */
  def dashboard = userAction.async { implicit request: AuthRequest[AnyContent] =>
    for {
      publications <- publicationRepo.findByUserId(request.userId)
      stats <- publicationRepo.getUserStats(request.userId)
      pubIds = publications.flatMap(_.id)
      feedbackCounts <- feedbackRepo.countVisibleByPublicationIds(pubIds)
      badges <- badgeRepo.findByUserId(request.userId)
    } yield {
      Ok(views.html.user.dashboard(
        username = request.username,
        publications = publications,
        stats = stats,
        feedbackCounts = feedbackCounts,
        badges = badges
      ))
    }
  }

  /**
   * Formulario para crear nueva publicación
   */
  def newPublicationForm = userAction { implicit request: AuthRequest[AnyContent] =>
    Ok(views.html.user.publicationForm(
      publicationForm,
      None,
      request.username
    ))
  }

  /**
   * Crear nueva publicación
   */
  def createPublication = userAction.async { implicit request: AuthRequest[AnyContent] =>
    publicationForm.bindFromRequest().fold(
      formWithErrors => {
        Future.successful(
          BadRequest(views.html.user.publicationForm(
            formWithErrors,
            None,
            request.username
          ))
        )
      },
      formData => {
        val slug = generateSlug(formData.title)
        val publication = Publication(
          userId = request.userId,
          title = formData.title,
          slug = slug,
          content = formData.content,
          excerpt = formData.excerpt,
          coverImage = formData.coverImage,
          category = formData.category,
          tags = formData.tags,
          status = PublicationStatus.Draft.toString
        )
        
        publicationRepo.create(publication).map { id =>
          // Track via AnalyticsEngine (fire-and-forget)
          analyticsAdapter.trackEvent("publication.created", Some(request.userId), Map(
            "publicationId" -> id.toString,
            "category" -> formData.category,
            "status" -> "draft"
          ))
          Redirect(routes.UserPublicationController.editPublicationForm(id))
            .flashing("success" -> "Publicación creada exitosamente como borrador")
        }
      }
    )
  }

  /**
   * Formulario para editar publicación existente
   */
  def editPublicationForm(id: Long) = userAction.async { implicit request: AuthRequest[AnyContent] =>
    publicationRepo.findById(id).map {
      case Some(publication) if publication.userId == request.userId =>
        val filledForm = publicationForm.fill(PublicationFormData(
          title = publication.title,
          content = publication.content,
          excerpt = publication.excerpt,
          coverImage = publication.coverImage,
          category = publication.category,
          tags = publication.tags
        ))
        Ok(views.html.user.publicationForm(
          filledForm,
          Some(publication),
          request.username
        ))
      case Some(_) =>
        Forbidden("No tienes permiso para editar esta publicación")
      case None =>
        NotFound("Publicación no encontrada")
    }
  }

  /**
   * Actualizar publicación
   */
  def updatePublication(id: Long) = userAction.async { implicit request: AuthRequest[AnyContent] =>
    publicationRepo.findById(id).flatMap {
      case Some(existingPub) if existingPub.userId == request.userId =>
        publicationForm.bindFromRequest().fold(
          formWithErrors => {
            Future.successful(
              BadRequest(views.html.user.publicationForm(
                formWithErrors,
                Some(existingPub),
                request.username
              ))
            )
          },
          formData => {
            val slug = generateSlug(formData.title)
            val updatedPub = existingPub.copy(
              title = formData.title,
              slug = slug,
              content = formData.content,
              excerpt = formData.excerpt,
              coverImage = formData.coverImage,
              category = formData.category,
              tags = formData.tags,
              updatedAt = Instant.now()
            )
            
            publicationRepo.update(updatedPub).map { success =>
              if (success) {
                Redirect(routes.UserPublicationController.dashboard())
                  .flashing("success" -> "Publicación actualizada exitosamente")
              } else {
                InternalServerError("Error al actualizar la publicación")
              }
            }
          }
        )
      case Some(_) =>
        Future.successful(Forbidden("No tienes permiso para editar esta publicación"))
      case None =>
        Future.successful(NotFound("Publicación no encontrada"))
    }
  }

  /**
   * Enviar publicación para revisión
   */
  def submitForReview(id: Long) = userAction.async { implicit request: AuthRequest[AnyContent] =>
    publicationRepo.findById(id).flatMap {
      case Some(publication) if publication.userId == request.userId =>
        // Stage 1: Auto-moderate via ModerationEngine (Ask)
        moderationAdapter.moderate(
          contentId = id,
          contentType = "publication",
          authorId = request.userId,
          title = Some(publication.title),
          content = publication.content
        ).flatMap {
          case ModerationResult(_, verdict, flags, score) =>
            val newStatus = if (verdict == "auto_rejected") "rejected" else PublicationStatus.Pending.toString
            val updated = publication.copy(
              status = newStatus,
              updatedAt = Instant.now()
            )
            publicationRepo.update(updated).map { success =>
              if (success) {
                // Track via AnalyticsEngine
                analyticsAdapter.trackEvent("publication.submitted", Some(request.userId), Map(
                  "publicationId" -> id.toString,
                  "verdict" -> verdict,
                  "score" -> score.toString
                ))
                // Notify via NotificationEngine
                notificationAdapter.notify(
                  userId = request.userId,
                  userEmail = None,
                  notificationType = "publication_submitted",
                  title = if (verdict == "auto_rejected") "Publicación rechazada" else "Publicación enviada",
                  message = if (verdict == "auto_rejected")
                    s"Tu publicación '${publication.title}' no pasó la moderación: ${flags.mkString(", ")}"
                  else
                    s"Tu publicación '${publication.title}' fue enviada para revisión.",
                  publicationId = Some(id)
                )
                // Check badges via GamificationEngine
                gamificationAdapter.checkBadges(request.userId, "publication", Map("publicationCount" -> 1L))

                if (verdict == "auto_rejected") {
                  Redirect(routes.UserPublicationController.dashboard())
                    .flashing("error" -> s"Publicación rechazada automáticamente: ${flags.mkString(", ")}")
                } else {
                  Redirect(routes.UserPublicationController.dashboard())
                    .flashing("success" -> s"Publicación enviada para revisión (moderación: $verdict)")
                }
              } else {
                InternalServerError("Error al enviar la publicación")
              }
            }
        }
      case Some(_) =>
        Future.successful(Forbidden("No tienes permiso"))
      case None =>
        Future.successful(NotFound("Publicación no encontrada"))
    }
  }

  /**
   * Eliminar publicación
   */
  def deletePublication(id: Long) = userAction.async { implicit request: AuthRequest[AnyContent] =>
    publicationRepo.delete(id, request.userId).map { success =>
      if (success) {
        Redirect(routes.UserPublicationController.dashboard())
          .flashing("success" -> "Publicación eliminada")
      } else {
        BadRequest("No se pudo eliminar la publicación")
      }
    }
  }

  /**
   * Ver publicación (preview)
   */
  def viewPublication(id: Long) = userAction.async { implicit request: AuthRequest[AnyContent] =>
    publicationRepo.findById(id).flatMap {
      case Some(publication) if publication.userId == request.userId || publication.status == PublicationStatus.Approved.toString =>
        // Track via AnalyticsEngine (fire-and-forget, non-blocking)
        publication.id.foreach { pubId =>
          analyticsAdapter.trackPublicationView(pubId, Some(request.userId))
        }
        for {
          feedbacks <- feedbackRepo.findVisibleByPublicationId(id)
          reactions <- reactionRepo.countByPublication(id)
          userReactions <- reactionRepo.getUserReactions(id, request.userId)
          comments <- commentRepo.findByPublicationId(id)
          isBookmarked <- bookmarkRepo.isBookmarked(request.userId, id)
        } yield {
          Ok(views.html.user.publicationPreview(
            publication, request.username, feedbacks,
            reactions, userReactions, comments, isBookmarked,
            Some(request.userId)
          ))
        }
      case Some(_) =>
        Future.successful(Forbidden("No tienes permiso para ver esta publicación"))
      case None =>
        Future.successful(NotFound("Publicación no encontrada"))
    }
  }

  /**
   * API: Listar publicaciones del usuario (JSON)
   */
  def listPublicationsJson = userAction.async { implicit request: AuthRequest[AnyContent] =>
    publicationRepo.findByUserId(request.userId).map { publications =>
      Ok(Json.toJson(publications.map { pub =>
        Json.obj(
          "id" -> pub.id,
          "title" -> pub.title,
          "status" -> pub.status,
          "category" -> pub.category,
          "viewCount" -> pub.viewCount,
          "createdAt" -> pub.createdAt.toString,
          "updatedAt" -> pub.updatedAt.toString
        )
      }))
    }
  }

  // ============================================
  // NOTIFICACIONES DEL USUARIO
  // ============================================

  /**
   * Página de notificaciones del usuario
   */
  def notifications = userAction.async { implicit request: AuthRequest[AnyContent] =>
    for {
      notifs <- notificationRepo.findByUserId(request.userId)
      _ <- notificationRepo.markAllAsRead(request.userId)
    } yield {
      Ok(views.html.user.notifications(request.username, notifs))
    }
  }

  /**
   * API: Contar notificaciones no leídas (para el badge del bell)
   */
  def unreadCount = userAction.async { implicit request: AuthRequest[AnyContent] =>
    notificationRepo.countUnread(request.userId).map { count =>
      Ok(Json.obj("count" -> count))
    }
  }

  // ============================================
  // REACTIONS / LIKES
  // ============================================

  /** Toggle a reaction (like/excellent/learned) on a publication */
  def toggleReaction(publicationId: Long) = userAction.async { implicit request: AuthRequest[AnyContent] =>
    val reactionType = request.body.asFormUrlEncoded
      .flatMap(_.get("reactionType").flatMap(_.headOption))
      .getOrElse("like")
    
    for {
      added <- reactionRepo.toggle(publicationId, request.userId, reactionType)
      _ = if (added) {
        // Fire-and-forget via GamificationEngine
        gamificationAdapter.checkBadges(request.userId, "reaction", Map("totalReactions" -> 1L))
        analyticsAdapter.trackEvent("reaction.toggled", Some(request.userId), Map(
          "publicationId" -> publicationId.toString,
          "reactionType" -> reactionType,
          "action" -> "added"
        ))
      }
    } yield {
      val referer = request.headers.get("Referer").getOrElse(routes.HomeController.publicaciones().url)
      Redirect(referer).flashing(
        "success" -> (if (added) "Reacción agregada" else "Reacción eliminada")
      )
    }
  }

  // ============================================
  // COMMENTS
  // ============================================

  /** Add a comment to a publication */
  def addComment(publicationId: Long) = userAction.async { implicit request: AuthRequest[AnyContent] =>
    val content = request.body.asFormUrlEncoded
      .flatMap(_.get("content").flatMap(_.headOption))
      .getOrElse("")
    
    if (content.trim.length < 3) {
      Future.successful(
        Redirect(request.headers.get("Referer").getOrElse(routes.HomeController.publicaciones().url))
          .flashing("error" -> "El comentario debe tener al menos 3 caracteres")
      )
    } else {
      val comment = PublicationComment(
        publicationId = publicationId,
        userId = request.userId,
        content = content.trim
      )
      for {
        _ <- commentRepo.create(comment)
        _ = {
          // Fire-and-forget via GamificationEngine
          gamificationAdapter.checkBadges(request.userId, "comment", Map("commentCount" -> 1L))
          analyticsAdapter.trackEvent("comment.added", Some(request.userId), Map(
            "publicationId" -> publicationId.toString
          ))
        }
      } yield {
        Redirect(request.headers.get("Referer").getOrElse(routes.HomeController.publicaciones().url))
          .flashing("success" -> "Comentario agregado")
      }
    }
  }

  /** Delete own comment */
  def deleteComment(commentId: Long) = userAction.async { implicit request: AuthRequest[AnyContent] =>
    commentRepo.delete(commentId, request.userId).map { _ =>
      Redirect(request.headers.get("Referer").getOrElse(routes.HomeController.publicaciones().url))
        .flashing("success" -> "Comentario eliminado")
    }
  }

  // ============================================
  // BOOKMARKS
  // ============================================

  /** Toggle bookmark for a publication */
  def toggleBookmark(publicationId: Long) = userAction.async { implicit request: AuthRequest[AnyContent] =>
    bookmarkRepo.toggle(request.userId, publicationId).map { added =>
      Redirect(request.headers.get("Referer").getOrElse(routes.HomeController.publicaciones().url))
        .flashing("success" -> (if (added) "Guardado en favoritos" else "Eliminado de favoritos"))
    }
  }

  /** Bookmarks page */
  def bookmarks = userAction.async { implicit request: AuthRequest[AnyContent] =>
    for {
      bms <- bookmarkRepo.findByUserId(request.userId)
      pubIds = bms.map(_.publicationId)
      publications <- publicationRepo.findByIds(pubIds)
    } yield {
      Ok(views.html.user.bookmarks(request.username, publications))
    }
  }

  // ============================================
  // PROFILE
  // ============================================

  /** Public profile page */
  def publicProfile(username: String) = Action.async { implicit request: Request[AnyContent] =>
    userRepo.findByUsername(username).flatMap {
      case Some(user) =>
        for {
          pubs <- publicationRepo.findApprovedByUserId(user.id.get)
          pubIds = pubs.flatMap(_.id)
          badges <- badgeRepo.findByUserId(user.id.get)
          totalReactions <- reactionRepo.totalReactionsForUser(user.id.get, pubIds)
          totalViews = pubs.map(_.viewCount).sum
        } yield {
          Ok(views.html.user.publicProfile(user, pubs, badges, totalReactions, totalViews))
        }
      case None =>
        Future.successful(NotFound("Usuario no encontrado"))
    }
  }

  /** Edit profile form page */
  def editProfileForm = userAction.async { implicit request: AuthRequest[AnyContent] =>
    userRepo.findById(request.userId).map {
      case Some(user) => Ok(views.html.user.editProfile(request.username, user))
      case None       => NotFound("Usuario no encontrado")
    }
  }

  /** Save profile */
  def updateProfile = userAction.async { implicit request: AuthRequest[AnyContent] =>
    val form = request.body.asFormUrlEncoded.getOrElse(Map.empty)
    def f(key: String) = form.get(key).flatMap(_.headOption).getOrElse("")
    userRepo.updateProfile(request.userId, f("bio"), f("avatarUrl"), f("website"), f("location")).map { _ =>
      Redirect(routes.UserPublicationController.publicProfile(request.username))
        .flashing("success" -> "Perfil actualizado")
    }
  }

  // ============================================
  // BADGES (API)
  // ============================================

  /** Get user badges as JSON */
  def userBadges = userAction.async { implicit request: AuthRequest[AnyContent] =>
    badgeRepo.findByUserId(request.userId).map { badges =>
      Ok(Json.toJson(badges.map { b =>
        Json.obj("key" -> b.badgeKey, "awardedAt" -> b.awardedAt.toString)
      }))
    }
  }

  // ============================================
  // MENSAJERÍA PRIVADA (Message-Driven / Reactive)
  // ============================================

  /**
   * Bandeja de mensajes (recibidos y enviados)
   */
  def inbox(tab: String) = userAction.async { implicit request: AuthRequest[AnyContent] =>
    val messagesF = tab match {
      case "sent" => messageRepo.findSentWithUsers(request.userId)
      case _      => messageRepo.findReceivedWithUsers(request.userId)
    }
    for {
      msgs <- messagesF
      unreadCount <- messageRepo.countUnread(request.userId)
    } yield {
      Ok(views.html.user.inbox(request.username, msgs, tab, unreadCount))
    }
  }

  /**
   * Ver un mensaje específico
   */
  def viewMessage(id: Long) = userAction.async { implicit request: AuthRequest[AnyContent] =>
    messageRepo.findByIdWithUsers(id, request.userId).map {
      case Some(msgWithUsers) =>
        // Marcar como leído si es el receptor
        if (msgWithUsers.message.receiverId == request.userId && !msgWithUsers.message.isRead) {
          messageRepo.markAsRead(id, request.userId)
        }
        Ok(views.html.user.viewMessage(request.username, msgWithUsers))
      case None =>
        NotFound("Mensaje no encontrado")
    }
  }

  /**
   * Enviar mensaje privado al autor de una publicación.
   * Usa el sistema reactivo (Akka Typed Actor) para procesar
   * el envío de forma asíncrona y crear la notificación.
   */
  def sendMessage(publicationId: Long) = userAction.async { implicit request: AuthRequest[AnyContent] =>
    val form = request.body.asFormUrlEncoded.getOrElse(Map.empty)
    val subject = form.get("subject").flatMap(_.headOption).getOrElse("").trim
    val content = form.get("content").flatMap(_.headOption).getOrElse("").trim

    if (subject.isEmpty || content.isEmpty) {
      Future.successful(
        Redirect(routes.HomeController.publicacion(""))
          .flashing("error" -> "El asunto y el contenido son obligatorios")
      )
    } else {
      publicationRepo.findById(publicationId).flatMap {
        case Some(pub) if pub.userId == request.userId =>
          Future.successful(
            Redirect(request.headers.get("Referer").getOrElse(routes.HomeController.publicaciones().url))
              .flashing("error" -> "No puedes enviarte un mensaje a ti mismo")
          )
        case Some(pub) =>
          // Enviar a través del sistema reactivo Message-Driven
          messageAdapter.sendMessage(
            senderId = request.userId,
            senderUsername = request.username,
            receiverId = pub.userId,
            publicationId = Some(publicationId),
            publicationTitle = Some(pub.title),
            subject = subject,
            content = content
          ).map {
            case MessageSent(_) =>
              Redirect(request.headers.get("Referer").getOrElse(routes.HomeController.publicaciones().url))
                .flashing("success" -> "Mensaje enviado correctamente")
            case MessageError(reason) =>
              Redirect(request.headers.get("Referer").getOrElse(routes.HomeController.publicaciones().url))
                .flashing("error" -> s"Error al enviar: $reason")
          }
        case None =>
          Future.successful(NotFound("Publicación no encontrada"))
      }
    }
  }

  /**
   * Enviar mensaje directo a un usuario (sin publicación asociada)
   */
  def sendDirectMessage = userAction.async { implicit request: AuthRequest[AnyContent] =>
    val form = request.body.asFormUrlEncoded.getOrElse(Map.empty)
    val receiverUsername = form.get("receiverUsername").flatMap(_.headOption).getOrElse("").trim
    val subject = form.get("subject").flatMap(_.headOption).getOrElse("").trim
    val content = form.get("content").flatMap(_.headOption).getOrElse("").trim

    if (subject.isEmpty || content.isEmpty || receiverUsername.isEmpty) {
      Future.successful(
        Redirect(routes.UserPublicationController.inbox("received"))
          .flashing("error" -> "Todos los campos son obligatorios")
      )
    } else {
      userRepo.findByUsername(receiverUsername).flatMap {
        case Some(receiver) if receiver.id.contains(request.userId) =>
          Future.successful(
            Redirect(routes.UserPublicationController.inbox("received"))
              .flashing("error" -> "No puedes enviarte un mensaje a ti mismo")
          )
        case Some(receiver) =>
          messageAdapter.sendMessage(
            senderId = request.userId,
            senderUsername = request.username,
            receiverId = receiver.id.get,
            publicationId = None,
            publicationTitle = None,
            subject = subject,
            content = content
          ).map {
            case MessageSent(_) =>
              Redirect(routes.UserPublicationController.inbox("sent"))
                .flashing("success" -> s"Mensaje enviado a ${receiver.fullName}")
            case MessageError(reason) =>
              Redirect(routes.UserPublicationController.inbox("received"))
                .flashing("error" -> s"Error al enviar: $reason")
          }
        case None =>
          Future.successful(
            Redirect(routes.UserPublicationController.inbox("received"))
              .flashing("error" -> "Usuario destinatario no encontrado")
          )
      }
    }
  }

  /**
   * API: Contar mensajes no leídos
   */
  def unreadMessagesCount = userAction.async { implicit request: AuthRequest[AnyContent] =>
    messageRepo.countUnread(request.userId).map { count =>
      Ok(Json.obj("count" -> count))
    }
  }

  /**
   * Generar slug a partir del título
   */
  private def generateSlug(title: String): String = {
    val slug = title.toLowerCase
      .replaceAll("[áàäâ]", "a")
      .replaceAll("[éèëê]", "e")
      .replaceAll("[íìïî]", "i")
      .replaceAll("[óòöô]", "o")
      .replaceAll("[úùüû]", "u")
      .replaceAll("[ñ]", "n")
      .replaceAll("[^a-z0-9]+", "-")
      .replaceAll("^-|-$", "")
    
    s"$slug-${System.currentTimeMillis()}"
  }
}

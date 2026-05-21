package controllers.admin

import javax.inject._
import play.api.mvc._
import controllers.actions.{AdminOnlyAction, CapabilityCheck}
import domains.editorial.models.Sponsor
import domains.editorial.repositories.SponsorRepository
import infrastructure.support.Capabilities.Cap

import java.time.Instant
import scala.concurrent.{ExecutionContext, Future}

@Singleton
class AdminSponsorController @Inject()(
  cc: ControllerComponents,
  sponsorRepo: SponsorRepository,
  adminAction: AdminOnlyAction
)(implicit ec: ExecutionContext) extends AbstractController(cc) {

  // ── Listado ──────────────────────────────────────────────────────
  def list() = adminAction.async { implicit request =>
    CapabilityCheck.require(request, Cap.SeasonsManage) {
      sponsorRepo.findAll().map { sponsors =>
        Ok(views.html.admin.sponsors.list(sponsors))
      }
    }
  }

  // ── Crear ────────────────────────────────────────────────────────
  def newForm() = adminAction.async { implicit request =>
    CapabilityCheck.require(request, Cap.SeasonsManage) {
      Future.successful(Ok(views.html.admin.sponsors.form(None, Map.empty, defaultFormData)))
    }
  }

  def create() = adminAction.async { implicit request =>
    CapabilityCheck.require(request, Cap.SeasonsManage) {
      val data = parseForm(request.body.asFormUrlEncoded.getOrElse(Map.empty))
      validate(data) match {
        case Left(errors) =>
          Future.successful(BadRequest(views.html.admin.sponsors.form(None, errors, data)))
        case Right(parsed) =>
          sponsorRepo.slugExists(parsed.slug).flatMap {
            case true =>
              Future.successful(BadRequest(views.html.admin.sponsors.form(
                None, Map("slug" -> "Ya existe un sponsor con ese slug"), data)))
            case false =>
              sponsorRepo.create(parsed).map { id =>
                Redirect(routes.AdminSponsorController.editForm(id))
                  .flashing("success" -> "Sponsor creado.")
              }
          }
      }
    }
  }

  // ── Editar ───────────────────────────────────────────────────────
  def editForm(id: Long) = adminAction.async { implicit request =>
    CapabilityCheck.require(request, Cap.SeasonsManage) {
      sponsorRepo.findByIdAny(id).map {
        case None    => NotFound("Sponsor no encontrado")
        case Some(s) => Ok(views.html.admin.sponsors.form(Some(s), Map.empty, toFormData(s)))
      }
    }
  }

  def update(id: Long) = adminAction.async { implicit request =>
    CapabilityCheck.require(request, Cap.SeasonsManage) {
      sponsorRepo.findByIdAny(id).flatMap {
        case None => Future.successful(NotFound("Sponsor no encontrado"))
        case Some(existing) =>
          val data = parseForm(request.body.asFormUrlEncoded.getOrElse(Map.empty))
          validate(data) match {
            case Left(errors) =>
              Future.successful(BadRequest(views.html.admin.sponsors.form(Some(existing), errors, data)))
            case Right(parsed) =>
              sponsorRepo.slugExists(parsed.slug, excludeId = Some(id)).flatMap {
                case true =>
                  Future.successful(BadRequest(views.html.admin.sponsors.form(
                    Some(existing), Map("slug" -> "Slug duplicado"), data)))
                case false =>
                  sponsorRepo.update(parsed.copy(id = Some(id))).map { _ =>
                    Redirect(routes.AdminSponsorController.editForm(id))
                      .flashing("success" -> "Sponsor actualizado.")
                  }
              }
          }
      }
    }
  }

  // ── Helpers ──────────────────────────────────────────────────────
  private val defaultFormData = Map(
    "slug" -> "", "name" -> "", "website" -> "", "logoUrl" -> "",
    "contactName" -> "", "contactEmail" -> "", "contactRole" -> "",
    "tierDefault" -> "bronze", "notes" -> "", "isActive" -> "true"
  )

  private def parseForm(raw: Map[String, Seq[String]]): Map[String, String] = {
    def s(k: String) = raw.get(k).flatMap(_.headOption).map(_.trim).getOrElse("")
    Seq("slug","name","website","logoUrl","contactName","contactEmail","contactRole","tierDefault","notes","isActive")
      .map(k => k -> s(k)).toMap
  }

  private def validate(data: Map[String, String]): Either[Map[String, String], Sponsor] = {
    val errors = scala.collection.mutable.Map.empty[String, String]
    val slug = data.getOrElse("slug", "").trim
    val name = data.getOrElse("name", "").trim
    if (slug.isEmpty) errors += "slug" -> "Slug requerido."
    else if (!slug.matches("^[a-z0-9-]+$")) errors += "slug" -> "Solo a-z, 0-9 y guiones."
    if (name.isEmpty) errors += "name" -> "Nombre requerido."
    if (errors.nonEmpty) Left(errors.toMap)
    else Right(Sponsor(
      slug         = slug,
      name         = name,
      website      = Some(data.getOrElse("website","")).filter(_.nonEmpty),
      logoUrl      = Some(data.getOrElse("logoUrl","")).filter(_.nonEmpty),
      contactName  = Some(data.getOrElse("contactName","")).filter(_.nonEmpty),
      contactEmail = Some(data.getOrElse("contactEmail","")).filter(_.nonEmpty),
      contactRole  = Some(data.getOrElse("contactRole","")).filter(_.nonEmpty),
      tierDefault  = data.getOrElse("tierDefault","bronze"),
      notes        = Some(data.getOrElse("notes","")).filter(_.nonEmpty),
      isActive     = data.getOrElse("isActive","true") == "true"
    ))
  }

  private def toFormData(s: Sponsor): Map[String, String] = Map(
    "slug"         -> s.slug,
    "name"         -> s.name,
    "website"      -> s.website.getOrElse(""),
    "logoUrl"      -> s.logoUrl.getOrElse(""),
    "contactName"  -> s.contactName.getOrElse(""),
    "contactEmail" -> s.contactEmail.getOrElse(""),
    "contactRole"  -> s.contactRole.getOrElse(""),
    "tierDefault"  -> s.tierDefault,
    "notes"        -> s.notes.getOrElse(""),
    "isActive"     -> s.isActive.toString
  )
}

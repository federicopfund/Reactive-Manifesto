package domains.editorial.repositories

import javax.inject.{Inject, Singleton}
import domains.editorial.models.{Sponsor, SponsorAssignment}
import java.time.Instant
import scala.concurrent.{ExecutionContext, Future}
import play.api.db.slick.DatabaseConfigProvider
import slick.jdbc.JdbcProfile

/**
 * Acceso al catálogo de patrocinadores (Issue #24 — Evolution 31).
 *
 * Solo lectura desde el lado público; escritura desde el backoffice.
 */
@Singleton
class SponsorRepository @Inject()(
  dbConfigProvider: DatabaseConfigProvider
)(implicit ec: ExecutionContext) {

  private val dbConfig = dbConfigProvider.get[JdbcProfile]
  import dbConfig._
  import profile.api._

  private class Sponsors(tag: Tag) extends Table[Sponsor](tag, "sponsors") {
    def id           = column[Long]("id", O.PrimaryKey, O.AutoInc)
    def slug         = column[String]("slug")
    def name         = column[String]("name")
    def website      = column[Option[String]]("website")
    def logoUrl      = column[Option[String]]("logo_url")
    def contactName  = column[Option[String]]("contact_name")
    def contactEmail = column[Option[String]]("contact_email")
    def contactRole  = column[Option[String]]("contact_role")
    def tierDefault  = column[String]("tier_default")
    def notes        = column[Option[String]]("notes")
    def isActive     = column[Boolean]("is_active")
    def createdAt    = column[Instant]("created_at")
    def updatedAt    = column[Instant]("updated_at")

    def * = (
      id.?, slug, name, website, logoUrl, contactName, contactEmail,
      contactRole, tierDefault, notes, isActive, createdAt, updatedAt
    ).mapTo[Sponsor]
  }

  private val sponsors = TableQuery[Sponsors]

  def findById(id: Long): Future[Option[Sponsor]] =
    db.run(sponsors.filter(s => s.id === id && s.isActive).result.headOption)

  def findBySlug(slug: String): Future[Option[Sponsor]] =
    db.run(sponsors.filter(s => s.slug === slug && s.isActive).result.headOption)

  def findAllActive(): Future[Seq[Sponsor]] =
    db.run(sponsors.filter(_.isActive).sortBy(_.name).result)

  def findAll(): Future[Seq[Sponsor]] =
    db.run(sponsors.sortBy(_.name).result)

  def slugExists(slug: String, excludeId: Option[Long] = None): Future[Boolean] = {
    val base = sponsors.filter(_.slug === slug)
    val q = excludeId match {
      case Some(id) => base.filter(_.id =!= id)
      case None     => base
    }
    db.run(q.exists.result)
  }

  def create(s: Sponsor): Future[Long] = {
    val now = Instant.now()
    val row = s.copy(createdAt = now, updatedAt = now)
    db.run((sponsors returning sponsors.map(_.id)) += row)
  }

  def update(s: Sponsor): Future[Int] = s.id match {
    case None => Future.successful(0)
    case Some(id) =>
      val now = Instant.now()
      db.run(
        sponsors.filter(_.id === id)
          .map(r => (r.slug, r.name, r.website, r.logoUrl, r.contactName, r.contactEmail, r.contactRole, r.tierDefault, r.notes, r.isActive, r.updatedAt))
          .update((s.slug, s.name, s.website, s.logoUrl, s.contactName, s.contactEmail, s.contactRole, s.tierDefault, s.notes, s.isActive, now))
      )
  }

  def findByIdAny(id: Long): Future[Option[Sponsor]] =
    db.run(sponsors.filter(_.id === id).result.headOption)

  // ── Asignaciones cruzadas ─────────────────────────────────────────────────
  // Retorna todas las entidades (publicaciones, eventos, colecciones, temporadas)
  // que tienen un sponsor asignado, para la vista /admin/comercial/asignaciones.

  def findAssignments(): Future[Seq[SponsorAssignment]] = {
    import slick.jdbc.GetResult
    implicit val gr: GetResult[SponsorAssignment] = GetResult { r =>
      SponsorAssignment(
        entityType   = r.nextString(),
        entityId     = r.nextLong(),
        entityTitle  = r.nextString(),
        entitySlug   = r.nextString(),
        sponsorId    = r.nextLong(),
        sponsorName  = r.nextString(),
        sponsorTier  = r.nextString(),
        sponsorLabel = r.nextStringOption(),
        showPublic   = r.nextBoolean()
      )
    }
    val q = sql"""
      SELECT 'publicacion' AS entity_type, p.id AS entity_id, p.title AS entity_title, p.slug AS entity_slug,
             s.id AS sponsor_id, s.name AS sponsor_name, s.tier_default AS sponsor_tier, p.sponsor_label, p.sponsor_show_public
      FROM publications p
      JOIN sponsors s ON p.sponsor_id = s.id
      UNION ALL
      SELECT 'evento', e.id, e.title, e.slug,
             s.id, s.name, s.tier_default, e.sponsor_label, e.sponsor_show_public
      FROM community_events e
      JOIN sponsors s ON e.sponsor_id = s.id
      UNION ALL
      SELECT 'coleccion', c.id, c.name, c.slug,
             s.id, s.name, s.tier_default, c.sponsor_label, c.sponsor_show_public
      FROM collections c
      JOIN sponsors s ON c.sponsor_id = s.id
      UNION ALL
      SELECT 'temporada', t.id, t.name, ''::text,
             s.id, s.name, s.tier_default, t.sponsor_label, t.sponsor_show_public
      FROM editorial_seasons t
      JOIN sponsors s ON t.sponsor_id = s.id
      ORDER BY entity_type, entity_id
    """.as[SponsorAssignment]
    db.run(q)
  }
}

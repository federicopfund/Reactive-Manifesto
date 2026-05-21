package domains.editorial.repositories

import javax.inject.{Inject, Singleton}
import domains.editorial.models.Sponsor
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
}

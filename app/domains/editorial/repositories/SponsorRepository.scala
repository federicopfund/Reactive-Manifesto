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
}

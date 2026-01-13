package repositories

import javax.inject.{Inject, Singleton}
import models.{Publication, PublicationWithAuthor}
import slick.jdbc.H2Profile.api._
import java.time.Instant
import scala.concurrent.{ExecutionContext, Future}
import play.api.db.slick.DatabaseConfigProvider
import slick.jdbc.JdbcProfile

/**
 * Tabla de publicaciones para Slick
 */
class PublicationsTable(tag: Tag) extends Table[Publication](tag, "publications") {
  def id = column[Long]("id", O.PrimaryKey, O.AutoInc)
  def userId = column[Long]("user_id")
  def title = column[String]("title")
  def slug = column[String]("slug")
  def content = column[String]("content")
  def excerpt = column[Option[String]]("excerpt")
  def coverImage = column[Option[String]]("cover_image")
  def category = column[String]("category")
  def tags = column[Option[String]]("tags")
  def status = column[String]("status")
  def viewCount = column[Int]("view_count")
  def createdAt = column[Instant]("created_at")
  def updatedAt = column[Instant]("updated_at")
  def publishedAt = column[Option[Instant]]("published_at")
  def reviewedBy = column[Option[Long]]("reviewed_by")
  def reviewedAt = column[Option[Instant]]("reviewed_at")
  def rejectionReason = column[Option[String]]("rejection_reason")

  def * = (
    id.?,
    userId,
    title,
    slug,
    content,
    excerpt,
    coverImage,
    category,
    tags,
    status,
    viewCount,
    createdAt,
    updatedAt,
    publishedAt,
    reviewedBy,
    reviewedAt,
    rejectionReason
  ).mapTo[Publication]
}

@Singleton
class PublicationRepository @Inject()(
  dbConfigProvider: DatabaseConfigProvider
)(implicit ec: ExecutionContext) {

  private val dbConfig = dbConfigProvider.get[JdbcProfile]
  private val db = dbConfig.db
  private val publications = TableQuery[PublicationsTable]
  
  // Implicit GetResult para mapear JOIN con usuarios
  import slick.jdbc.GetResult
  implicit val getPublicationWithAuthorResult: GetResult[PublicationWithAuthor] = GetResult { r =>
    PublicationWithAuthor(
      publication = Publication(
        id = Some(r.nextLong()),
        userId = r.nextLong(),
        title = r.nextString(),
        slug = r.nextString(),
        content = r.nextString(),
        excerpt = r.nextStringOption(),
        coverImage = r.nextStringOption(),
        category = r.nextString(),
        tags = r.nextStringOption(),
        status = r.nextString(),
        viewCount = r.nextInt(),
        createdAt = r.nextTimestamp().toInstant,
        updatedAt = r.nextTimestamp().toInstant,
        publishedAt = r.nextTimestampOption().map(_.toInstant),
        reviewedBy = r.nextLongOption(),
        reviewedAt = r.nextTimestampOption().map(_.toInstant),
        rejectionReason = r.nextStringOption()
      ),
      authorUsername = r.nextString(),
      authorFullName = r.nextString()
    )
  }

  /**
   * Crear una nueva publicación
   */
  def create(publication: Publication): Future[Long] = {
    val insertQuery = publications returning publications.map(_.id)
    db.run(insertQuery += publication)
  }

  /**
   * Actualizar una publicación
   */
  def update(publication: Publication): Future[Boolean] = {
    val query = publications
      .filter(p => p.id === publication.id && p.userId === publication.userId)
      .map(p => (p.title, p.slug, p.content, p.excerpt, p.coverImage, p.category, p.tags, p.status, p.updatedAt))
      .update((
        publication.title,
        publication.slug,
        publication.content,
        publication.excerpt,
        publication.coverImage,
        publication.category,
        publication.tags,
        publication.status,
        Instant.now()
      ))
    
    db.run(query).map(_ > 0)
  }

  /**
   * Obtener publicación por ID
   */
  def findById(id: Long): Future[Option[Publication]] = {
    db.run(publications.filter(_.id === id).result.headOption)
  }

  /**
   * Obtener publicación por slug
   */
  def findBySlug(slug: String): Future[Option[Publication]] = {
    db.run(
      publications
        .filter(p => p.slug === slug && p.status === "approved")
        .result
        .headOption
    )
  }

  /**
   * Listar publicaciones de un usuario
   */
  def findByUserId(userId: Long): Future[List[Publication]] = {
    db.run(
      publications
        .filter(_.userId === userId)
        .sortBy(_.createdAt.desc)
        .result
    ).map(_.toList)
  }

  /**
   * Listar todas las publicaciones aprobadas (públicas)
   */
  def findAllApproved(limit: Int = 50, offset: Int = 0): Future[List[PublicationWithAuthor]] = {
    val query = sql"""
      SELECT p.*, u.username, u.full_name
      FROM publications p
      JOIN users u ON p.user_id = u.id
      WHERE p.status = 'approved'
      ORDER BY p.published_at DESC
      LIMIT $limit OFFSET $offset
    """.as[PublicationWithAuthor]
    
    db.run(query).map(_.toList)
  }

  /**
   * Listar publicaciones pendientes de aprobación
   */
  def findPending(limit: Int = 100): Future[List[PublicationWithAuthor]] = {
    val query = sql"""
      SELECT p.*, u.username, u.full_name
      FROM publications p
      JOIN users u ON p.user_id = u.id
      WHERE p.status = 'pending'
      ORDER BY p.updated_at ASC
      LIMIT $limit
    """.as[PublicationWithAuthor]
    
    db.run(query).map(_.toList)
  }

  /**
   * Cambiar estado de una publicación (para admin)
   */
  def changeStatus(
    publicationId: Long,
    newStatus: String,
    reviewerId: Long,
    rejectionReason: Option[String] = None
  ): Future[Boolean] = {
    val now = Instant.now()
    val publishedAt = if (newStatus == "approved") Some(now) else None
    
    val query = publications
      .filter(_.id === publicationId)
      .map(p => (p.status, p.reviewedBy, p.reviewedAt, p.publishedAt, p.rejectionReason))
      .update((newStatus, Some(reviewerId), Some(now), publishedAt, rejectionReason))
    
    db.run(query).map(_ > 0)
  }

  /**
   * Eliminar publicación (solo si es del usuario)
   */
  def delete(id: Long, userId: Long): Future[Boolean] = {
    db.run(
      publications
        .filter(p => p.id === id && p.userId === userId)
        .delete
    ).map(_ > 0)
  }

  /**
   * Incrementar contador de vistas
   */
  def incrementViewCount(id: Long): Future[Unit] = {
    val query = publications
      .filter(_.id === id)
      .map(_.viewCount)
      .result
      .headOption
      .flatMap {
        case Some(count) =>
          publications
            .filter(_.id === id)
            .map(_.viewCount)
            .update(count + 1)
            .map(_ => ())
        case None => DBIO.successful(())
      }
    
    db.run(query)
  }

  /**
   * Buscar publicaciones por categoría
   */
  def findByCategory(category: String, limit: Int = 20): Future[List[PublicationWithAuthor]] = {
    val query = sql"""
      SELECT p.*, u.username, u.full_name
      FROM publications p
      JOIN users u ON p.user_id = u.id
      WHERE p.category = $category AND p.status = 'approved'
      ORDER BY p.published_at DESC
      LIMIT $limit
    """.as[PublicationWithAuthor]
    
    db.run(query).map(_.toList)
  }

  /**
   * Obtener estadísticas de publicaciones de un usuario
   */
  def getUserStats(userId: Long): Future[Map[String, Int]] = {
    val query = publications
      .filter(_.userId === userId)
      .groupBy(_.status)
      .map { case (status, group) => (status, group.length) }
    
    db.run(query.result).map(_.toMap)
  }
}

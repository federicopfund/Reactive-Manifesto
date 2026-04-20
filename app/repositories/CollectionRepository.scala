package repositories

import javax.inject.{Inject, Singleton}
import models.{Collection, CollectionItem, CollectionWithCount}
import java.time.Instant
import scala.concurrent.{ExecutionContext, Future}
import play.api.db.slick.DatabaseConfigProvider
import slick.jdbc.JdbcProfile

/**
 * Colecciones temáticas curadas + sus items.
 */
@Singleton
class CollectionRepository @Inject()(
  dbConfigProvider: DatabaseConfigProvider
)(implicit ec: ExecutionContext) {

  private val dbConfig = dbConfigProvider.get[JdbcProfile]
  import dbConfig._
  import profile.api._

  private class Collections(tag: Tag)
      extends Table[Collection](tag, "collections") {
    def id          = column[Long]("id", O.PrimaryKey, O.AutoInc)
    def slug        = column[String]("slug")
    def name        = column[String]("name")
    def description = column[Option[String]]("description")
    def coverLabel  = column[String]("cover_label")
    def curatorId   = column[Option[Long]]("curator_id")
    def isPublished = column[Boolean]("is_published")
    def orderIndex  = column[Int]("order_index")
    def createdAt   = column[Instant]("created_at")
    def updatedAt   = column[Instant]("updated_at")

    def * = (
      id.?, slug, name, description, coverLabel, curatorId,
      isPublished, orderIndex, createdAt, updatedAt
    ).mapTo[Collection]
  }

  private class CollectionItems(tag: Tag)
      extends Table[CollectionItem](tag, "collection_items") {
    def id           = column[Long]("id", O.PrimaryKey, O.AutoInc)
    def collectionId = column[Long]("collection_id")
    def itemType     = column[String]("item_type")
    def itemId       = column[Long]("item_id")
    def orderIndex   = column[Int]("order_index")
    def addedAt      = column[Instant]("added_at")

    def * = (
      id.?, collectionId, itemType, itemId, orderIndex, addedAt
    ).mapTo[CollectionItem]
  }

  private val collections = TableQuery[Collections]
  private val items       = TableQuery[CollectionItems]

  /** Colecciones publicadas con conteo de piezas, en orden canónico. */
  def findPublishedWithCounts(): Future[Seq[CollectionWithCount]] = {
    val q = collections
      .filter(_.isPublished)
      .joinLeft(items).on(_.id === _.collectionId)
      .result

    db.run(q).map { rows =>
      rows.groupBy(_._1.id).toSeq.map { case (_, group) =>
        val coll  = group.head._1
        val count = group.count(_._2.isDefined)
        CollectionWithCount(coll, count)
      }.sortBy(_.collection.orderIndex)
    }
  }

  def findBySlug(slug: String): Future[Option[Collection]] =
    db.run(collections.filter(_.slug === slug).result.headOption)

  def findItems(collectionId: Long): Future[Seq[CollectionItem]] =
    db.run(items.filter(_.collectionId === collectionId).sortBy(_.orderIndex.asc).result)
}

package models

import java.time.Instant

/**
 * Colección temática curada por un editor.
 * Agrupa piezas de publications + editorial_articles vía collection_items.
 */
case class Collection(
  id:          Option[Long]   = None,
  slug:        String,
  name:        String,
  description: Option[String] = None,
  coverLabel:  String         = "COLECCIÓN",
  curatorId:   Option[Long]   = None,
  isPublished: Boolean        = true,
  orderIndex:  Int            = 100,
  createdAt:   Instant        = Instant.now(),
  updatedAt:   Instant        = Instant.now()
)

object CollectionItemType {
  val Publication      = "publication"
  val EditorialArticle = "editorial_article"
  val all: Set[String] = Set(Publication, EditorialArticle)
}

case class CollectionItem(
  id:           Option[Long] = None,
  collectionId: Long,
  itemType:     String,
  itemId:       Long,
  orderIndex:   Int          = 100,
  addedAt:      Instant      = Instant.now()
)

/** Colección con conteo de piezas (para listados/portafolio). */
case class CollectionWithCount(
  collection: Collection,
  itemCount:  Int
)

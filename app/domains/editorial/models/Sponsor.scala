package domains.editorial.models

import java.time.Instant

/**
 * Empresa / organización patrocinadora (Issue #24 — Evolution 31).
 *
 * Tiers:
 *   gold   → Temporada completa. Exclusividad máxima, 1 empresa / temporada.
 *   silver → Colección temática. 1 empresa / colección.
 *   bronze → Evento puntual. Hasta 2 empresas / evento.
 */
case class Sponsor(
  id:           Option[Long]   = None,
  slug:         String,
  name:         String,
  website:      Option[String] = None,
  logoUrl:      Option[String] = None,
  contactName:  Option[String] = None,
  contactEmail: Option[String] = None,
  contactRole:  Option[String] = None,
  tierDefault:  String         = "bronze",
  notes:        Option[String] = None,
  isActive:     Boolean        = true,
  createdAt:    Instant        = Instant.now(),
  updatedAt:    Instant        = Instant.now()
) {
  /** Tier formateado para mostrar en la UI. */
  def tierLabel: String = tierDefault match {
    case "gold"   => "Gold"
    case "silver" => "Silver"
    case "bronze" => "Bronze"
    case other    => other.capitalize
  }
}

/**
 * Resumen de una asignación de sponsor a una entidad del sistema.
 * Se usa en la vista de /admin/comercial/asignaciones.
 */
case class SponsorAssignment(
  entityType:   String,         // "publicacion" | "evento" | "coleccion" | "temporada"
  entityId:     Long,
  entityTitle:  String,
  entitySlug:   String,
  sponsorId:    Long,
  sponsorName:  String,
  sponsorTier:  String,
  sponsorLabel: Option[String],
  showPublic:   Boolean
) {
  def entityTypeLabel: String = entityType match {
    case "publicacion" => "Publicación"
    case "evento"      => "Evento"
    case "coleccion"   => "Colección"
    case "temporada"   => "Temporada"
    case other         => other.capitalize
  }
  def tierLabel: String = sponsorTier match {
    case "gold"   => "Gold"
    case "silver" => "Silver"
    case "bronze" => "Bronze"
    case other    => other.capitalize
  }
}

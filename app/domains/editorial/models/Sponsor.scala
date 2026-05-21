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

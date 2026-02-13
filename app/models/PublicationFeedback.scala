package models

import java.time.Instant

/**
 * Tipos de feedback del admin al usuario:
 *
 *  - content_quality : Calidad de Contenido (profundidad, originalidad, precisión)
 *  - structure       : Estructura y Formato (organización, legibilidad, secciones)
 *  - relevance       : Relevancia Temática  (alineación con la categoría elegida)
 *  - writing_style   : Redacción y Estilo  (gramática, claridad, tono)
 *  - general         : Sugerencia General   (observaciones abiertas)
 */
object FeedbackType extends Enumeration {
  type FeedbackType = Value
  val ContentQuality = Value("content_quality")
  val Structure      = Value("structure")
  val Relevance      = Value("relevance")
  val WritingStyle   = Value("writing_style")
  val General        = Value("general")

  /** Etiqueta legible para la vista */
  def label(ft: String): String = ft match {
    case "content_quality" => "Calidad de Contenido"
    case "structure"       => "Estructura y Formato"
    case "relevance"       => "Relevancia Temática"
    case "writing_style"   => "Redacción y Estilo"
    case "general"         => "Sugerencia General"
    case other             => other
  }

  /** Icono para la vista */
  def icon(ft: String): String = ft match {
    case "content_quality" => "\uD83D\uDCCA"  // 📊
    case "structure"       => "\uD83D\uDCC1"  // 📁
    case "relevance"       => "\uD83C\uDFAF"  // 🎯
    case "writing_style"   => "\u270D\uFE0F"   // ✍️
    case "general"         => "\uD83D\uDCA1"   // 💡
    case _                 => "\uD83D\uDCDD"   // 📝
  }
}

case class PublicationFeedback(
  id: Option[Long] = None,
  publicationId: Long,
  adminId: Long,
  feedbackType: String,
  message: String,
  sentToUser: Boolean = false,
  createdAt: Instant = Instant.now()
)

case class PublicationFeedbackWithAdmin(
  feedback: PublicationFeedback,
  adminUsername: String
)

package controllers

import org.scalatest.matchers.should.Matchers
import org.scalatest.wordspec.AnyWordSpec

class AdminSeasonValidationSpec extends AnyWordSpec with Matchers {

  "AdminSeasonValidation.validate" should {
    "aceptar fechas válidas con inicio menor a fin" in {
      val data = Map(
        "code" -> "2026-q3",
        "name" -> "Temporada 2026 Q3",
        "tagline" -> "Tagline",
        "openingEssay" -> "Ensayo",
        "startsOn" -> "2026-07-01",
        "endsOn" -> "2026-09-30"
      )

      AdminSeasonValidation.validate(data, isCreate = true).isRight shouldBe true
    }

    "rechazar cuando startsOn no es menor que endsOn" in {
      val data = Map(
        "code" -> "2026-q3",
        "name" -> "Temporada 2026 Q3",
        "startsOn" -> "2026-09-30",
        "endsOn" -> "2026-09-30"
      )

      val result = AdminSeasonValidation.validate(data, isCreate = true)
      result.isLeft shouldBe true
      result.left.toOption.get should contain key "endsOn"
    }

    "rechazar código inválido al crear" in {
      val data = Map(
        "code" -> "Temporada 2026",
        "name" -> "Temporada 2026 Q3",
        "startsOn" -> "",
        "endsOn" -> ""
      )

      val result = AdminSeasonValidation.validate(data, isCreate = true)
      result.isLeft shouldBe true
      result.left.toOption.get should contain key "code"
    }
  }
}

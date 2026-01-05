package controllers

import akka.actor.typed.ActorSystem
import org.scalatestplus.play._
import org.scalatestplus.play.guice._
import play.api.test._
import play.api.test.Helpers._
import play.api.inject.guice.GuiceApplicationBuilder
import services.ReactiveContactAdapter
import core._

/**
 * Test suite for ContactController following Reactive principles
 */
class ContactControllerSpec extends PlaySpec with GuiceOneAppPerTest with Injecting {

  "ContactController GET /contact" should {

    "render the contact form" in {
      val controller = inject[ContactController]
      val request = FakeRequest(GET, "/contact")
      val result = controller.form().apply(request)

      status(result) mustBe OK
      contentType(result) mustBe Some("text/html")
      contentAsString(result) must include("Formulario de Contacto")
      contentAsString(result) must include("Principios Reactivos")
    }

    "include CSRF token in form" in {
      val controller = inject[ContactController]
      val request = FakeRequest(GET, "/contact")
      val result = controller.form().apply(request)

      contentAsString(result) must include("csrfToken")
    }
  }

  "ContactController POST /contact" should {

    "accept valid contact data" in {
      val controller = inject[ContactController]
      val request = FakeRequest(POST, "/contact")
        .withFormUrlEncodedBody(
          "name" -> "Juan Pérez",
          "email" -> "juan@example.com",
          "message" -> "Este es un mensaje de prueba con más de 10 caracteres",
          "subject" -> "Consulta"
        )

      val result = controller.submit().apply(request)

      status(result) mustBe OK
      contentAsString(result) must include("Mensaje enviado correctamente")
    }

    "reject empty name" in {
      val controller = inject[ContactController]
      val request = FakeRequest(POST, "/contact")
        .withFormUrlEncodedBody(
          "name" -> "",
          "email" -> "juan@example.com",
          "message" -> "Este es un mensaje de prueba"
        )

      val result = controller.submit().apply(request)

      status(result) mustBe BAD_REQUEST
      contentAsString(result) must include("error")
    }

    "reject invalid email" in {
      val controller = inject[ContactController]
      val request = FakeRequest(POST, "/contact")
        .withFormUrlEncodedBody(
          "name" -> "Juan Pérez",
          "email" -> "invalid-email",
          "message" -> "Este es un mensaje de prueba con más de 10 caracteres"
        )

      val result = controller.submit().apply(request)

      status(result) mustBe BAD_REQUEST
    }

    "reject short message" in {
      val controller = inject[ContactController]
      val request = FakeRequest(POST, "/contact")
        .withFormUrlEncodedBody(
          "name" -> "Juan Pérez",
          "email" -> "juan@example.com",
          "message" -> "Corto"
        )

      val result = controller.submit().apply(request)

      status(result) mustBe BAD_REQUEST
    }

    "handle subject field correctly" in {
      val controller = inject[ContactController]
      val request = FakeRequest(POST, "/contact")
        .withFormUrlEncodedBody(
          "name" -> "Juan Pérez",
          "email" -> "juan@example.com",
          "message" -> "Este es un mensaje de prueba con más de 10 caracteres",
          "subject" -> "Mi asunto importante"
        )

      val result = controller.submit().apply(request)

      status(result) mustBe OK
    }
  }

  "ContactController GET /contact/health" should {

    "return OK when system is healthy" in {
      val controller = inject[ContactController]
      val request = FakeRequest(GET, "/contact/health")
      val result = controller.health().apply(request)

      status(result) mustBe OK
      contentAsString(result) mustBe "OK"
    }
  }

  "ContactController GET /contact/stats" should {

    "return statistics in JSON format" in {
      val controller = inject[ContactController]
      val request = FakeRequest(GET, "/contact/stats")
      val result = controller.stats().apply(request)

      status(result) mustBe OK
      contentType(result) mustBe Some("application/json")
      contentAsString(result) must include("received")
      contentAsString(result) must include("accepted")
      contentAsString(result) must include("rejected")
    }
  }
}

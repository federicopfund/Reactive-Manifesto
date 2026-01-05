package core

import akka.actor.testkit.typed.scaladsl.ScalaTestWithActorTestKit
import org.scalatest.wordspec.AnyWordSpecLike

/**
 * Test suite for ContactEngine Actor
 * Tests the core reactive behavior and validation logic
 */
class ContactEngineSpec extends ScalaTestWithActorTestKit with AnyWordSpecLike {

  "ContactEngine" should {

    "accept valid contact submission" in {
      val contactEngine = testKit.spawn(ContactEngine())
      val probe = testKit.createTestProbe[ContactResponse]()

      contactEngine ! SubmitContact(
        name = "Juan Pérez",
        email = "juan@example.com",
        message = "Este es un mensaje válido con más de 10 caracteres",
        replyTo = probe.ref
      )

      probe.expectMessage(ContactAccepted)
    }

    "reject empty name" in {
      val contactEngine = testKit.spawn(ContactEngine())
      val probe = testKit.createTestProbe[ContactResponse]()

      contactEngine ! SubmitContact(
        name = "",
        email = "juan@example.com",
        message = "Este es un mensaje válido",
        replyTo = probe.ref
      )

      val response = probe.expectMessageType[ContactRejected]
      response.reason should include("nombre")
    }

    "reject name with only one character" in {
      val contactEngine = testKit.spawn(ContactEngine())
      val probe = testKit.createTestProbe[ContactResponse]()

      contactEngine ! SubmitContact(
        name = "J",
        email = "juan@example.com",
        message = "Este es un mensaje válido",
        replyTo = probe.ref
      )

      val response = probe.expectMessageType[ContactRejected]
      response.reason should include("2 caracteres")
    }

    "reject name longer than 100 characters" in {
      val contactEngine = testKit.spawn(ContactEngine())
      val probe = testKit.createTestProbe[ContactResponse]()

      contactEngine ! SubmitContact(
        name = "A" * 101,
        email = "juan@example.com",
        message = "Este es un mensaje válido",
        replyTo = probe.ref
      )

      val response = probe.expectMessageType[ContactRejected]
      response.reason should include("100 caracteres")
    }

    "reject empty message" in {
      val contactEngine = testKit.spawn(ContactEngine())
      val probe = testKit.createTestProbe[ContactResponse]()

      contactEngine ! SubmitContact(
        name = "Juan Pérez",
        email = "juan@example.com",
        message = "",
        replyTo = probe.ref
      )

      val response = probe.expectMessageType[ContactRejected]
      response.reason should include("mensaje")
    }

    "reject short message" in {
      val contactEngine = testKit.spawn(ContactEngine())
      val probe = testKit.createTestProbe[ContactResponse]()

      contactEngine ! SubmitContact(
        name = "Juan Pérez",
        email = "juan@example.com",
        message = "Corto",
        replyTo = probe.ref
      )

      val response = probe.expectMessageType[ContactRejected]
      response.reason should include("10 caracteres")
    }

    "reject message longer than 5000 characters" in {
      val contactEngine = testKit.spawn(ContactEngine())
      val probe = testKit.createTestProbe[ContactResponse]()

      contactEngine ! SubmitContact(
        name = "Juan Pérez",
        email = "juan@example.com",
        message = "A" * 5001,
        replyTo = probe.ref
      )

      val response = probe.expectMessageType[ContactRejected]
      response.reason should include("5000 caracteres")
    }

    "reject invalid email format" in {
      val contactEngine = testKit.spawn(ContactEngine())
      val probe = testKit.createTestProbe[ContactResponse]()

      contactEngine ! SubmitContact(
        name = "Juan Pérez",
        email = "invalid-email",
        message = "Este es un mensaje válido",
        replyTo = probe.ref
      )

      val response = probe.expectMessageType[ContactRejected]
      response.reason should include("email")
    }

    "accept valid email formats" in {
      val contactEngine = testKit.spawn(ContactEngine())
      val probe = testKit.createTestProbe[ContactResponse]()

      val validEmails = Seq(
        "user@example.com",
        "user.name@example.com",
        "user+tag@example.co.uk",
        "user_name@sub.example.com"
      )

      validEmails.foreach { email =>
        contactEngine ! SubmitContact(
          name = "Juan Pérez",
          email = email,
          message = "Este es un mensaje válido con más de 10 caracteres",
          replyTo = probe.ref
        )

        probe.expectMessage(ContactAccepted)
      }
    }

    "maintain state correctly" in {
      val contactEngine = testKit.spawn(ContactEngine())
      val probe = testKit.createTestProbe[ContactResponse]()
      val statsProbe = testKit.createTestProbe[ContactResponse]()

      // Submit valid contact
      contactEngine ! SubmitContact(
        name = "Juan Pérez",
        email = "juan@example.com",
        message = "Este es un mensaje válido",
        replyTo = probe.ref
      )
      probe.expectMessage(ContactAccepted)

      // Submit invalid contact
      contactEngine ! SubmitContact(
        name = "",
        email = "invalid",
        message = "x",
        replyTo = probe.ref
      )
      probe.expectMessageType[ContactRejected]

      // Check stats
      contactEngine ! GetContactStats(statsProbe.ref)
      val stats = statsProbe.expectMessageType[ContactStatsResponse]

      stats.totalReceived shouldBe 2
      stats.totalAccepted shouldBe 1
      stats.totalRejected shouldBe 1
    }

    "handle multiple concurrent requests" in {
      val contactEngine = testKit.spawn(ContactEngine())
      val probes = (1 to 10).map(_ => testKit.createTestProbe[ContactResponse]())

      // Send 10 concurrent requests
      probes.foreach { probe =>
        contactEngine ! SubmitContact(
          name = "Juan Pérez",
          email = "juan@example.com",
          message = "Este es un mensaje válido",
          replyTo = probe.ref
        )
      }

      // All should succeed
      probes.foreach { probe =>
        probe.expectMessage(ContactAccepted)
      }
    }
  }

  "ContactEngine.supervised" should {

    "restart on failure" in {
      val supervisedEngine = testKit.spawn(ContactEngine.supervised())
      val probe = testKit.createTestProbe[ContactResponse]()

      // This should work normally
      supervisedEngine ! SubmitContact(
        name = "Juan Pérez",
        email = "juan@example.com",
        message = "Este es un mensaje válido",
        replyTo = probe.ref
      )

      probe.expectMessage(ContactAccepted)

      // Engine should continue working after processing
      supervisedEngine ! SubmitContact(
        name = "María García",
        email = "maria@example.com",
        message = "Otro mensaje válido para probar",
        replyTo = probe.ref
      )

      probe.expectMessage(ContactAccepted)
    }
  }
}

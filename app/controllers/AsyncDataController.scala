package controllers

import play.api.libs.json._
import play.api.mvc._

import javax.inject._
import scala.concurrent.{ExecutionContext, Future}
import scala.concurrent.duration._
import akka.pattern.after
import akka.actor.ActorSystem

@Singleton
class AsyncDataController @Inject()(
  cc: ControllerComponents,
  actorSystem: ActorSystem
)(implicit ec: ExecutionContext)
  extends AbstractController(cc) {

  /**
   * Endpoint reactivo que simula la obtención de datos de forma asíncrona
   * Demuestra: Non-blocking I/O, manejo de Futures, y timeouts
   */
  def getData: Action[AnyContent] = Action.async {
    // Simula una llamada asíncrona a un servicio externo o base de datos
    val futureData = after(500.millis, actorSystem.scheduler) {
      Future.successful(
        Json.obj(
          "status" -> "success",
          "data" -> Json.arr(
            Json.obj("id" -> 1, "name" -> "Reactive Item 1"),
            Json.obj("id" -> 2, "name" -> "Reactive Item 2"),
            Json.obj("id" -> 3, "name" -> "Reactive Item 3")
          ),
          "timestamp" -> System.currentTimeMillis()
        )
      )
    }

    // Aplica timeout para resiliencia
    val timeout = after(2.seconds, actorSystem.scheduler)(
      Future.successful(
        Json.obj(
          "status" -> "timeout",
          "message" -> "Request timed out"
        )
      )
    )

    Future.firstCompletedOf(Seq(futureData, timeout)).map { result =>
      Ok(result)
    }
  }

  /**
   * Endpoint reactivo que combina múltiples fuentes de datos de forma asíncrona
   * Demuestra: Composición de Futures, paralelización
   */
  def getCombinedData: Action[AnyContent] = Action.async {
    val futureUsers = after(300.millis, actorSystem.scheduler) {
      Future.successful(
        Json.arr(
          Json.obj("userId" -> 1, "username" -> "reactive_user1"),
          Json.obj("userId" -> 2, "username" -> "reactive_user2")
        )
      )
    }

    val futurePosts = after(200.millis, actorSystem.scheduler) {
      Future.successful(
        Json.arr(
          Json.obj("postId" -> 1, "title" -> "Reactive Programming"),
          Json.obj("postId" -> 2, "title" -> "Akka Streams")
        )
      )
    }

    val futureComments = after(250.millis, actorSystem.scheduler) {
      Future.successful(
        Json.arr(
          Json.obj("commentId" -> 1, "text" -> "Great post!"),
          Json.obj("commentId" -> 2, "text" -> "Very informative")
        )
      )
    }

    // Combina todas las fuentes de forma paralela
    for {
      users <- futureUsers
      posts <- futurePosts
      comments <- futureComments
    } yield {
      Ok(Json.obj(
        "users" -> users,
        "posts" -> posts,
        "comments" -> comments,
        "timestamp" -> System.currentTimeMillis()
      ))
    }
  }

  /**
   * Endpoint reactivo con manejo de errores
   * Demuestra: Error handling en contexto reactivo, fallback patterns
   */
  def getDataWithErrorHandling: Action[AnyContent] = Action.async {
    val futureData = Future {
      val random = scala.util.Random.nextInt(100)
      if (random < 30) {
        throw new RuntimeException("Simulated service failure")
      }
      Json.obj(
        "status" -> "success",
        "random" -> random,
        "message" -> "Data fetched successfully"
      )
    }

    futureData.recover {
      case ex: RuntimeException =>
        // Fallback response en caso de error
        InternalServerError(Json.obj(
          "status" -> "error",
          "message" -> ex.getMessage,
          "fallback" -> Json.obj(
            "data" -> "cached data or default value"
          )
        ))
      case ex: Exception =>
        InternalServerError(Json.obj(
          "status" -> "error",
          "message" -> "Unknown error occurred"
        ))
    }
  }
}

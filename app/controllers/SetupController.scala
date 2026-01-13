package controllers

import javax.inject._
import play.api.mvc._
import play.api.{Configuration, Environment}
import repositories.AdminRepository
import models.Admin
import scala.concurrent.{ExecutionContext, Future}
import play.api.libs.json.Json

@Singleton
class SetupController @Inject()(
  cc: ControllerComponents,
  adminRepo: AdminRepository,
  config: Configuration,
  env: Environment
)(implicit ec: ExecutionContext) extends AbstractController(cc) {

  // Solo habilitar en modo desarrollo o si la variable de entorno SETUP_ENABLED está activada
  private def isSetupEnabled: Boolean = {
    env.mode == play.api.Mode.Dev || config.getOptional[Boolean]("setup.enabled").getOrElse(false)
  }

  private def withSetupAccess(block: => Future[Result]): Future[Result] = {
    if (isSetupEnabled) {
      block
    } else {
      Future.successful(Forbidden(Json.obj(
        "error" -> "Setup endpoints están deshabilitados en producción",
        "message" -> "Por razones de seguridad, estos endpoints solo están disponibles en modo desarrollo"
      )))
    }
  }

  /**
   * Endpoint temporal para crear admin inicial
   * Acceder a: http://localhost:9000/setup/create-admin
   */
  def createInitialAdmin(): Action[AnyContent] = Action.async { implicit request =>
    withSetupAccess {
    val adminData = Admin(
      id = None,
      username = "federico",
      email = "federico@reactivemanifesto.com",
      passwordHash = "$2a$10$So8GceVpZX3J2ZX4ARqViuj9ldnk3uupjDGWGk9kReFufCpup3m1C",
      role = "admin",
      createdAt = java.time.Instant.now(),
      lastLogin = None
    )

    adminRepo.findByUsername("federico").flatMap {
      case Some(_) =>
        Future.successful(Ok(Json.obj(
          "success" -> false,
          "message" -> "El admin 'federico' ya existe",
          "action" -> "Usa /setup/list-admins para ver los admins existentes"
        )))
      case None =>
        adminRepo.create(adminData).map { admin =>
          Ok(Json.obj(
            "success" -> true,
            "message" -> "Admin creado exitosamente",
            "admin" -> Json.obj(
              "id" -> admin.id,
              "username" -> admin.username,
              "email" -> admin.email,
              "role" -> admin.role
            ),
            "credentials" -> Json.obj(
              "username" -> "federico",
              "password" -> "Fede/(40021)",
              "loginUrl" -> "/admin/login"
            )
          ))
        }
    }
    }
  }

  /**
   * Listar todos los admins
   * Acceder a: http://localhost:9000/setup/list-admins
   */
  def listAdmins(): Action[AnyContent] = Action.async { implicit request =>
    withSetupAccess {
      adminRepo.listAll().map { admins =>
      Ok(Json.obj(
        "success" -> true,
        "count" -> admins.length,
        "admins" -> Json.toJson(admins.map { admin =>
          Json.obj(
            "id" -> admin.id,
            "username" -> admin.username,
            "email" -> admin.email,
            "role" -> admin.role,
            "createdAt" -> admin.createdAt.toString,
            "lastLogin" -> admin.lastLogin.map(_.toString).getOrElse[String]("Nunca")
          )
        })
      ))
    }    }  }

  /**
   * Actualizar contraseña de un admin
   * Acceder a: http://localhost:9000/setup/update-password?username=admin&password=admin123
   */
  def updatePassword(username: String, password: String): Action[AnyContent] = Action.async { implicit request =>
    withSetupAccess {
      import org.mindrot.jbcrypt.BCrypt
    
    val newHash = BCrypt.hashpw(password, BCrypt.gensalt())
    
    adminRepo.findByUsername(username).flatMap {
      case Some(admin) =>
        adminRepo.updatePassword(admin.id.get, newHash).map { _ =>
          Ok(Json.obj(
            "success" -> true,
            "message" -> s"Contraseña actualizada para '$username'",
            "credentials" -> Json.obj(
              "username" -> username,
              "password" -> password,
              "loginUrl" -> "/admin/login"
            )
          ))
        }
      case None =>
        Future.successful(NotFound(Json.obj(
          "success" -> false,
          "message" -> s"Admin '$username' no encontrado"
        )))
    }
    }
  }

  /**
   * Probar login de admin
   * Acceder a: http://localhost:9000/setup/test-login?username=admin&password=admin123
   */
  def testLogin(username: String, password: String): Action[AnyContent] = Action.async { implicit request =>
    withSetupAccess {
      import org.mindrot.jbcrypt.BCrypt
    
    adminRepo.findByUsername(username).map {
      case Some(admin) =>
        val passwordMatch = BCrypt.checkpw(password, admin.passwordHash)
        Ok(Json.obj(
          "success" -> passwordMatch,
          "message" -> (if (passwordMatch) "Credenciales válidas" else "Contraseña incorrecta"),
          "admin" -> Json.obj(
            "id" -> admin.id,
            "username" -> admin.username,
            "email" -> admin.email,
            "role" -> admin.role
          )
        ))
      case None =>
        NotFound(Json.obj(
          "success" -> false,
          "message" -> s"Admin '$username' no encontrado"
        ))
    }
    }
  }
}

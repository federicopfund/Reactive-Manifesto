package controllers

import javax.inject._
import play.api.mvc._
import play.api.data._
import play.api.data.Forms._
import play.api.i18n.I18nSupport
import scala.concurrent.{ExecutionContext, Future}
import repositories.{UserRepository, AdminRepository}
import services.EmailVerificationService
import models.User
import org.mindrot.jbcrypt.BCrypt
import java.time.Instant

case class UserLoginForm(username: String, password: String, loginType: String)
case class UserRegisterForm(username: String, email: String, password: String, confirmPassword: String, fullName: String)
case class EmailVerificationForm(userId: Long, code: String)

@Singleton
class AuthController @Inject()(
  cc: ControllerComponents,
  userRepository: UserRepository,
  adminRepository: AdminRepository,
  emailVerificationService: EmailVerificationService
)(implicit ec: ExecutionContext) extends AbstractController(cc) with I18nSupport {

  // Formulario de login unificado
  val loginForm = Form(
    mapping(
      "username" -> nonEmptyText,
      "password" -> nonEmptyText(minLength = 6),
      "loginType" -> nonEmptyText
    )(UserLoginForm.apply)(UserLoginForm.unapply)
  )

  // Formulario de registro
  val registerForm = Form(
    mapping(
      "username" -> nonEmptyText(minLength = 3, maxLength = 50),
      "email" -> email,
      "password" -> nonEmptyText(minLength = 6),
      "confirmPassword" -> nonEmptyText(minLength = 6),
      "fullName" -> nonEmptyText
    )(UserRegisterForm.apply)(UserRegisterForm.unapply)
      .verifying("Las contraseñas no coinciden", fields => fields match {
        case form => form.password == form.confirmPassword
      })
  )

  // Formulario de verificación de email
  val verificationForm = Form(
    mapping(
      "userId" -> longNumber,
      "code" -> nonEmptyText(minLength = 3, maxLength = 3)
    )(EmailVerificationForm.apply)(EmailVerificationForm.unapply)
  )

  // Helpers de autenticación
  private def isUserAuthenticated(request: RequestHeader): Boolean = {
    request.session.get("userId").isDefined
  }

  private def isAdminAuthenticated(request: RequestHeader): Boolean = {
    request.session.get("userId").exists(_ => 
      request.session.get("userRole").contains("admin")
    )
  }

  private def withUserAuth(block: => Future[Result])(implicit request: RequestHeader): Future[Result] = {
    if (isUserAuthenticated(request)) {
      block
    } else {
      Future.successful(Redirect(routes.AuthController.loginPage()).withNewSession)
    }
  }

  /**
   * Página de login unificada (usuarios y admins)
   */
  def loginPage(): Action[AnyContent] = Action { implicit request =>
    // Si ya está logueado, redirigir según el rol
    request.session.get("userId") match {
      case Some(_) =>
        request.session.get("userRole") match {
          case Some("admin") => Redirect(routes.AdminController.dashboard(0, None))
          case _ => Redirect(routes.AuthController.userDashboard())
        }
      case None => Ok(views.html.auth.login(loginForm))
    }
  }

  /**
   * Procesar login unificado
   */
  def login(): Action[AnyContent] = Action.async { implicit request =>
    loginForm.bindFromRequest().fold(
      formWithErrors => {
        Future.successful(BadRequest(views.html.auth.login(formWithErrors)))
      },
      loginData => {
        loginData.loginType match {
          case "user" => authenticateUser(loginData.username, loginData.password)
          case "admin" => authenticateAdmin(loginData.username, loginData.password)
          case _ => Future.successful(BadRequest(views.html.auth.login(
            loginForm.withGlobalError("Tipo de usuario inválido")
          )))
        }
      }
    )
  }

  /**
   * Autenticar usuario común
   */
  private def authenticateUser(username: String, password: String)(implicit request: RequestHeader): Future[Result] = {
    userRepository.findByUsername(username).flatMap {
      case Some(user) if BCrypt.checkpw(password, user.passwordHash) =>
        if (!user.emailVerified) {
          // Usuario no verificado, enviar código
          emailVerificationService.createAndSendCode(user.id.get, user.email).map { _ =>
            Redirect(routes.AuthController.verifyEmailPage(user.id.get))
              .flashing("info" -> "Por favor verifica tu email para continuar")
          }
        } else {
          // Usuario verificado, login normal
          userRepository.updateLastLogin(user.id.get).map { _ =>
            Redirect(routes.AuthController.userDashboard())
              .withSession("userId" -> user.id.get.toString, "username" -> user.username, "userRole" -> user.role)
              .flashing("success" -> s"Bienvenido, ${user.fullName}")
          }
        }
      case _ =>
        Future.successful(
          Unauthorized(views.html.auth.login(loginForm.withGlobalError("Credenciales inválidas")))
        )
    }
  }

  /**
   * Autenticar administrador
   */
  private def authenticateAdmin(username: String, password: String)(implicit request: RequestHeader): Future[Result] = {
    adminRepository.findByUsername(username).flatMap {
      case Some(admin) if BCrypt.checkpw(password, admin.passwordHash) =>
        adminRepository.updateLastLogin(admin.id.get).map { _ =>
          Redirect(routes.AdminController.dashboard(0, None))
            .withSession(
              "userId" -> admin.id.get.toString,
              "username" -> admin.username,
              "userRole" -> "admin"
            )
            .flashing("success" -> s"Bienvenido Admin, ${admin.username}")
        }
      case _ =>
        Future.successful(
          Unauthorized(views.html.auth.login(loginForm.withGlobalError("Credenciales de administrador inválidas")))
        )
    }
  }

  /**
   * Página de registro
   */
  def registerPage(): Action[AnyContent] = Action { implicit request =>
    if (isUserAuthenticated(request)) {
      Redirect(routes.AuthController.userDashboard())
    } else {
      Ok(views.html.auth.register(registerForm))
    }
  }

  /**
   * Procesar registro
   */
  def register(): Action[AnyContent] = Action.async { implicit request =>
    registerForm.bindFromRequest().fold(
      formWithErrors => {
        Future.successful(BadRequest(views.html.auth.register(formWithErrors)))
      },
      registerData => {
        // Verificar si el usuario o email ya existe
        userRepository.usernameExists(registerData.username).flatMap { usernameExists =>
          if (usernameExists) {
            Future.successful(BadRequest(views.html.auth.register(
              registerForm.withError("username", "Este nombre de usuario ya está en uso")
            )))
          } else {
            userRepository.emailExists(registerData.email).flatMap { emailExists =>
              if (emailExists) {
                Future.successful(BadRequest(views.html.auth.register(
                  registerForm.withError("email", "Este email ya está registrado")
                )))
              } else {
                // Crear nuevo usuario
                val hashedPassword = BCrypt.hashpw(registerData.password, BCrypt.gensalt(10))
                val newUser = User(
                  id = None,
                  username = registerData.username,
                  email = registerData.email,
                  passwordHash = hashedPassword,
                  fullName = registerData.fullName,
                  role = "user",
                  isActive = true,
                  createdAt = Instant.now(),
                  lastLogin = None,
                  emailVerified = false  // Requiere verificación por código
                )
                
                userRepository.create(newUser).map { _ =>
                  Redirect(routes.AuthController.loginPage())
                    .flashing("success" -> "Registro exitoso. Por favor inicia sesión para verificar tu email.")
                }
              }
            }
          }
        }
      }
    )
  }

  /**
   * Logout unificado
   */
  def logout(): Action[AnyContent] = Action { implicit request =>
    Redirect(routes.HomeController.index())
      .withNewSession
      .flashing("success" -> "Sesión cerrada exitosamente")
  }

  /**
   * Dashboard de usuario
   */
  def userDashboard(): Action[AnyContent] = Action.async { implicit request =>
    withUserAuth {
      val userId = request.session.get("userId").get.toLong
      userRepository.findById(userId).map {
        case Some(user) => Ok(views.html.auth.userDashboard(user))
        case None => Redirect(routes.AuthController.loginPage()).withNewSession
      }
    }
  }

  /**
   * Perfil de usuario
   */
  def userProfile(): Action[AnyContent] = Action.async { implicit request =>
    withUserAuth {
      val userId = request.session.get("userId").get.toLong
      userRepository.findById(userId).map {
        case Some(user) => Ok(views.html.auth.userProfile(user))
        case None => Redirect(routes.AuthController.loginPage()).withNewSession
      }
    }
  }

  /**
   * Página de verificación de email
   */
  def verifyEmailPage(userId: Long): Action[AnyContent] = Action.async { implicit request =>
    userRepository.findById(userId).map {
      case Some(user) =>
        Ok(views.html.auth.verifyEmail(user.email, userId, None))
      case None =>
        Redirect(routes.AuthController.loginPage())
          .flashing("error" -> "Usuario no encontrado")
    }
  }

  /**
   * Procesar verificación de código
   */
  def verifyEmailCode(): Action[AnyContent] = Action.async { implicit request =>
    verificationForm.bindFromRequest().fold(
      formWithErrors => {
        Future.successful(BadRequest("Formulario inválido"))
      },
      verificationData => {
        emailVerificationService.verifyCode(verificationData.userId, verificationData.code).flatMap {
          case Right(true) =>
            // Código válido, actualizar usuario y hacer login
            for {
              _ <- userRepository.updateEmailVerified(verificationData.userId, true)
              user <- userRepository.findById(verificationData.userId)
              _ <- userRepository.updateLastLogin(verificationData.userId)
            } yield {
              user match {
                case Some(u) =>
                  Redirect(routes.AuthController.userDashboard())
                    .withSession("userId" -> u.id.get.toString, "username" -> u.username, "userRole" -> u.role)
                    .flashing("success" -> "¡Email verificado exitosamente! Bienvenido")
                case None =>
                  Redirect(routes.AuthController.loginPage())
                    .flashing("error" -> "Error al verificar email")
              }
            }
          
          case Right(false) =>
            // Este caso no debería ocurrir según la lógica del servicio, pero lo manejamos por completitud
            userRepository.findById(verificationData.userId).map { user =>
              Ok(views.html.auth.verifyEmail(
                user.map(_.email).getOrElse(""),
                verificationData.userId,
                Some("Error inesperado al verificar el código")
              ))
            }
          
          case Left(error) =>
            // Código inválido o error
            userRepository.findById(verificationData.userId).map {
              case Some(user) =>
                Ok(views.html.auth.verifyEmail(user.email, verificationData.userId, Some(error)))
              case None =>
                Redirect(routes.AuthController.loginPage())
                  .flashing("error" -> "Usuario no encontrado")
            }
        }
      }
    )
  }

  /**
   * Reenviar código de verificación
   */
  def resendVerificationCode(userId: Long): Action[AnyContent] = Action.async { implicit request =>
    userRepository.findById(userId).flatMap {
      case Some(user) =>
        emailVerificationService.createAndSendCode(userId, user.email).map { _ =>
          Redirect(routes.AuthController.verifyEmailPage(userId))
            .flashing("success" -> "Código reenviado. Revisa tu email")
        }.recover {
          case ex: Exception =>
            Redirect(routes.AuthController.verifyEmailPage(userId))
              .flashing("error" -> s"Error al reenviar código: ${ex.getMessage}")
        }
      case None =>
        Future.successful(
          Redirect(routes.AuthController.loginPage())
            .flashing("error" -> "Usuario no encontrado")
        )
    }
  }
}

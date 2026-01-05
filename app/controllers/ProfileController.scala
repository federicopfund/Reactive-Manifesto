package controllers

import javax.inject._
import play.api.mvc._

@Singleton
class ProfileController @Inject()(cc: ControllerComponents)
  extends AbstractController(cc) {

  def home = Action { implicit request =>
    Ok(views.html.home())
  }

  def portfolio = Action { implicit request =>
    Ok(views.html.portfolio())
  }

  def publications = Action { implicit request =>
    Ok(views.html.publications())
  }

  def blog = Action { implicit request =>
    Ok(views.html.blog())
  }

  def ideas = Action { implicit request =>
    Ok(views.html.ideas())
  }

  def reactiveDemo = Action { implicit request =>
    Ok(views.html.reactiveDemo())
  }
}

package services

import javax.inject.{Inject, Singleton}
import repositories.{BadgeRepository, ReactionRepository, CommentRepository, PublicationRepository}
import scala.concurrent.{ExecutionContext, Future}

@Singleton
class GamificationService @Inject()(
  badgeRepo: BadgeRepository,
  reactionRepo: ReactionRepository,
  commentRepo: CommentRepository,
  publicationRepo: PublicationRepository
)(implicit ec: ExecutionContext) {

  /**
   * Check and award badges after a publication action.
   * Call this after creating/approving publications.
   */
  def checkPublicationBadges(userId: Long): Future[List[String]] = {
    for {
      pubs <- publicationRepo.findByUserId(userId)
      pubCount = pubs.size
      approvedCount = pubs.count(_.status == "approved")
      totalViews = pubs.map(_.viewCount).sum
      pubIds = pubs.flatMap(_.id)
      totalReactions <- reactionRepo.totalReactionsForUser(userId, pubIds)
      awarded <- awardIfEligible(userId, Seq(
        ("first_publication", pubCount >= 1),
        ("five_publications", pubCount >= 5),
        ("ten_publications", pubCount >= 10),
        ("first_approved", approvedCount >= 1),
        ("ten_likes", totalReactions >= 10),
        ("fifty_likes", totalReactions >= 50),
        ("hundred_views", totalViews >= 100),
        ("five_hundred_views", totalViews >= 500)
      ))
    } yield awarded
  }

  /**
   * Check and award badges after a comment action.
   */
  def checkCommentBadges(userId: Long): Future[List[String]] = {
    for {
      commentCount <- commentRepo.countByUserId(userId)
      awarded <- awardIfEligible(userId, Seq(
        ("first_comment", commentCount >= 1),
        ("ten_comments", commentCount >= 10)
      ))
    } yield awarded
  }

  private def awardIfEligible(userId: Long, checks: Seq[(String, Boolean)]): Future[List[String]] = {
    val eligible = checks.filter(_._2).map(_._1)
    Future.sequence(eligible.map { key =>
      badgeRepo.award(userId, key).map(awarded => if (awarded) Some(key) else None)
    }).map(_.flatten.toList)
  }
}

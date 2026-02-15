import akka.actor.typed.ActorSystem
import com.google.inject.{AbstractModule, Provides, Singleton}
import core._
import services._
import repositories.{ContactRepository, PrivateMessageRepository, UserNotificationRepository, PublicationRepository, BadgeRepository}
import scala.concurrent.ExecutionContext

class Module extends AbstractModule {

  // ══════════════════════════════════════════════════════════════
  //  EXISTING AGENTS
  // ══════════════════════════════════════════════════════════════

  @Provides
  @Singleton
  def provideContactActorSystem(repository: ContactRepository)(implicit ec: ExecutionContext): ActorSystem[ContactCommand] =
    ActorSystem(ContactEngine(repository), "contact-core")

  @Provides
  @Singleton
  def provideContactAdapter(system: ActorSystem[ContactCommand])(implicit ec: ExecutionContext): ReactiveContactAdapter =
    new ReactiveContactAdapter(system)

  @Provides
  @Singleton
  def provideMessageActorSystem(
    messageRepo: PrivateMessageRepository,
    notificationRepo: UserNotificationRepository
  )(implicit ec: ExecutionContext): ActorSystem[MessageCommand] =
    ActorSystem(MessageEngine(messageRepo, notificationRepo), "message-core")

  @Provides
  @Singleton
  def provideMessageAdapter(system: ActorSystem[MessageCommand])(implicit ec: ExecutionContext): ReactiveMessageAdapter =
    new ReactiveMessageAdapter(system)

  // ══════════════════════════════════════════════════════════════
  //  DOMAIN AGENTS
  // ══════════════════════════════════════════════════════════════

  // ── PublicationEngine ──
  @Provides
  @Singleton
  def providePublicationActorSystem(
    publicationRepo: PublicationRepository,
    notificationRepo: UserNotificationRepository
  )(implicit ec: ExecutionContext): ActorSystem[PublicationCommand] =
    ActorSystem(PublicationEngine(publicationRepo, notificationRepo), "publication-core")

  @Provides
  @Singleton
  def providePublicationAdapter(system: ActorSystem[PublicationCommand])(implicit ec: ExecutionContext): ReactivePublicationAdapter =
    new ReactivePublicationAdapter(system)

  // ── GamificationEngine ──
  @Provides
  @Singleton
  def provideGamificationActorSystem(
    badgeRepo: BadgeRepository
  )(implicit ec: ExecutionContext): ActorSystem[GamificationCommand] =
    ActorSystem(GamificationEngine(badgeRepo), "gamification-core")

  @Provides
  @Singleton
  def provideGamificationAdapter(system: ActorSystem[GamificationCommand])(implicit ec: ExecutionContext): ReactiveGamificationAdapter =
    new ReactiveGamificationAdapter(system)

  // ── NotificationEngine (with Circuit Breaker) ──
  @Provides
  @Singleton
  def provideNotificationActorSystem(
    notificationRepo: UserNotificationRepository,
    emailService: EmailService
  )(implicit ec: ExecutionContext): ActorSystem[NotificationCommand] =
    ActorSystem(NotificationEngine(notificationRepo, emailService), "notification-core")

  @Provides
  @Singleton
  def provideNotificationAdapter(system: ActorSystem[NotificationCommand])(implicit ec: ExecutionContext): ReactiveNotificationAdapter =
    new ReactiveNotificationAdapter(system)

  // ── ModerationEngine ──
  @Provides
  @Singleton
  def provideModerationActorSystem()(implicit ec: ExecutionContext): ActorSystem[ModerationCommand] =
    ActorSystem(ModerationEngine(), "moderation-core")

  @Provides
  @Singleton
  def provideModerationAdapter(system: ActorSystem[ModerationCommand])(implicit ec: ExecutionContext): ReactiveModerationAdapter =
    new ReactiveModerationAdapter(system)

  // ── AnalyticsEngine ──
  @Provides
  @Singleton
  def provideAnalyticsActorSystem()(implicit ec: ExecutionContext): ActorSystem[AnalyticsCommand] =
    ActorSystem(AnalyticsEngine(), "analytics-core")

  @Provides
  @Singleton
  def provideAnalyticsAdapter(system: ActorSystem[AnalyticsCommand])(implicit ec: ExecutionContext): ReactiveAnalyticsAdapter =
    new ReactiveAnalyticsAdapter(system)

  // ══════════════════════════════════════════════════════════════
  //  INFRASTRUCTURE AGENTS (inter-agent communication)
  // ══════════════════════════════════════════════════════════════

  // ── EventBusEngine (Pub/Sub) ──
  @Provides
  @Singleton
  def provideEventBusActorSystem()(implicit ec: ExecutionContext): ActorSystem[EventBusCommand] =
    ActorSystem(EventBusEngine(), "eventbus-core")

  @Provides
  @Singleton
  def provideEventBusAdapter(system: ActorSystem[EventBusCommand])(implicit ec: ExecutionContext): ReactiveEventBusAdapter =
    new ReactiveEventBusAdapter(system)

  // ── PublicationPipelineEngine (Saga Orchestrator) ──
  @Provides
  @Singleton
  def providePipelineActorSystem(
    moderationSystem: ActorSystem[ModerationCommand],
    publicationSystem: ActorSystem[PublicationCommand],
    notificationSystem: ActorSystem[NotificationCommand],
    gamificationSystem: ActorSystem[GamificationCommand],
    analyticsSystem: ActorSystem[AnalyticsCommand],
    eventBusSystem: ActorSystem[EventBusCommand]
  )(implicit ec: ExecutionContext): ActorSystem[PipelineCommand] =
    ActorSystem(
      PublicationPipelineEngine(
        moderationSystem,
        publicationSystem,
        notificationSystem,
        gamificationSystem,
        analyticsSystem,
        eventBusSystem
      ),
      "pipeline-core"
    )

  @Provides
  @Singleton
  def providePipelineAdapter(system: ActorSystem[PipelineCommand])(implicit ec: ExecutionContext): ReactivePipelineAdapter =
    new ReactivePipelineAdapter(system)
}

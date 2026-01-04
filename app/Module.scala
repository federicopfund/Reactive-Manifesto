import akka.actor.typed.ActorSystem
import com.google.inject.{AbstractModule, Provides, Singleton}
import core._
import services.ReactiveContactAdapter

class Module extends AbstractModule {

  @Provides
  @Singleton
  def provideActorSystem(): ActorSystem[ContactCommand] =
    ActorSystem(ContactEngine(), "contact-core")

  @Provides
  @Singleton
  def provideAdapter(system: ActorSystem[ContactCommand]): ReactiveContactAdapter =
    new ReactiveContactAdapter(system)
}

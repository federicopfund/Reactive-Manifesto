import akka.actor.typed.ActorSystem
import com.google.inject.{AbstractModule, Provides, Singleton}
import core._
import services.ReactiveContactAdapter
import repositories.ContactRepository
import scala.concurrent.ExecutionContext

class Module extends AbstractModule {

  @Provides
  @Singleton
  def provideActorSystem(repository: ContactRepository)(implicit ec: ExecutionContext): ActorSystem[ContactCommand] =
    ActorSystem(ContactEngine(repository), "contact-core")

  @Provides
  @Singleton
  def provideAdapter(system: ActorSystem[ContactCommand])(implicit ec: ExecutionContext): ReactiveContactAdapter =
    new ReactiveContactAdapter(system)
}

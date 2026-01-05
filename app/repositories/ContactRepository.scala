package repositories

import models.ContactRecord
import slick.jdbc.H2Profile.api._
import java.time.Instant
import scala.concurrent.{ExecutionContext, Future}
import javax.inject.{Inject, Singleton}
import play.api.db.slick.DatabaseConfigProvider
import slick.jdbc.JdbcProfile

class ContactsTable(tag: Tag) extends Table[ContactRecord](tag, "contacts") {
  def id = column[Long]("id", O.PrimaryKey, O.AutoInc)
  def name = column[String]("name")
  def email = column[String]("email")
  def message = column[String]("message")
  def createdAt = column[Instant]("created_at")
  def status = column[String]("status")

  def * = (id.?, name, email, message, createdAt, status).mapTo[ContactRecord]
}

@Singleton
class ContactRepository @Inject()(
  dbConfigProvider: DatabaseConfigProvider
)(implicit ec: ExecutionContext) {

  private val dbConfig = dbConfigProvider.get[JdbcProfile]
  private val db = dbConfig.db
  private val contacts = TableQuery[ContactsTable]

  /**
   * Guarda un nuevo contacto en la base de datos
   */
  def save(contact: ContactRecord): Future[ContactRecord] = {
    val insertQuery = contacts returning contacts.map(_.id) into ((contact, id) => contact.copy(id = Some(id)))
    db.run(insertQuery += contact)
  }

  /**
   * Busca un contacto por ID
   */
  def findById(id: Long): Future[Option[ContactRecord]] = {
    db.run(contacts.filter(_.id === id).result.headOption)
  }

  /**
   * Lista todos los contactos
   */
  def listAll(): Future[Seq[ContactRecord]] = {
    db.run(contacts.sortBy(_.createdAt.desc).result)
  }

  /**
   * Lista contactos con paginación
   */
  def list(page: Int = 0, pageSize: Int = 20): Future[Seq[ContactRecord]] = {
    val offset = page * pageSize
    db.run(
      contacts
        .sortBy(_.createdAt.desc)
        .drop(offset)
        .take(pageSize)
        .result
    )
  }

  /**
   * Busca contactos por email
   */
  def findByEmail(email: String): Future[Seq[ContactRecord]] = {
    db.run(contacts.filter(_.email === email).result)
  }

  /**
   * Actualiza el estado de un contacto
   */
  def updateStatus(id: Long, newStatus: String): Future[Int] = {
    val query = contacts.filter(_.id === id).map(_.status).update(newStatus)
    db.run(query)
  }

  /**
   * Cuenta el total de contactos
   */
  def count(): Future[Int] = {
    db.run(contacts.length.result)
  }

  /**
   * Elimina un contacto por ID
   */
  def delete(id: Long): Future[Int] = {
    db.run(contacts.filter(_.id === id).delete)
  }
}

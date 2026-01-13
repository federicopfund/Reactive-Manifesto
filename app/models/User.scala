package models

import java.time.Instant

case class User(
  id: Option[Long] = None,
  username: String,
  email: String,
  passwordHash: String,
  fullName: String,
  role: String = "user",
  isActive: Boolean = true,
  createdAt: Instant = Instant.now(),
  lastLogin: Option[Instant] = None,
  emailVerified: Boolean = false
)

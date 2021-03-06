package com.thenewmotion.ocpi.msgs

import java.security.SecureRandom
import Ownership.Theirs

sealed trait Ownership {
  type Opposite <: Ownership
}

object Ownership {
  trait Theirs extends Ownership {
    type Opposite = Ours
  }
  trait Ours extends Ownership {
    type Opposite = Theirs
  }
}

case class AuthToken[O <: Ownership](value: String) {
  override def toString = value.substring(0, 3) + "..."
  require(value.length <= 64)
}

object AuthToken {
  private val TOKEN_LENGTH = 32
  private val TOKEN_CHARS =
    "0123456789ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz-._"
  private val secureRandom = new SecureRandom()

  private def generateToken(length: Int): AuthToken[Theirs] =
    AuthToken[Theirs]((1 to length).map(_ => TOKEN_CHARS(secureRandom.nextInt(TOKEN_CHARS.length))).mkString)

  def generateTheirs: AuthToken[Theirs] = generateToken(TOKEN_LENGTH)
}

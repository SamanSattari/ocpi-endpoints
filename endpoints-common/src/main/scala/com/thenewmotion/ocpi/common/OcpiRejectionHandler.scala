package com.thenewmotion.ocpi
package common

import akka.http.scaladsl.marshallers.sprayjson.SprayJsonSupport
import akka.http.scaladsl.model.StatusCodes._
import akka.http.scaladsl.server.Directives._
import akka.http.scaladsl.server._
import akka.http.scaladsl.server.directives.BasicDirectives
import msgs.ErrorResp
import msgs.OcpiStatusCode._

object OcpiRejectionHandler extends BasicDirectives with SprayJsonSupport {

  import com.thenewmotion.ocpi.msgs.v2_1.OcpiJsonProtocol._

  val Default = RejectionHandler.newBuilder().handle {

    case MalformedRequestContentRejection(msg, _) => complete {
      ( BadRequest,
        ErrorResp(
          GenericClientFailure,
          Some(msg)))
    }

    case AuthenticationFailedRejection(AuthenticationFailedRejection.CredentialsMissing, _) =>
      complete {
        ( Unauthorized,
          ErrorResp(
            MissingHeader,
            Some("Authorization Token not supplied")))
      }

    case AuthenticationFailedRejection(AuthenticationFailedRejection.CredentialsRejected, _) =>
      complete {
        ( Unauthorized,
          ErrorResp(
            AuthenticationFailed,
            Some("Invalid Authorization Token")))
      }

    case AuthorizationFailedRejection => extractUri { uri =>
      complete {
        Forbidden -> ErrorResp(
          GenericClientFailure,
          Some(s"The client is not authorized to access ${uri.toRelative}")
        )
      }
    }

    case MissingHeaderRejection(header) => complete {
      ( BadRequest,
        ErrorResp(
          MissingHeader,
          Some(s"Header not found: '$header'")))
    }
  }.handleAll[Rejection] { rejections =>
    complete {
      ( BadRequest,
        ErrorResp(
          GenericClientFailure,
          Some(rejections.mkString(", "))))
    }
  }.result()
}

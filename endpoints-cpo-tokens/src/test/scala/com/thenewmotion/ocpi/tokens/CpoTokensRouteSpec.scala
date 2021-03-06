package com.thenewmotion.ocpi
package tokens

import akka.http.scaladsl.testkit.Specs2RouteTest
import msgs.{ErrorResp, GlobalPartyId}
import org.specs2.mock.Mockito
import org.specs2.mutable.Specification
import org.specs2.specification.Scope
import scala.concurrent.Future
import scalaz._

class CpoTokensRouteSpec extends Specification with Specs2RouteTest with Mockito {

  import TokenError.TokenNotFound
  import akka.http.scaladsl.marshallers.sprayjson.SprayJsonSupport._
  import akka.http.scaladsl.model.StatusCodes
  import msgs.v2_1.OcpiJsonProtocol._
  import msgs.v2_1.Tokens._
  import org.joda.time.DateTime

  "tokens endpoint" should {
    "reject unauthorized access" in new TokensTestScope {
      val unAuthorizedUser = GlobalPartyId("NL", "SBM")

      Put(s"$tokenPath/$tokenUid") ~> akka.http.scaladsl.server.Route.seal(
        cpoTokensRoute.route(unAuthorizedUser)) ~> check {
        responseAs[ErrorResp]
        status mustEqual StatusCodes.Forbidden
      }
    }

    "accept a new token object" in new TokensTestScope {
      val token = Token(
        tokenUid,
        TokenType.Rfid,
        authId = "FA54320",
        visualNumber = Some("DF000-2001-8999"),
        issuer = "TheNewMotion",
        valid = true,
        WhitelistType.Allowed,
        language = Some("nl"),
        lastUpdated = DateTime.now
      )

      cpoTokensService.createOrUpdateToken(
        ===(apiUser),
        ===(tokenUid),
        any[Token]
      ) returns Future(\/-(true))

      def beMostlyEqualTo = (be_==(_: Token)) ^^^ ((_: Token).copy(lastUpdated = new DateTime(0)))

      Put(s"$tokenPath/$tokenUid", token) ~>
        cpoTokensRoute.route(apiUser) ~> check {
        there was one(cpoTokensService).createOrUpdateToken(
          ===(apiUser),
          ===(tokenUid),
          beMostlyEqualTo(token)
        )
        there were noCallsTo(cpoTokensService)
      }
    }

    "accept patches to a token object" in new TokensTestScope {
      val whitelistPatch = Some(WhitelistType.Always)
      val tokenPatch = TokenPatch(
        whitelist = whitelistPatch
      )

      cpoTokensService.updateToken(
        apiUser,
        tokenUid,
        tokenPatch
      ) returns Future(\/-(()))

      Patch(s"$tokenPath/$tokenUid", tokenPatch) ~>
        cpoTokensRoute.route(apiUser) ~> check {
        there was one(cpoTokensService).updateToken(
          apiUser,
          tokenUid,
          tokenPatch
        )
        there were noCallsTo(cpoTokensService)
      }
    }

    "retrieve a token object" in new TokensTestScope {
      cpoTokensService.token(
        apiUser,
        tokenUid
      ) returns Future(-\/(TokenNotFound()))

      Get(s"$tokenPath/$tokenUid") ~>
        cpoTokensRoute.route(apiUser) ~> check {
        there was one(cpoTokensService).token(
          apiUser,
          tokenUid
        )
        there were noCallsTo(cpoTokensService)
      }
    }
  }

  trait TokensTestScope extends Scope {
    val tokenUid = "012345678"
    val countryCodeString = "NL"
    val operatorIdString = "TNM"
    val apiUser = GlobalPartyId(countryCodeString, operatorIdString)
    val tokenPath = s"/$countryCodeString/$operatorIdString"
    val cpoTokensService = mock[CpoTokensService]
    val cpoTokensRoute = new CpoTokensRoute(cpoTokensService)
  }
}

package com.thenewmotion.ocpi.msgs
package v2_1

import CommonTypes.{BusinessDetails, _}
import Credentials.Creds
import Locations._
import Tokens._
import Versions._
import com.github.nscala_time.time.Imports._
import org.joda.time.format.ISODateTimeFormat
import spray.json._

trait OcpiJsonProtocol extends DefaultJsonProtocol {

  import reflect._

  private val PASS1 = """([A-Z]+)([A-Z][a-z])""".r
  private val PASS2 = """([a-z\d])([A-Z])""".r
  private val REPLACEMENT = "$1_$2"

  // Convert camelCase to snake_case
  override protected def extractFieldNames(classTag: ClassTag[_]) = {
    import java.util.Locale

    def snakify(name: String) = PASS2.replaceAllIn(PASS1.replaceAllIn(name, REPLACEMENT), REPLACEMENT).toLowerCase(Locale.US)

    super.extractFieldNames(classTag) map snakify
  }

  implicit val dateTimeOptionalMillisFormat = new JsonFormat[DateTime] {
    val formatterNoMillis = ISODateTimeFormat.dateTimeNoMillis.withZoneUTC
    def write (x: DateTime) = JsString (formatterNoMillis.print (x) )
    def read (value: JsValue) = value match {
      case JsString (x) => OcpiDatetimeParser.toOcpiDateTime(x) match {
        case Some(parsed) => parsed
        case None => deserializationError ("Expected DateTime conforming to pattern " +
          "specified in OCPI 2.1 section 14.2, but got " + x)
      }
      case x => deserializationError ("Expected DateTime as JsString, but got " + x)
    }
  }

  implicit val partyIdFmt = new JsonFormat[PartyId] {
    override def write(obj: PartyId) = JsString(obj.value)
    override def read(json: JsValue) = json match {
      case JsString(s) => PartyId(s)
      case x => deserializationError("Expected PartyId as JsString, but got " + x)
    }
  }

  implicit val countryCodeFmt = new JsonFormat[CountryCode] {
    override def write(obj: CountryCode) = JsString(obj.value)
    override def read(json: JsValue) = json match {
      case JsString(s) => CountryCode(s)
      case x => deserializationError("Expected CountryCode as JsString, but got " + x)
    }
  }

  implicit val capabilityFormat =
    new SimpleStringEnumSerializer[Capability](Capability).enumFormat

  implicit val connectorStatusFormat =
    new SimpleStringEnumSerializer[ConnectorStatus](ConnectorStatus).enumFormat

  implicit val connectorTypeFormat =
    new SimpleStringEnumSerializer[ConnectorType](ConnectorType).enumFormat

  implicit val connectorFormatFormat =
    new SimpleStringEnumSerializer[ConnectorFormat](ConnectorFormat).enumFormat

  implicit val currentTypeFormat =
    new SimpleStringEnumSerializer[PowerType](PowerType).enumFormat

  implicit val pricingUnitFormat =
    new SimpleStringEnumSerializer[PricingUnit](PricingUnit).enumFormat

  implicit val periodTypeFormat =
    new SimpleStringEnumSerializer[PeriodType](PeriodType).enumFormat

  implicit val locationTypeFormat =
    new SimpleStringEnumSerializer[LocationType](LocationType).enumFormat

  implicit val parkingRestrictionTypeFormat =
    new SimpleStringEnumSerializer[ParkingRestriction](ParkingRestriction).enumFormat

  implicit val imageCategoryTypeFormat =
    new SimpleStringEnumSerializer[ImageCategory](ImageCategory).enumFormat

  implicit val facilityTypeFormat =
    new SimpleStringEnumSerializer[Facility](Facility).enumFormat

  implicit val energySourceCategoryTypeFormat =
    new SimpleStringEnumSerializer[EnergySourceCategory](EnergySourceCategory).enumFormat

  implicit val environmentalImpactCategoryTypeFormat =
    new SimpleStringEnumSerializer[EnvironmentalImpactCategory](EnvironmentalImpactCategory).enumFormat

  implicit val tokenTypeFormat =
    new SimpleStringEnumSerializer[TokenType](TokenType).enumFormat

  implicit val whitelistTypeFormat =
    new SimpleStringEnumSerializer[WhitelistType](WhitelistType).enumFormat

  implicit val energySourceFormat = jsonFormat2(EnergySource)
  implicit val environmentalImpactFormat = jsonFormat2(EnvironmentalImpact)
  implicit val energyMixFormat = jsonFormat5(EnergyMix)
  implicit val powerFormat = jsonFormat3(Power)
  implicit val displayTestFormat = jsonFormat2(DisplayText)
  implicit val geoLocationFormat = jsonFormat2(GeoLocation)
  implicit val additionalGeoLocationFormat = jsonFormat3(AdditionalGeoLocation)
  implicit val regularHoursFormat = jsonFormat3(RegularHours)
  implicit val exceptionalPeriodFormat = jsonFormat2(ExceptionalPeriod)
  implicit val hoursFormat = jsonFormat4(Hours)
  implicit val imageFormat = jsonFormat6(Image)
  implicit val businessDetailsFormat = jsonFormat3(BusinessDetails)

  implicit val connectorFormat = jsonFormat9(Connector)
  implicit val connectorPatchFormat = jsonFormat8(ConnectorPatch)
  implicit val statusScheduleFormat = jsonFormat3(StatusSchedule)
  implicit val evseFormat = jsonFormat13(Evse)
  implicit val evsePatchFormat = jsonFormat12(EvsePatch)
  implicit val operatorFormat = jsonFormat3(Operator)
  implicit val locationFormat = jsonFormat21(Location)
  implicit val tokensFormat = jsonFormat9(Token)

  implicit val locationPatchFormat = jsonFormat21(LocationPatch)
  implicit val tokenPatchFormat = jsonFormat9(TokenPatch)

  implicit def statusCodeFormat[T <: OcpiStatusCode : ClassTag] = new JsonFormat[T] {
    override def read(json: JsValue): T = json match {
      case JsNumber(x) => OcpiStatusCode(x.toInt) match {
        case y: T => y
        case _ => deserializationError(s"StatusCode $x is not of type ${classTag[T].runtimeClass.getSimpleName}")
      }
      case x => deserializationError("Expected StatusCode as JsNumber, but got " + x)
    }
    override def write(obj: T): JsValue = JsNumber(obj.code)
  }

  implicit val endpointIdentifierFormat =
    new SimpleStringEnumSerializer[EndpointIdentifier](EndpointIdentifier).enumFormat

  implicit val versionNumberFormat = new JsonFormat[VersionNumber] {
    override def write(obj: VersionNumber) = JsString(obj.toString)

    override def read(json: JsValue) = json match {
      case JsString(s) => VersionNumber(s)
      case x => deserializationError(s"Expected VersionNumber as JsString, but got $x")
    }
  }

  def tokenFormat[T <: AuthToken : ClassTag](c: String => T) = new JsonFormat[T] {
    override def read(json: JsValue) = json match {
      case JsString(x) => c(x)
      case x => deserializationError(s"Expected ${classTag[T].runtimeClass.getSimpleName} as JsString, but got $x")
    }
    override def write(obj: T) = JsString(obj.value)
  }

  implicit val ourTokenF = tokenFormat(OurAuthToken)
  implicit val theirTokenF = tokenFormat(TheirAuthToken)

  implicit val versionFormat = jsonFormat2(Version)
  implicit val endpointFormat = jsonFormat2(Endpoint)
  implicit val versionDetailsFormat = jsonFormat2(VersionDetails)
  implicit val errorRespFormat = jsonFormat3(ErrorResp)
  implicit val successRespFormat = jsonFormat3(SuccessResp)
  implicit def successRespWithDataFormat[D : JsonFormat] = jsonFormat4(SuccessWithDataResp[D])

  implicit def credentialsFormat[T <: AuthToken : JsonFormat] = jsonFormat5(Creds[T])

  implicit val locationReferencesFormat = jsonFormat3(LocationReferences)
  implicit val allowedFormat =
    new SimpleStringEnumSerializer[Allowed](Allowed).enumFormat

  implicit val authorizationInfoFormat = jsonFormat3(AuthorizationInfo)
}

object OcpiJsonProtocol extends OcpiJsonProtocol

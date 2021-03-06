package com.thenewmotion.ochp
package client

import Converters._
import api._
import eu.ochp._1.{CdrStatusType => GenCdrStatusType, ConnectorFormat => GenConnectorFormat, ConnectorStandard => GenConnectorStandard, ConnectorType => GenConnectorType, EmtId => GenEmtId, _}
import org.joda.time.format.ISODateTimeFormat
import org.specs2.mutable.Specification
import org.specs2.specification.Scope

class ConverterSpec extends Specification {
   "Converter" should {

     " translate Token into RoamingAuthorisationInfo" >> new TestScope {
       val token1 = ChargeToken(
         contractId = "YYABCC00000003",
         emtId=EmtId(
           tokenSubType = Some(TokenSubType.withName("mifareCls")),
           tokenId = "96B0149B4EA098BE769EFDE5BD6A7403F3A25BA0"),
         printedNumber = Some("YYABCC00000003J"),
         expiryDate = DateTimeNoMillis("2014-07-14T02:00:00+02:00")
       )

       val rai = tokenToRoamingAuthorisationInfo(token1)
       rai.getExpiryDate.getDateTime mustEqual  "2014-07-14T00:00:00Z"
     }

     " translate CDRinfo into CDR" >> new TestScope {
       val emtId = new GenEmtId()
       emtId.setInstance("96B0149B4EA098BE769EFDE5BD6A7403F3A25BA0")
       emtId.setTokenType("rfid")
       emtId.setTokenSubType("mifareCls")
       emtId.setRepresentation("plain")
       val status = new GenCdrStatusType()
       status.setCdrStatusType("new")

       val startDate = new LocalDateTimeType()
       startDate.setLocalDateTime("2014-08-08T10:10:10+01:00")

       val endDate = new LocalDateTimeType()
       endDate.setLocalDateTime("2014-08-08T18:10:10+01:00")

       val cdrinfo = new CDRInfo()
       cdrinfo.setContractId("DE-LND-C00001516-E")
       cdrinfo.setEmtId(emtId)
       cdrinfo.setStatus(status)
       cdrinfo.setStartDateTime(startDate)
       cdrinfo.setEndDateTime(endDate)
       cdrinfo.setConnectorType(teslaSocketConnector)

       val cdr:CDR = cdrInfoToCdr(cdrinfo)
       cdr.contractId === "DE-LND-C00001516-E"
       cdr.emtId.tokenId === "96B0149B4EA098BE769EFDE5BD6A7403F3A25BA0"
       cdr.emtId.tokenType.toString === "rfid"
       cdr.emtId.tokenSubType.get.toString === "mifareCls"
       cdr.status.toString === "new"
       cdr.maxSocketPower === 0
       cdr.liveAuthId === None
       cdr.startDateTime === DateTimeNoMillis("2014-08-08T10:10:10+01:00")
       cdr.endDateTime === DateTimeNoMillis("2014-08-08T18:10:10+01:00")
       cdr.connectorType.connectorStandard.toString === "TESLA-R"
       cdr.connectorType.connectorFormat.toString === "Socket"
     }

     " translate CDR into CDRInfo" >> new TestScope {

       val cdr1 = CDR(
         cdrId = "123456someId123456",
         evseId = "FR*A23*E45B*78C",
         emtId = EmtId(
           tokenId = "96B0149B4EA098BE769EFDE5BD6A7403F3A25BA0",
           tokenType = TokenType.withName("rfid"),
           tokenSubType = Some(TokenSubType.withName("mifareCls"))
         ),
         contractId = "DE-LND-C00001516-E",
         liveAuthId = Some("wtf"),
         status = CdrStatus.withName("new"),
         startDateTime = DateTimeNoMillis("2014-08-08T10:10:10+01:00"),
         endDateTime = DateTimeNoMillis("2014-08-08T18:10:10+01:00"),
         duration = Some("200"),
         houseNumber = Some("585"),
         address = Some("Keizersgracht"),
         zipCode = Some("1017 DR"),
         city = Some("Amsterdam"),
         country = "NL",
         chargePointType = "AC",
         connectorType = Connector(
           connectorStandard = ConnectorStandard.`TESLA-R`,
           connectorFormat = ConnectorFormat.Socket),
         maxSocketPower = 16,
         productType = Some("wtf"),
         meterId = Some("1234"),
         chargingPeriods = List(
           CdrPeriod(
             startDateTime = DateTimeNoMillis("2014-08-08T10:10:10+01:00"),
             endDateTime = DateTimeNoMillis("2014-08-08T18:10:10+01:00"),
             billingItem = BillingItem.withName("power"),
             billingValue = 1,
             currency = "EUR",
             itemPrice = 6,
             periodCost = Some(5)
           )))

       val cdrinfo: CDRInfo = cdrToCdrInfo(cdr1)
       cdrinfo.getCdrId === "123456someId123456"
       cdrinfo.getEvseId === "FR*A23*E45B*78C"
       cdrinfo.getEmtId.getInstance === "96B0149B4EA098BE769EFDE5BD6A7403F3A25BA0"
       cdrinfo.getContractId === "DE-LND-C00001516-E"
       cdrinfo.getLiveAuthId === "wtf"
       cdrinfo.getStatus.getCdrStatusType === "new"
       val formatter = ISODateTimeFormat.dateTime()
       cdrinfo.getStartDateTime.getLocalDateTime ===
         formatter.print(formatter.parseDateTime("2014-08-08T09:10:10.000Z"))
       cdrinfo.getConnectorType.getConnectorStandard.getConnectorStandard === "TESLA-R"
       cdrinfo.getMaxSocketPower === 16
       cdrinfo.getProductType === "wtf"
       import scala.collection.JavaConverters._

       val chargePeriod: eu.ochp._1.CdrPeriodType = cdrinfo.getChargingPeriods.asScala.head
       chargePeriod.getBillingItem.getBillingItemType === "power"
       chargePeriod.getStartDateTime.getLocalDateTime ===
         formatter.print(formatter.parseDateTime("2014-08-08T09:10:10.000Z"))
       chargePeriod.getPeriodCost === 5
       chargePeriod.getItemPrice === 6
     }

     " translate ChargePointInfo into ChargePoint" >> new TestScope {

       val chargePointInfo1 = new ChargePointInfo()
       chargePointInfo1.setEvseId("DE*823*E1234*5678")
       chargePointInfo1.setLocationId("Wereld")
       chargePointInfo1.setLocationName("Keizersgracht-585")
       chargePointInfo1.setLocationNameLang("NL")
       chargePointInfo1.setAddress("Keizersgracht 585")
       chargePointInfo1.setCity("Amsterdam")
       chargePointInfo1.setZipCode("1017DR")
       chargePointInfo1.setCountry("NL")
       val loc = new GeoPointType()
       loc.setLat("52.36420822143555")
       loc.setLon("4.891792297363281")
       chargePointInfo1.setGeoLocation(loc)
       val authType = new AuthMethodType()
       authType.setAuthMethodType("RfidMifareCls")
       chargePointInfo1.getAuthMethods.add(authType)
       chargePointInfo1.getConnectors.add(teslaSocketConnector)
       val operatingTimes = new HoursType()
       val regularHours = new RegularHoursType()
       regularHours.setWeekday(1)
       regularHours.setPeriodBegin("08:00")
       regularHours.setPeriodEnd("18:00")
       operatingTimes.getRegularHours.add(regularHours)
       val faultyRegularHours = new RegularHoursType()
       faultyRegularHours.setWeekday(1)
       faultyRegularHours.setPeriodBegin("08:00")
       faultyRegularHours.setPeriodEnd("24:00") // not a valid value for joda time
       operatingTimes.getRegularHours.add(faultyRegularHours)
       chargePointInfo1.setOperatingTimes(operatingTimes)

       val chargePointOpt = cpInfoToChargePoint(chargePointInfo1)
       chargePointOpt must beSome

       val chargePoint = chargePointOpt.get
       chargePoint.evseId.value === chargePointInfo1.getEvseId
       chargePoint.locationId === chargePointInfo1.getLocationId
       chargePoint.locationName === chargePointInfo1.getLocationName
       chargePoint.locationNameLang === chargePointInfo1.getLocationNameLang
       chargePoint.address.address === chargePointInfo1.getAddress
       chargePoint.address.city === chargePointInfo1.getCity

       chargePoint.authMethods.head.toString === chargePointInfo1.getAuthMethods.get(0).getAuthMethodType
       chargePoint.connectors.head.connectorFormat.toString ===
         chargePointInfo1.getConnectors.get(0).getConnectorFormat.getConnectorFormat
       chargePoint.connectors.head.connectorStandard.toString ===
       chargePointInfo1.getConnectors.get(0).getConnectorStandard.getConnectorStandard
       chargePoint.operatingTimes.get.regularHours.size === 2

       chargePoint.operatingTimes.get.regularHours.head.weekday ===
         chargePointInfo1.getOperatingTimes.getRegularHours.get(0).getWeekday
       chargePoint.operatingTimes.get.regularHours.head.periodBegin.toString ===
         chargePointInfo1.getOperatingTimes.getRegularHours.get(0).getPeriodBegin
       chargePoint.operatingTimes.get.regularHours.head.periodEnd.toString ===
         chargePointInfo1.getOperatingTimes.getRegularHours.get(0).getPeriodEnd
     }

     " translate ChargePoint into ChargePointInfo" >> new TestScope {
       val chargePoint1 = ChargePoint(
         evseId = EvseId("DE*823*E1234*5678"),
         locationId = "WERELD",
         locationName = "Keizersgracht-585",
         locationNameLang = "NLD",
         address = CpAddress(
           address = "Keizersgracht 585",
           city = "Amsterdam",
           zipCode = "1017DR",
           country = "NLD"
         ),
         geoLocation = GeoPoint(
           lat = 52.364208,
           lon = 4.891792
         ),
         authMethods = List(AuthMethod.RfidMifareCls),
         connectors = List(Connector(ConnectorStandard.`TESLA-R`,ConnectorFormat.Socket)),
         operatingTimes = Some(Hours(
           regularHours = List(RegularHours(
             weekday = 1,
             periodBegin = TimeNoSecs("08:00"),
             periodEnd = TimeNoSecs("18:00")
           )),
           exceptionalOpenings = List(),
           exceptionalClosings = List()))
       )

       val chargePointInfo = chargePointToCpInfo(chargePoint1)
       chargePointInfo.getEvseId === chargePoint1.evseId.value
       chargePointInfo.getLocationId === chargePoint1.locationId
       chargePointInfo.getLocationName === chargePoint1.locationName
       chargePointInfo.getLocationNameLang === chargePoint1.locationNameLang
       chargePointInfo.getAddress === chargePoint1.address.address
       chargePointInfo.getZipCode === chargePoint1.address.zipCode
       chargePointInfo.getGeoLocation.getLat === chargePoint1.geoLocation.lat.toString
       chargePointInfo.getGeoLocation.getLon === chargePoint1.geoLocation.lon.toString
       chargePointInfo.getAuthMethods.get(0).getAuthMethodType ===
         chargePoint1.authMethods.head.toString
       chargePointInfo.getConnectors.get(0).getConnectorFormat.getConnectorFormat ===
         chargePoint1.connectors.head.connectorFormat.toString
       chargePointInfo.getConnectors.get(0).getConnectorStandard.getConnectorStandard ===
         chargePoint1.connectors.head.connectorStandard.toString
       chargePointInfo.getOperatingTimes.getRegularHours.get(0).getWeekday ===
         chargePoint1.operatingTimes.get.regularHours.head.weekday
       chargePointInfo.getOperatingTimes.getRegularHours.get(0).getPeriodBegin ===
         chargePoint1.operatingTimes.get.regularHours.head.periodBegin.toString
       chargePointInfo.getOperatingTimes.getRegularHours.get(0).getPeriodEnd ===
         chargePoint1.operatingTimes.get.regularHours.head.periodEnd.toString
     }

     " deal with different and empty lat/lon string" >> {
       GeoPoint("", "") must throwA[IllegalArgumentException]
       GeoPoint.fmt(-123.1234567) === "-123.123457"
       GeoPoint.fmt(123.1234567)  === "123.123457"
       GeoPoint.fmt(-0.0004567) === "-0.000457"
       GeoPoint.fmt(.0004567) === "0.000457"
     }
   }

  trait TestScope extends Scope{

    val teslaSocketConnector = new GenConnectorType()
    val connForm = new GenConnectorFormat()
    connForm.setConnectorFormat("Socket")
    val connStandard = new GenConnectorStandard()
    connStandard.setConnectorStandard("TESLA-R")

    teslaSocketConnector.setConnectorFormat(connForm)
    teslaSocketConnector.setConnectorStandard(connStandard)
  }
}



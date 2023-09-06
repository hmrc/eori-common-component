/*
 * Copyright 2023 HM Revenue & Customs
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package integration

import com.github.tomakehurst.wiremock.client.WireMock
import com.github.tomakehurst.wiremock.client.WireMock.{getRequestedFor, urlEqualTo}
import play.api.http.Status.{BAD_REQUEST, INTERNAL_SERVER_ERROR, NOT_FOUND}
import play.api.test.Helpers.OK
import uk.gov.hmrc.customs.managesubscription.connectors.{GetVatCustomerInformationConnector, ResponseError}
import uk.gov.hmrc.customs.managesubscription.domain.vat.{
  VatApprovedInformation,
  VatCustomerAddress,
  VatCustomerDetails,
  VatCustomerInformation,
  VatCustomerInformationPPOB
}
import uk.gov.hmrc.http.{BadRequestException, HeaderCarrier}
import util.IntegrationFrameworkService

class GetVatCustomerInformationConnectorIntegrationSpec
    extends IntegrationTestsWithDbSpec with IntegrationFrameworkService {

  private implicit val hc: HeaderCarrier = HeaderCarrier()
  private lazy val connector             = app.injector.instanceOf[GetVatCustomerInformationConnector]
  private val vrn                        = "123456789"
  private val url                        = s"/vat/customer/vrn/$vrn/information"

  override def beforeAll: Unit =
    startMockServer()

  override def afterAll: Unit =
    stopMockServer()

  private val successResponseBody =
    """{
      |  "approvedInformation": {
      |    "customerDetails": {
      |      "nameIsReadOnly": false,
      |      "organisationName": "Ancient Antiques",
      |      "individual": {
      |        "title": "0001",
      |        "firstName": "Fred",
      |        "middleName": "M",
      |        "lastName": "Flintstone"
      |      },
      |      "dataOrigin": "0002",
      |      "dateOfBirth": "2001-01-01",
      |      "tradingName": "a",
      |      "mandationStatus": "1",
      |      "registrationReason": "0001",
      |      "effectiveRegistrationDate": "2001-01-01",
      |      "businessStartDate": "2001-01-01",
      |      "customerMigratedToETMPDate": "2001-01-01",
      |      "welshIndicator": true,
      |      "partyType": "1",
      |      "optionToTax": true,
      |      "isPartialMigration": false,
      |      "hybridToFullMigrationDate": "2001-01-01",
      |      "isInsolvent": true,
      |      "insolvencyType": "01",
      |      "insolvencyDate": "2001-01-01",
      |      "continueToTrade": true,
      |      "overseasIndicator": false,
      |      "singleMarketIndicator": false
      |    },
      |    "PPOB": {
      |      "address": {
      |        "line1": "VAT ADDR 1",
      |        "line2": "VAT ADDR 2",
      |        "line3": "VAT ADDR 3",
      |        "line4": "VAT ADDR 4",
      |        "line5": "VAT ADDR 5",
      |        "postCode": "SW1A 2BQ",
      |        "countryCode": "ES"
      |      },
      |      "RLS": "0001",
      |      "contactDetails": {
      |        "primaryPhoneNumber": "01257162661",
      |        "mobileNumber": "07128126712",
      |        "faxNumber": "01268712671",
      |        "emailAddress": "antiques@email.com",
      |        "emailVerified": true
      |      },
      |      "websiteAddress": "https://www.gov.uk/government/organisations/hm-revenue-customs",
      |      "commsPreference": "P01"
      |    },
      |    "correspondenceContactDetails": {
      |      "address": {
      |        "line1": "VAT ADDR 1",
      |        "line2": "VAT ADDR 2",
      |        "line3": "VAT ADDR 3",
      |        "line4": "VAT ADDR 4",
      |        "line5": "VAT ADDR 5",
      |        "postCode": "SW1A 2BQ",
      |        "countryCode": "ES"
      |      },
      |      "RLS": "0001"
      |    },
      |    "bankDetails": {
      |      "IBAN": "a",
      |      "BIC": "a",
      |      "accountHolderName": "Flintstone Quarry",
      |      "bankAccountNumber": "00012345",
      |      "sortCode": "010103",
      |      "buildingSocietyNumber": "12312345",
      |      "bankBuildSocietyName": "a"
      |    },
      |    "businessActivities": {
      |      "primaryMainCode": "00000",
      |      "mainCode2": "00000",
      |      "mainCode3": "00000",
      |      "mainCode4": "00000"
      |    },
      |    "flatRateScheme": {
      |      "FRSCategory": "001",
      |      "FRSPercentage": "134",
      |      "startDate": "2001-01-01",
      |      "endDate": "2004-09-10",
      |      "limitedCostTrader": true
      |    },
      |    "deregistration": {
      |      "deregistrationReason": "0001",
      |      "effectDateOfCancellation": "2001-01-01",
      |      "lastReturnDueDate": "2001-01-01"
      |    },
      |    "returnPeriod": {
      |      "stdReturnPeriod": "MA",
      |      "nonStdTaxPeriods": [
      |        {
      |          "periodKey": "00A0",
      |          "periodStartDate": "2012-01-01",
      |          "periodEndDate": "2012-04-11",
      |          "periodDueDate": "2012-06-21"
      |        },
      |        {
      |          "periodKey": "00A1",
      |          "periodStartDate": "2013-02-01",
      |          "periodEndDate": "2013-04-01",
      |          "periodDueDate": "2013-06-21"
      |        }
      |      ],
      |      "firstNonNSTPPeriod": {
      |        "periodKeyOfFirstStandardPeriod": "19A2",
      |        "periodStartDateOfFirstStandardPeriod": "2018-04-04",
      |        "periodEndDateOfFirstStandardPeriod": "2019-04-05",
      |        "periodDueDateOfFirstStandardPeriod": "2019-01-01"
      |      }
      |    },
      |    "groupOrPartnerMbrs": [
      |      {
      |        "typeOfRelationship": "01",
      |        "nameIsReadOnly": true,
      |        "organisationName": "abcd",
      |        "individual": {
      |          "title": "0001",
      |          "firstName": "abcdefghijklmno",
      |          "middleName": "abc",
      |          "lastName": "abcdefghijk"
      |        },
      |        "SAP_Number": "012345678901234567890123456789012345678912"
      |      }
      |    ]
      |  },
      |  "inFlightInformation": {
      |    "changeIndicators": {
      |      "organisationDetails": true,
      |      "PPOBDetails": true,
      |      "correspondenceContactDetails": true,
      |      "bankDetails": true,
      |      "returnPeriod": true,
      |      "flatRateScheme": true,
      |      "businessActivities": true,
      |      "deregister": true,
      |      "effectiveDateOfRegistration": false,
      |      "mandationStatus": true,
      |      "annualAccounting": true,
      |      "commsPreference": true
      |    },
      |    "inFlightChanges": {
      |      "organisationDetails": {
      |        "formInformation": {
      |          "formBundle": "012345678912",
      |          "dateReceived": "2001-01-01"
      |        },
      |        "organisationName": "Ancient Antiques of the World",
      |        "individual": {
      |          "title": "0001",
      |          "firstName": "Paul",
      |          "middleName": "M",
      |          "lastName": "Flintstone"
      |        },
      |        "tradingName": "b"
      |      },
      |      "PPOBDetails": {
      |        "formInformation": {
      |          "formBundle": "012345678912",
      |          "dateReceived": "2001-01-01"
      |        },
      |        "address": {
      |          "line1": "VAT ADDR 1",
      |          "line2": "VAT ADDR 2",
      |          "line3": "VAT ADDR 3",
      |          "line4": "VAT ADDR 4",
      |          "line5": "VAT ADDR 5",
      |          "postCode": "SW1A 2BQ",
      |          "countryCode": "ES"
      |        },
      |        "contactDetails": {
      |          "primaryPhoneNumber": "01257162661",
      |          "mobileNumber": "07128126712",
      |          "faxNumber": "01268712671",
      |          "emailAddress": "antiques@email.com",
      |          "emailVerified": true
      |        },
      |        "websiteAddress": "https://www.gov.uk/government/organisations/hm-revenue-customs"
      |      },
      |      "correspondenceContactDetails": {
      |        "formInformation": {
      |          "formBundle": "012345678912",
      |          "dateReceived": "2001-01-01"
      |        },
      |        "address": {
      |          "line1": "VAT ADDR 1",
      |          "line2": "VAT ADDR 2",
      |          "line3": "VAT ADDR 3",
      |          "line4": "VAT ADDR 4",
      |          "line5": "VAT ADDR 5",
      |          "postCode": "SW1A 2BQ",
      |          "countryCode": "ES"
      |        }
      |      },
      |      "bankDetails": {
      |        "formInformation": {
      |          "formBundle": "012345678915",
      |          "dateReceived": "2001-01-01"
      |        },
      |        "IBAN": "a",
      |        "BIC": "a",
      |        "accountHolderName": "Flintstone Quarry",
      |        "bankAccountNumber": "00012345",
      |        "sortCode": "010103",
      |        "buildingSocietyNumber": "12312345",
      |        "bankBuildSocietyName": "a"
      |      },
      |      "returnPeriod": {
      |        "formInformation": {
      |          "formBundle": "012345678912",
      |          "dateReceived": "2001-01-01"
      |        },
      |        "changeReturnPeriod": false,
      |        "returnPeriod": "MA"
      |      },
      |      "flatRateScheme": {
      |        "formInformation": {
      |          "formBundle": "012345678915",
      |          "dateReceived": "2001-01-01"
      |        },
      |        "FRSCategory": "001",
      |        "FRSPercentage": 100,
      |        "startDate": "2001-01-01",
      |        "limitedCostTrader": true
      |      },
      |      "businessActivities": {
      |        "formInformation": {
      |          "formBundle": "012345678915",
      |          "dateReceived": "2001-01-01"
      |        },
      |        "primaryMainCode": "00000",
      |        "mainCode2": "00000",
      |        "mainCode3": "00000",
      |        "mainCode4": "00000",
      |        "dataToRecordLandProperties": false
      |      },
      |      "deregister": {
      |        "formInformation": {
      |          "formBundle": "012345678912",
      |          "dateReceived": "2001-01-01"
      |        },
      |        "deregistrationReason": "0012",
      |        "deregistrationDate": "2001-01-01",
      |        "deregistrationLaterDate": "2002-02-02",
      |        "turnoverBelowDeregistrationLimit": {
      |          "aboveBelowThreshold": true,
      |          "taxableSuppliesValue": 1000,
      |          "reason": "Deceased"
      |        },
      |        "zeroRatedExmpApplication": {
      |          "natureOfSupplies": "Nature of Supplies",
      |          "repaymentSituation": false,
      |          "zeroRatedSuppliesValue": 1010.05,
      |          "estTotalTaxTurnover": 2020.1
      |        },
      |        "newOwnerDetails": {
      |          "retainVATNumber": true,
      |          "acceptVATConditions": false,
      |          "newOwnerVRN": "012345678",
      |          "VATGroupMember": true,
      |          "stockAssetTransfer": false,
      |          "organisationName": "ABCD Ltd",
      |          "individual": {
      |            "title": "0001",
      |            "firstName": "Paul",
      |            "middleName": "M",
      |            "lastName": "Flintstone"
      |          },
      |          "tradingName": "b",
      |          "address": {
      |            "line1": "VAT ADDR 1",
      |            "line2": "VAT ADDR 2",
      |            "line3": "VAT ADDR 3",
      |            "line4": "VAT ADDR 4",
      |            "line5": "VAT ADDR 5",
      |            "postCode": "SW1A 2BQ",
      |            "countryCode": "ES"
      |          },
      |          "contactDetails": {
      |            "primaryPhoneNumber": "01257162661",
      |            "mobileNumber": "07128126712",
      |            "faxNumber": "01268712671",
      |            "emailAddress": "antiques@email.com"
      |          },
      |          "websiteAddress": "https://www.gov.uk/government/organisations/abcd-ltd"
      |        },
      |        "disbandingGroupDeregQuestions": {
      |          "groupDeregQ1": true,
      |          "groupDeregQ1Text": "a",
      |          "groupDeregQ2": true,
      |          "groupDeregQ2Text": "a",
      |          "groupDeregQ3": true,
      |          "groupDeregQ3Text": "a",
      |          "groupDeregQ4": true,
      |          "groupDeregQ4Text": "a",
      |          "groupDeregQ5": true,
      |          "groupDeregQ5Text": "a"
      |        },
      |        "joinedAFRSDeregQuestionsType": {
      |          "AFRSFarmingTurnover": 0.02,
      |          "AFRSNonFarmingTurnover": 0.02
      |        },
      |        "deregistrationDetails": {
      |          "optionTaxProperty": true,
      |          "cashAccountingScheme": false,
      |          "OTTStocksAssetsValue": 3030.15,
      |          "intendSellCapitalAssets": true,
      |          "additionalTaxInvoices": false
      |        }
      |      },
      |      "effectiveDateOfRegistration": {
      |        "formInformation": {
      |          "formBundle": "000000000000",
      |          "dateReceived": "212-01-01"
      |        },
      |        "requestedNewEDR": "2012-04-01"
      |      },
      |      "mandationStatus": {
      |        "formInformation": {
      |          "dateReceived": "2013-01-01",
      |          "formBundle": "000000000000"
      |        },
      |        "mandationStatus": "1"
      |      },
      |      "annualAccounting": {
      |        "formInformation": {
      |          "formBundle": "000000000011",
      |          "dateReceived": "2018-01-01"
      |        }
      |      },
      |      "commsPreference": {
      |        "formInformation": {
      |          "dateReceived": "2013-01-01",
      |          "formBundle": "000000000000"
      |        },
      |        "commsPreference": "ZEL"
      |      }
      |    }
      |  }
      |}""".stripMargin

  private val failedResponseBody =
    """{
                                     |  "failures": {
                                     |    "code": "INVALID_IDVALUE",
                                     |    "reason": "Submission has not passed validation. Invalid path parameter idValue."
                                     |  }
                                     |}""".stripMargin

  "GetVatCustomerInformationConnector" should {
    "call vatCustomerInformation with correct url successfully" in {
      scala.concurrent.Await.ready(connector.getVatCustomerInformation(vrn), defaultTimeout)
      WireMock.verify(getRequestedFor(urlEqualTo(url)))
    }

    "return successful VatCustomerInformation with OK status and response body" in {
      returnGetVatCustomerInformationResponse(url, OK, successResponseBody)
      val format = new java.text.SimpleDateFormat("yyyy-MM-dd")
      val expected = Right(
        VatCustomerInformation(
          VatApprovedInformation(
            VatCustomerDetails(Some(format.parse("2001-01-01"))),
            VatCustomerInformationPPOB(Some(VatCustomerAddress(Some("SW1A 2BQ"))))
          )
        )
      )
      val result: Either[ResponseError, VatCustomerInformation] = await(connector.getVatCustomerInformation(vrn).value)

      result shouldBe expected
    }

    "return failed VatCustomerInformation INTERNAL_SERVER_ERROR status and ResponseError for failed js conversion" in {
      returnGetVatCustomerInformationResponse(url, OK, failedResponseBody)
      val expected = Left(
        ResponseError(
          INTERNAL_SERVER_ERROR,
          """Invalid JSON returned: {
                                                                 |  "failures": {
                                                                 |    "code": "INVALID_IDVALUE",
                                                                 |    "reason": "Submission has not passed validation. Invalid path parameter idValue."
                                                                 |  }
                                                                 |}""".stripMargin
        )
      )
      val result: Either[ResponseError, VatCustomerInformation] = await(connector.getVatCustomerInformation(vrn).value)

      result shouldBe expected
    }

    "return failed VatCustomerInformation call with 404 status and ResponseError" in {
      returnGetVatCustomerInformationResponse(url, BAD_REQUEST, "some other failure")
      val result = intercept[BadRequestException](await(connector.getVatCustomerInformation(vrn).value))
      result.getMessage shouldBe "GET of 'http://localhost:11111/vat/customer/vrn/123456789/information' returned 400 (Bad Request). Response body 'some other failure'"
    }

  }
}

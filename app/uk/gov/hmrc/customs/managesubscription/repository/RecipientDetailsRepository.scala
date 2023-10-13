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

package uk.gov.hmrc.customs.managesubscription.repository

import play.api.Logger
import play.api.libs.json._
import uk.gov.hmrc.customs.managesubscription.domain.protocol.Eori
import uk.gov.hmrc.customs.managesubscription.domain.{RecipientDetails, RecipientDetailsWithEori}
import uk.gov.hmrc.mongo.cache.{CacheIdType, CacheItem, DataKey, MongoCacheRepository}
import uk.gov.hmrc.mongo.{MongoComponent, TimestampSupport}
import uk.gov.hmrc.play.bootstrap.config.ServicesConfig
import uk.gov.hmrc.play.http.logging.Mdc.preservingMdc

import javax.inject.{Inject, Singleton}
import scala.concurrent.{ExecutionContext, Future}

@Singleton
class RecipientDetailsRepository @Inject() (cacheRepo: RecipientDetailsCacheRepository)(implicit ec: ExecutionContext) {

  private val logger = Logger(this.getClass)

  private val recipientDetailsKey = "recipientDetailsWithEori"

  def saveRecipientDetailsForBundleId(
    formBundleId: String,
    eori: Option[Eori],
    recipientDetails: RecipientDetails,
    emailVerificationTimestamp: String,
    safeId: String
  )(implicit writes: Writes[RecipientDetailsWithEori]): Future[Unit] =
    preservingMdc {
      cacheRepo.put(formBundleId)(
        DataKey(recipientDetailsKey),
        Json.toJson(RecipientDetailsWithEori(eori.map(_.value), recipientDetails, emailVerificationTimestamp, safeId))
      ).map(_ => (): Unit)
    }

  def recipientDetailsForBundleId(formBundleId: String)(implicit
    reads: Reads[RecipientDetailsWithEori]
  ): Future[Either[JsError, RecipientDetailsWithEori]] = getCached(formBundleId)

  private def getCached(
    formBundleId: String
  )(implicit reads: Reads[RecipientDetailsWithEori]): Future[Either[JsError, RecipientDetailsWithEori]] =
    preservingMdc {
      cacheRepo.findById(formBundleId).map {
        case Some(CacheItem(_, data, _, _)) =>
          (data \ recipientDetailsKey).validate[RecipientDetailsWithEori] match {
            case d: JsSuccess[RecipientDetailsWithEori] =>
              Right(d.value)
            case _: JsError =>
              logger.error(s"Data saved in db is invalid for formBundleId: ${formBundleId}")
              Left(JsError(s"Data saved in db is invalid for formBundleId: ${formBundleId}"))
          }
        case _ =>
          logger.error(s"No data is saved for the formBundleId: ${formBundleId}")
          Left(JsError(s"No data is saved for the formBundleId: ${formBundleId}"))
      }
    }

}

@Singleton
class RecipientDetailsCacheRepository @Inject() (
  sc: ServicesConfig,
  mongoComponent: MongoComponent,
  timestampSupport: TimestampSupport
)(implicit ec: ExecutionContext)
    extends MongoCacheRepository(
      mongoComponent = mongoComponent,
      collectionName = "recipient-details",
      ttl = sc.getDuration("cache.expiryInMinutes"),
      timestampSupport = timestampSupport,
      cacheIdType = CacheIdType.SimpleCacheId
    )(ec)

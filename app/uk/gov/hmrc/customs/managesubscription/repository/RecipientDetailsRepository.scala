/*
 * Copyright 2020 HM Revenue & Customs
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

import javax.inject.{Inject, Singleton}
import play.api.libs.json._
import play.modules.reactivemongo.ReactiveMongoComponent
import uk.gov.hmrc.cache.model.{Cache, Id}
import uk.gov.hmrc.cache.repository.CacheMongoRepository
import uk.gov.hmrc.customs.managesubscription.CdsLogger.logger
import uk.gov.hmrc.customs.managesubscription.domain.protocol.Eori
import uk.gov.hmrc.customs.managesubscription.domain.{RecipientDetails, RecipientDetailsWithEori}
import uk.gov.hmrc.play.bootstrap.config.ServicesConfig

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.{ExecutionContext, Future}

@Singleton
class RecipientDetailsRepository @Inject()(cacheRepo: RecipientDetailsCacheRepository) {

  private val recipientDetailsKey = "recipientDetailsWithEori"

  def saveRecipientDetailsForBundleId(
    formBundleId: String,
    eori: Option[Eori],
    recipientDetails: RecipientDetails,
    emailVerificationTimestamp: String,
    safeId: String
  )(implicit writes: Writes[RecipientDetailsWithEori]): Future[Unit] =
    cacheRepo.createOrUpdate(
      Id(formBundleId),
      recipientDetailsKey,
      Json.toJson(
        RecipientDetailsWithEori(
          eori.map(_.value),
          recipientDetails,
          emailVerificationTimestamp,
          safeId
        )
      )
    ).map(_ => (): Unit)

  def recipientDetailsForBundleId(formBundleId: String)(
    implicit reads: Reads[RecipientDetailsWithEori]
  ): Future[Either[JsError, RecipientDetailsWithEori]] = getCached(formBundleId)

  private def getCached(formBundleId: Id)(implicit reads: Reads[RecipientDetailsWithEori]): Future[Either[JsError, RecipientDetailsWithEori]] =
    cacheRepo.findById(formBundleId.id).map {
      case Some(Cache(_, Some(data), _, _)) => (data \ recipientDetailsKey).validate[RecipientDetailsWithEori] match {
        case d: JsSuccess[RecipientDetailsWithEori] =>
          Right(d.value)
        case _: JsError =>
          logger.error(s"Data saved in db is invalid for formBundleId: ${formBundleId.id}")
          Left(JsError(s"Data saved in db is invalid for formBundleId: ${formBundleId.id}"))
      }
      case _ => {
        logger.error(s"No data is saved for the formBundleId: ${formBundleId.id}")
        Left(JsError(s"No data is saved for the formBundleId: ${formBundleId.id}"))
      }
    }
}

@Singleton
class RecipientDetailsCacheRepository @Inject()(sc: ServicesConfig, mongo: ReactiveMongoComponent)(implicit ec: ExecutionContext)
  extends CacheMongoRepository("recipient-details", sc.getDuration("cache.expiryInMinutes").toSeconds)(mongo.mongoConnector.db, ec)
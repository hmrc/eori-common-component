/*
 * Copyright 2025 HM Revenue & Customs
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
import uk.gov.hmrc.mongo.cache.{CacheIdType, CacheItem, DataKey, MongoCacheRepository}
import uk.gov.hmrc.mongo.{MongoComponent, TimestampSupport}
import uk.gov.hmrc.play.bootstrap.config.ServicesConfig
import uk.gov.hmrc.play.http.logging.Mdc.preservingMdc

import javax.inject.{Inject, Singleton}
import scala.concurrent.{ExecutionContext, Future}

@Singleton
class Save4LaterRepository @Inject() (
  sc: ServicesConfig,
  mongoComponent: MongoComponent,
  timestampSupport: TimestampSupport
)(implicit ec: ExecutionContext)
    extends MongoCacheRepository(
      mongoComponent = mongoComponent,
      collectionName = "save4later",
      ttl = sc.getDuration("cache.expiryInMinutes"),
      timestampSupport = timestampSupport,
      cacheIdType = CacheIdType.SimpleCacheId
    )(ec) {
  private val logger = Logger(this.getClass)

  def putData[A: Writes](id: String, key: String, data: A): Future[A] =
    preservingMdc {
      put[A](id)(DataKey(key), data).map(_ => data)
    }

  def save(id: String, key: String, jsValue: JsValue): Future[Unit] =
    putData(id, key, jsValue).map(_ => (): Unit)

  def findByIdAndKey(id: String, key: String): Future[Option[JsValue]] =
    preservingMdc {
      findById(id).map {
        case Some(CacheItem(_, data, _, _)) =>
          (data \ key) match {
            case js: JsDefined =>
              Some(js.value)
            case js: JsUndefined =>
              logger.error(s"Key Not found : $key")
              logger.debug(s"Key Not found : $key \n details : ${js.error}")
              None
          }
        case _ =>
          logger.error(s"Id Not found: $id")
          None
      }
    }

  def remove(id: String): Future[Boolean] =
    preservingMdc {
      deleteEntity(id).map(_ => true).recoverWith {
        case _ => Future.successful(false)
      }
    }

  def removeKeyById(id: String, key: String): Future[Boolean] =
    preservingMdc {
      findByIdAndKey(id, key).flatMap {
        case Some(_) =>
          delete(id)(DataKey(key)).map(_ => true).recoverWith {
            case _ => Future.successful(false)
          }
        case _ => Future.successful(false)
      }
    }

}

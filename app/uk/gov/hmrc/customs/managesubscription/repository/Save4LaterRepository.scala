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
import uk.gov.hmrc.cache.model.{Cache, Id => CacheId}
import uk.gov.hmrc.cache.repository.CacheMongoRepository
import uk.gov.hmrc.play.bootstrap.config.ServicesConfig

import scala.concurrent.{ExecutionContext, Future}

@Singleton
class Save4LaterRepository @Inject() (sc: ServicesConfig, mongo: ReactiveMongoComponent)(implicit ec: ExecutionContext)
    extends CacheMongoRepository("save4later", sc.getDuration("cache.expiryInMinutes").toSeconds)(
      mongo.mongoConnector.db,
      ec
    ) {

  def save(id: String, key: String, jsValue: JsValue): Future[Unit] =
    createOrUpdate(CacheId(id), key, jsValue).map(_ => (): Unit)

  def findByIdAndKey(id: String, key: String): Future[Option[JsValue]] =
    findById(CacheId(id)).map {
      case Some(Cache(_, Some(data), _, _)) =>
        (data \ key) match {
          case js: JsDefined => Some(js.value)
          case js: JsUndefined =>
            logger.error(s"Key Not found : $key")
            logger.debug(s"Key Not found : $key \n details : ${js.error}")
            None
        }
      case _ =>
        logger.error(s"Id Not found: $id")
        None
    }

  def remove(id: String): Future[Boolean] = removeById(CacheId(id)).map(_.ok)

  def removeKeyById(id: String, key: String): Future[Boolean] = {
    val selector = Json.obj(Id -> id)
    findById(CacheId(id)).flatMap {
      case Some(cache) =>
        cache.data.fold(Future.successful(false)) { data =>
          (data \ key) match {
            case _: JsDefined =>
              val updateData   = data.as[JsObject] - key
              val cacheUpdated = Json.toJson(cache.copy(data = Some(updateData)))
              findAndUpdate(selector, cacheUpdated.as[JsObject], fetchNewObject = true, upsert = true).map {
                updateResult =>
                  if (updateResult.value.isEmpty) {
                    updateResult.lastError.foreach(
                      _.err.foreach(errorMsg => logger.error(s"Problem during database update: $errorMsg"))
                    )
                    false
                  } else
                    true
              }
            case _: JsUndefined =>
              logger.warn(s"Key not found: $key")
              Future.successful(false)
          }
        }
      case _ => Future.successful(false)
    }
  }

}

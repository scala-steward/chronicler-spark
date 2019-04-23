/*
 * Copyright 2018-2019 Faiaz Sanaulla
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

package com.github.fsanaulla.chronicler.spark

import com.github.fsanaulla.chronicler.core.enums.{Consistency, Precision}
import com.github.fsanaulla.chronicler.core.model.{InfluxWriter, WriteResult}
import com.github.fsanaulla.chronicler.urlhttp.io.InfluxIO
import com.github.fsanaulla.chronicler.urlhttp.shared.InfluxConfig
import org.apache.spark.rdd.RDD

import scala.reflect.ClassTag
import scala.util.{Failure, Success}

package object rdd {

  /**
    * Extension that will provide static methods for saving RDDs to InfluxDB
    *
    * @param rdd - [[org.apache.spark.rdd.RDD]]
    * @tparam T - inner type
    */
  implicit final class RddOps[T](private val rdd: RDD[T]) extends AnyVal {

    /**
      * Write [[org.apache.spark.rdd.RDD]] to InfluxDB
      *
      * @param dbName          - database name
      * @param measName        - measurement name
      * @param batchSize       - batch size
      * @param onFailure       - function to handle failed cases
      * @param onSuccess       - function to handle success case
      * @param consistency     - consistence level
      * @param precision       - time precision
      * @param retentionPolicy - retention policy type
      * @param wr              - implicit [[InfluxWriter]]
      */
    def saveToInfluxDB(dbName: String,
                       measName: String,
                       batchSize: Int = 2500,
                       onFailure: Throwable => Unit = _ => (),
                       onSuccess: WriteResult => Unit = _ => (),
                       consistency: Option[Consistency] = None,
                       precision: Option[Precision] = None,
                       retentionPolicy: Option[String] = None)
                      (implicit wr: InfluxWriter[T], conf: InfluxConfig, tt: ClassTag[T]): Unit = {
      rdd.foreachPartition { partition =>
        val client = InfluxIO(conf)
        val meas = client.measurement[T](dbName, measName)

        partition.sliding(batchSize, batchSize).foreach { batch =>
          meas.bulkWrite(batch, consistency, precision, retentionPolicy) match {
            case Success(result) => onSuccess(result)
            case Failure(ex)     => onFailure(ex)
          }
        }

        client.close()
      }
    }
  }
}
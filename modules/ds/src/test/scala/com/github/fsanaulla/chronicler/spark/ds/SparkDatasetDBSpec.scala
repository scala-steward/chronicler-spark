/*
 * Copyright 2018-2021 Faiaz Sanaulla
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

package com.github.fsanaulla.chronicler.spark.ds

import com.github.fsanaulla.chronicler.core.alias.ErrorOr
import com.github.fsanaulla.chronicler.core.auth.{InfluxCredentials}
import com.github.fsanaulla.chronicler.core.model.{InfluxWriter}
import com.github.fsanaulla.chronicler.spark.testing.{DockerizedInfluxDB, Entity, SparkSessionBase}
import com.github.fsanaulla.chronicler.sync.io.{InfluxIO}
import com.github.fsanaulla.chronicler.sync.management.{InfluxMng}
import com.github.fsanaulla.chronicler.sync.shared.InfluxConfig
import com.github.fsanaulla.chronicler.spark.core.CallbackHandler
import org.apache.spark.SparkConf
import org.apache.spark.sql.SparkSession
import org.scalatest.concurrent.{Eventually, IntegrationPatience}
import org.scalatest.{TryValues, EitherValues}
import SparkDatasetDBSpec._

class SparkDatasetDBSpec
    extends SparkSessionBase
    with Eventually
    with DockerizedInfluxDB
    with TryValues
    with EitherValues {

  override def afterAll(): Unit = {
    mng.close()
    io.close()

    super.afterAll()
  }

  val dbName = "db"

  implicit lazy val influxConf: InfluxConfig =
    InfluxConfig(host, port, Some(InfluxCredentials.Basic("admin", "password")))

  lazy val mng = InfluxMng(influxConf)
  lazy val io  = InfluxIO(influxConf)

  import spark.implicits._

  "Influx" - {
    "create database" in {
      mng.createDatabase(dbName).success.value.value mustEqual 200
    }

    "store data in database" - {
      val ch = new CallbackHandler(
        onSuccess = _ => (),
        onApplicationFailure = ex => println(ex),
        onNetworkFailure = ex => println(ex)
      )

      "write" in {
        Entity
          .samples()
          .toDS()
          .saveToInfluxDB(dbName, ch = Some(ch))
          .mustEqual {}
      }

      "check" in {
        eventually {
          io.database(dbName)
            .readJson("SELECT * FROM meas")
            .success
            .value
            .value
            .length mustEqual 20
        }
      }
    }
  }
}

object SparkDatasetDBSpec {
  implicit val wr: InfluxWriter[Entity] = new InfluxWriter[Entity] {
    override def write(obj: Entity): ErrorOr[String] =
      Right("meas,name=\"" + obj.name + "\" surname=\"" + obj.surname + "\"")

  }
}

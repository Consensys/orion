/*
 * Copyright 2018 ConsenSys AG.
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except in compliance with
 * the License. You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software distributed under the License is distributed on
 * an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the License for the
 * specific language governing permissions and limitations under the License.
 */
package net.consensys.orion.load

import java.util.Base64
import collection.JavaConverters._

import io.gatling.core.Predef._
import io.gatling.core.structure._
import io.gatling.http.Predef._
import com.fasterxml.jackson.databind.ObjectMapper
import scala.concurrent.duration._

class LoadSimulation extends Simulation {

  val mapper = new ObjectMapper()

  val foo = "zO9rl50icfFDnI5MAQzRgmrQIY8mgYSWEVBKbym2Hho="

  val bar = "k2zXEin4Ip/qBGlRkJejnGWdP9cjkK+DAvKNW31L2C8="

  val foobar = "PCbwySqYvCAC5GpjDaJpiKuPp5qvCX0ADPXcHI47yx0="

  val upCheck = scenario("UpCheck")
    .exec(http("upcheck")
    .get("/upcheck"))
    .pause(5)

  val base64payload = new String(Base64.getEncoder.encode("somebyteshere".getBytes))

  def buildSendRequests(host: String, from : String, to : Seq[String]): ChainBuilder = {
    return exec(http("sends")
    .post(s"$host/send")
    .body(StringBody(mapper.writeValueAsString(Map("payload" -> base64payload, "from" -> from, "to" -> to.toArray).asJava)))
    .asJSON.check(jsonPath("$.key").saveAs("sendResponse")))
  }

  def buildReceiveRequests(host: String, to : String): ChainBuilder = {
    return exec(http("receives")
      .post(s"$host/receive")
      .body(StringBody("""{"key":"${sendResponse}","to":"""" + to + "\"}"))
      .asJSON.check(jsonPath("$.payload").is(base64payload)))
  }

  val fooToFooRequests = buildSendRequests("http://localhost:8081", foo, Seq(foo))
  val receiveFooRequests = buildReceiveRequests("http://localhost:8081", foo)

  val fooToBarRequests = buildSendRequests("http://localhost:8081", foo, Seq(bar))

  val fooToBarAndFoobarRequests = buildSendRequests("http://localhost:8081", foo, Seq(bar, foobar))
  val receiveBarRequests = buildReceiveRequests("http://localhost:8083", bar)
  val receiveFooBarRequests = buildReceiveRequests("http://localhost:8085", foobar)


  setUp(
    upCheck.inject(atOnceUsers(10)),
    scenario("SendThenReceiveOneNode").exec(fooToFooRequests).exec(receiveFooRequests).inject(rampUsers(1000) over (2 minutes)),
    scenario("SendThenReceiveOtherNode").exec(fooToBarRequests).exec(receiveBarRequests).inject(rampUsers(1000) over (2 minutes)),
    scenario("SendThenReceiveTwoOtherNodes").exec(fooToBarAndFoobarRequests).exec(receiveBarRequests).
      exec(receiveFooBarRequests).inject(atOnceUsers(10), rampUsers(20000) over (2 minutes))
  )
}
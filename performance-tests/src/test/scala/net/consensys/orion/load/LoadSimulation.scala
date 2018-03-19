package net.consensys.orion.load

import java.util.Base64

import io.gatling.core.Predef._
import io.gatling.http.Predef._

class LoadSimulation extends Simulation {   

  val httpConf = http   
    .baseURL("http://localhost:8081")
    .acceptEncodingHeader("gzip, deflate")

  val upCheck = scenario("UpCheck")
    .exec(http("upcheck")
    .get("/upcheck"))   
    .pause(5)

  val base64payload = new String(Base64.getEncoder.encode("somebyteshere".getBytes))

  val sendRequests = exec(http("sends")
    .post("/send")
      .body(StringBody(s"""{"from":"zO9rl50icfFDnI5MAQzRgmrQIY8mgYSWEVBKbym2Hho=","to":["zO9rl50icfFDnI5MAQzRgmrQIY8mgYSWEVBKbym2Hho="],"payload":"$base64payload"}"""))
      .asJSON.check(jsonPath("$.key").saveAs("sendResponse")))

  val receiveRequests = exec(http("receives")
      .post("/receive")
      .body(StringBody("""{"key":"${sendResponse}","to":"zO9rl50icfFDnI5MAQzRgmrQIY8mgYSWEVBKbym2Hho="}"""))
      .asJSON.check(jsonPath("$.payload").is(base64payload))
    )

  setUp(
    upCheck.inject(atOnceUsers(10)),
    scenario("SendThenReceive").exec(sendRequests).exec(receiveRequests).inject(atOnceUsers(1000))
  ).protocols(httpConf)
}
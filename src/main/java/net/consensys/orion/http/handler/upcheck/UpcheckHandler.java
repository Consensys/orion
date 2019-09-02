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
package net.consensys.orion.http.handler.upcheck;

import io.vertx.core.Handler;
import io.vertx.ext.web.RoutingContext;

/**
 * Simple upcheck/hello check to see if the server is up and running. Returns a 200 response with the body "I'm up!"
 */
public class UpcheckHandler implements Handler<RoutingContext> {

  @Override
  public void handle(final RoutingContext routingContext) {
    routingContext.response().end("I'm up!");
  }
}

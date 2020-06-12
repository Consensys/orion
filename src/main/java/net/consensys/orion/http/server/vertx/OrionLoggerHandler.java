/*
 * Copyright 2020 ConsenSys AG.
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
package net.consensys.orion.http.server.vertx;

import io.vertx.core.logging.Logger;
import io.vertx.core.logging.LoggerFactory;
import io.vertx.ext.web.handler.LoggerFormat;

public class OrionLoggerHandler extends io.vertx.ext.web.handler.impl.LoggerHandlerImpl {
  private final Logger logger;

  public OrionLoggerHandler() {
    super(LoggerFormat.DEFAULT);
    this.logger = LoggerFactory.getLogger(this.getClass());
  }

  @Override
  protected void doLog(final int status, final String message) {
    if (status >= 500) {
      this.logger.error(message);
    } else if (status >= 400) {
      // To reduce noise 404s are only logged as debug. A 404 is returned for the common use case where Besu
      // receives a private marker transaction for a privacy group that this Orion is not part of.
      if (status == 404) {
        this.logger.debug(message);
      } else {
        this.logger.warn(message);
      }
    } else {
      this.logger.info(message);
    }

  }
}

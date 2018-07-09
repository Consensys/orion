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

package net.consensys.orion.impl.enclave.sodium.serialization;

import net.consensys.cava.crypto.sodium.Box;
import net.consensys.orion.impl.utils.Base64;

import java.io.IOException;

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.JsonDeserializer;

public final class PublicKeyDeserializer extends JsonDeserializer<Box.PublicKey> {

  @Override
  public Box.PublicKey deserialize(JsonParser p, DeserializationContext ctxt) throws IOException {
    return Box.PublicKey.fromBytes(Base64.decode(p.getValueAsString()));
  }
}

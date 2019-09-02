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
package net.consensys.orion.enclave.sodium.serialization;

import net.consensys.orion.enclave.sodium.StoredPrivateKey;

import java.io.IOException;

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.databind.JsonSerializer;
import com.fasterxml.jackson.databind.SerializerProvider;

public final class StoredPrivateKeySerializer extends JsonSerializer<StoredPrivateKey> {

  @Override
  public void serialize(final StoredPrivateKey key, final JsonGenerator gen, final SerializerProvider serializers)
      throws IOException {
    gen.writeStartObject();
    gen.writeFieldName("data");
    gen.writeStartObject();
    gen.writeFieldName("bytes");
    gen.writeString(key.encoded());
    gen.writeEndObject();
    gen.writeFieldName("type");
    gen.writeString(key.type());
    gen.writeEndObject();
  }
}

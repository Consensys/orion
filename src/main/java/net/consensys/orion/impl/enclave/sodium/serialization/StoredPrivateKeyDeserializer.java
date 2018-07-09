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

import net.consensys.orion.api.enclave.EnclaveException;
import net.consensys.orion.api.exception.OrionErrorCode;
import net.consensys.orion.impl.enclave.sodium.StoredPrivateKey;

import java.io.IOException;

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.deser.std.StdDeserializer;

public final class StoredPrivateKeyDeserializer extends StdDeserializer<StoredPrivateKey> {

  public StoredPrivateKeyDeserializer() {
    this(null);
  }

  public StoredPrivateKeyDeserializer(Class<?> vc) {
    super(vc);
  }

  @Override
  public StoredPrivateKey deserialize(JsonParser p, DeserializationContext ctxt) throws IOException {
    JsonNode rootNode = p.getCodec().readTree(p);
    JsonNode typeNode = rootNode.get("type");
    if (typeNode == null) {
      throw new EnclaveException(
          OrionErrorCode.ENCLAVE_UNSUPPORTED_PRIVATE_KEY_TYPE,
          "Unknown stored key format (missing 'type')");
    }
    String type = typeNode.textValue();
    JsonNode dataNode = rootNode.get("data");
    if (dataNode == null) {
      throw new EnclaveException(
          OrionErrorCode.ENCLAVE_INVALID_PRIVATE_KEY,
          "Invalid stored key format (missing 'data')");
    }
    JsonNode bytesNode = dataNode.get("bytes");
    if (bytesNode == null) {
      throw new EnclaveException(
          OrionErrorCode.ENCLAVE_INVALID_PRIVATE_KEY,
          "Invalid stored key format (missing 'data.bytes')");
    }
    String encoded = bytesNode.textValue();
    return new StoredPrivateKey(encoded, type);
  }
}

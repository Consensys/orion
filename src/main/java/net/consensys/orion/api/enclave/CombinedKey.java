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

package net.consensys.orion.api.enclave;

/**
 * A combined key for encryption. This interface contains no methods or constants. It merely serves to group (and
 * provide type safety for) the combined key interface.
 *
 * <p>
 * This takes inspiration from the java.security.PrivateKey/java.security.Key interface family.
 *
 * <p>
 * Note: specialized combined key interfaces will extend this interface.
 *
 * @see java.security.Key
 * @see java.security.PrivateKey
 */
public interface CombinedKey {
  byte[] getEncoded();
}

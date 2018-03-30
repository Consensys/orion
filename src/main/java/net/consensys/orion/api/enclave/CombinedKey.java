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

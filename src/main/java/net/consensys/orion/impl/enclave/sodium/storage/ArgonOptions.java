package net.consensys.orion.impl.enclave.sodium.storage;

import java.util.Optional;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * Encapsulates the options to use with Argon encryption.
 *
 * <p>
 * If we are using the lib sodium implementation, use: opsLimit and memLimit
 *
 * <p>
 * At the end of the day, this is an internal class, that is currently tightly coupled to the SodiumFileKeyStore. It's
 * just a tool to help with the json serialisation/deserialisation.
 *
 * <p>
 * Memory, Iterations and Parallelism are used by the native lib argon implementation.
 */
public class ArgonOptions {

  public static final long OPS_LIMIT_MODERATE = 3L;
  public static final long MEM_LIMIT_MODERATE = 268435456L;
  public static final String VERSION = "1.3";

  private final String variant;
  private final String version;
  private final Optional<Long> memory;
  private final Optional<Integer> iterations;
  private final Optional<Integer> parallelism;
  private final Optional<Long> opsLimit;
  private final Optional<Long> memLimit;

  @JsonCreator
  public ArgonOptions(
      @JsonProperty("variant") String variant,
      @JsonProperty("version") String version,
      @JsonProperty("memory") Optional<Long> memory,
      @JsonProperty("iterations") Optional<Integer> iterations,
      @JsonProperty("parallelism") Optional<Integer> parallelism,
      @JsonProperty("opsLimit") Optional<Long> opsLimit,
      @JsonProperty("memLimit") Optional<Long> memLimit) {
    this.variant = variant;
    this.memory = memory;
    this.iterations = iterations;
    this.parallelism = parallelism;
    this.version = version;
    this.opsLimit = opsLimit;
    this.memLimit = memLimit;
  }

  /**
   * Argon variant to use. One of i, id
   *
   * @return the variant being used
   */
  @JsonProperty("variant")
  public String variant() {
    return variant;
  }

  /** @return version of argon2. */
  @JsonProperty("version")
  public String version() {
    return version;
  }

  /** @return amount of memory to use. */
  @JsonProperty("memory")
  public Optional<Long> memory() {
    return memory;
  }

  /** @return Number of iterations */
  @JsonProperty("iterations")
  public Optional<Integer> iterations() {
    return iterations;
  }

  /** @return The amount of parrallisation. */
  @JsonProperty("parallelism")
  public Optional<Integer> parallelism() {
    return parallelism;
  }

  /** @return Operation limit */
  @JsonProperty("opsLimit")
  public Optional<Long> opsLimit() {
    return opsLimit;
  }

  /** @return Memory limit */
  @JsonProperty("memLimit")
  public Optional<Long> memLimit() {
    return memLimit;
  }
}

package net.consensys.athena.impl.enclave.sodium.storage;

import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * Encapsulates the options to use with Argon encryption.
 *
 * <p>If we are using the lib sodium implementation, use: opsLimit and memLimit
 *
 * <p>At the end of the day, this is an internal class, that is currently tightly coupled to the
 * SodiumFileKeyStore. It's just a tool to help with the json serialisation/deserialisation.
 *
 * <p>Memory, Iterations and Parallelism are used by the native lib argon implementation.
 */
public class ArgonOptions {

  public static final int OPS_LIMIT_MODERATE = 3;
  public static final int MEM_LIMIT_MODERATE = 268435456;
  public static final String VERSION = "1.3";

  private String variant;
  private Long memory;
  private Integer iterations;
  private Integer parallelism;
  private String version;
  private Long opsLimit;
  private Long memLimit;

  /**
   * Argon variant to use. One of i, id
   *
   * @return the variant being used
   */
  @JsonProperty("variant")
  public String variant() {
    return variant;
  }

  @JsonProperty("variant")
  public void variant(String variant) {
    this.variant = variant;
  }

  /** @return amount of memory to use. */
  @JsonProperty("memory")
  public Long memory() {
    return memory;
  }

  @JsonProperty("memory")
  public void memory(long memory) {
    this.memory = memory;
  }

  /** @return Number of iterations */
  @JsonProperty("iterations")
  public Integer iterations() {
    return iterations;
  }

  @JsonProperty("iterations")
  public void iterations(int iterations) {
    this.iterations = iterations;
  }

  /** @return The amount of parrallisation. */
  @JsonProperty("parallelism")
  public Integer parallelism() {
    return parallelism;
  }

  @JsonProperty("parallelism")
  public void parallelism(int parallelism) {
    this.parallelism = parallelism;
  }

  /** @return version of argon2. */
  @JsonProperty("version")
  public String version() {
    return version;
  }

  @JsonProperty("version")
  public void version(String version) {
    this.version = version;
  }

  /** @return Operation limit */
  @JsonProperty("opsLimit")
  public Long opsLimit() {
    return opsLimit;
  }

  @JsonProperty("opsLimit")
  public void opsLimit(long opsLimit) {
    this.opsLimit = opsLimit;
  }

  /** @return Memory limit */
  @JsonProperty("memLimit")
  public Long memLimit() {
    return memLimit;
  }

  @JsonProperty("memLimit")
  public void memLimit(long memLimit) {
    this.memLimit = memLimit;
  }
}

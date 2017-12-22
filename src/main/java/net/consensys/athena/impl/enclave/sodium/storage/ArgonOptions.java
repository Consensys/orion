package net.consensys.athena.impl.enclave.sodium.storage;

/**
 * Encapsulates the options to use with Argon encryption.
 *
 * <p>If we are using the lib sodium implementation, use: getOpsLimit and getMemLimit
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
  public String getVariant() {
    return variant;
  }

  public void setVariant(String variant) {
    this.variant = variant;
  }

  /** @return amount of memory to use. */
  public Long getMemory() {
    return memory;
  }

  public void setMemory(long memory) {
    this.memory = memory;
  }

  /** @return Number of iterations */
  public Integer getIterations() {
    return iterations;
  }

  public void setIterations(int iterations) {
    this.iterations = iterations;
  }

  /** @return The amount of parrallisation. */
  public Integer getParallelism() {
    return parallelism;
  }

  public void setParallelism(int parallelism) {
    this.parallelism = parallelism;
  }

  /** @return version of argon2. */
  public String getVersion() {
    return version;
  }

  public void setVersion(String version) {
    this.version = version;
  }

  /** @return Operation limit */
  public Long getOpsLimit() {
    return opsLimit;
  }

  public void setOpsLimit(long opsLimit) {
    this.opsLimit = opsLimit;
  }

  /** @return Memory limit */
  public Long getMemLimit() {
    return memLimit;
  }

  public void setMemLimit(long memLimit) {
    this.memLimit = memLimit;
  }
}

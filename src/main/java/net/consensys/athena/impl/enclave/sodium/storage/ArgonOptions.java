package net.consensys.athena.impl.enclave.sodium.storage;

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

  public String getVariant() {
    return variant;
  }

  public void setVariant(String variant) {
    this.variant = variant;
  }

  public Long getMemory() {
    return memory;
  }

  public void setMemory(long memory) {
    this.memory = memory;
  }

  public Integer getIterations() {
    return iterations;
  }

  public void setIterations(int iterations) {
    this.iterations = iterations;
  }

  public Integer getParallelism() {
    return parallelism;
  }

  public void setParallelism(int parallelism) {
    this.parallelism = parallelism;
  }

  public String getVersion() {
    return version;
  }

  public void setVersion(String version) {
    this.version = version;
  }

  public Long getOpsLimit() {
    return opsLimit;
  }

  public void setOpsLimit(long opsLimit) {
    this.opsLimit = opsLimit;
  }

  public Long getMemLimit() {
    return memLimit;
  }

  public void setMemLimit(long memLimit) {
    this.memLimit = memLimit;
  }
}

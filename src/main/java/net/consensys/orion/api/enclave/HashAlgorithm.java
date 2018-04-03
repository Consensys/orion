package net.consensys.orion.api.enclave;

public enum HashAlgorithm {
  SHA_512_256("SHA-512/256");

  private final String name;

  //We provide a specific name constructor and accessor to simplify the mappings between names for
  // algorithms in the java libraries and what makes sense in the enum.
  HashAlgorithm(String name) {
    this.name = name;
  }

  public String getName() {
    return name;
  }
}

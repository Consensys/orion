package net.consensys.orion.api.network;

import static java.nio.charset.StandardCharsets.UTF_8;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import com.google.common.base.Splitter;
import io.netty.util.internal.StringUtil;

public class HostFingerprintRepository {

  private final Map<String, byte[]> fingerprints;

  private final Path fingerprintFile;

  public HostFingerprintRepository(Path fingerprintFile) throws IOException {
    this.fingerprints = Files.readAllLines(fingerprintFile).stream().filter(line -> !line.startsWith("#")).map(line -> {
      List<String> segments = Splitter.on(" ").splitToList(line);
      return segments;
    }).collect(
        Collectors.toMap(segments -> segments.get(0), segments -> StringUtil.decodeHexDump(segments.get(1)), (u, v) -> {
          throw new IllegalStateException(String.format("Duplicate key %s", u));
        }));
    this.fingerprintFile = fingerprintFile;
  }

  public boolean contains(String peerHostname) {
    return fingerprints.containsKey(peerHostname);
  }

  public boolean contains(String peerHostname, byte[] fingerprint) {
    return Arrays.equals(fingerprints.get(peerHostname), fingerprint);
  }

  public void addHostFingerprint(String host, byte[] fingerprint) {
    try {
      if (!contains(host, fingerprint)) {
        synchronized (fingerprints) {
          if (!contains(host, fingerprint)) {
            String fingerprintAsString = StringUtil.toHexStringPadded(fingerprint);
            Files.write(
                fingerprintFile,
                (host + " " + fingerprintAsString + "\n").getBytes(UTF_8),
                StandardOpenOption.APPEND);
            fingerprints.put(host, fingerprint);
          }
        }
      }
    } catch (IOException e) {
      throw new RuntimeException(e);
    }
  }
}

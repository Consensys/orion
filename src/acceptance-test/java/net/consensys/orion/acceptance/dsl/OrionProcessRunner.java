/*
 * Copyright 2019 ConsenSys AG.
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
package net.consensys.orion.acceptance.dsl;

import static java.nio.charset.StandardCharsets.UTF_8;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.lang.ProcessBuilder.Redirect;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.stream.Stream;

import com.google.common.collect.Lists;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.awaitility.Awaitility;

public class OrionProcessRunner {

  private static final Logger LOG = LogManager.getLogger();
  private static final Logger PROCESS_LOG = LogManager.getLogger("tech.pegasys.orion.SubProcessLog");
  private final Map<String, Process> processes = new HashMap<>();
  private final ExecutorService outputProcessorExecutor = Executors.newCachedThreadPool();


  private final String configFilename;
  private static final String PORTS_FILENAME = "orion.ports";
  private final Path workPath;
  private final Properties portProperties = new Properties();

  public OrionProcessRunner(final String configFilename, final Path workPath) {
    this.configFilename = configFilename;
    this.workPath = workPath;
  }

  public void start(final String processName) {
    final List<String> params = Lists.newArrayList();
    params.add(executableLocation());
    params.add(configFilename);

    final ProcessBuilder processBuilder = new ProcessBuilder(params)
        .directory(new File(System.getProperty("user.dir")).getParentFile())
        .redirectErrorStream(true)
        .redirectInput(Redirect.INHERIT);

    try {
      final Process process = processBuilder.start();
      outputProcessorExecutor.submit(() -> printOutput(processName, process));
      processes.put(processName, process);
    } catch (final IOException e) {
      LOG.error("Error starting Orion process", e);
    }

    loadPortsFile();
  }

  private void printOutput(final String name, final Process process) {
    try (final BufferedReader in = new BufferedReader(new InputStreamReader(process.getInputStream(), UTF_8))) {
      String line = in.readLine();
      while (line != null) {
        PROCESS_LOG.info("{}: {}", name, line);
        line = in.readLine();
      }
    } catch (final IOException e) {
      LOG.error("Failed to read output from process", e);
    }
  }

  private String executableLocation() {
    return "build/install/orion/bin/orion";
  }
/*
  private void killProcess(final String name, final Process process) {
    LOG.info("Killing {} process", name);

    Awaitility.waitAtMost(30, TimeUnit.SECONDS).until(() -> {
      if (process.isAlive()) {
        process.destroy();
        processes.remove(name);
        return false;
      } else {
        processes.remove(name);
        return true;
      }
    });
  }

  public int nodePort() {
    final String value = portProperties.getProperty("http-node-port");
    return Integer.parseInt(value);
  }

  public int clientPort() {
    final String value = portProperties.getProperty("http-client-port");
    return Integer.parseInt(value);
  }
  */


  private void loadPortsFile() {
    final File portsFile = new File(workPath.toFile(), PORTS_FILENAME);
    LOG.info("Awaiting presence of ethsigner.ports file: {}", portsFile.getAbsolutePath());
    awaitPortsFile(workPath);
    LOG.info("Found ethsigner.ports file: {}", portsFile.getAbsolutePath());

    try (final FileInputStream fis = new FileInputStream(portsFile)) {
      portProperties.load(fis);
      LOG.info("EthSigner ports: {}", portProperties);
    } catch (final IOException e) {
      throw new RuntimeException("Error reading Pantheon ports file", e);
    }
  }



  private void awaitPortsFile(final Path dataDir) {
    final File file = new File(dataDir.toFile(), PORTS_FILENAME);
    Awaitility.waitAtMost(30, TimeUnit.SECONDS).until(() -> {
      if (file.exists()) {
        try (final Stream<String> s = Files.lines(file.toPath())) {
          return s.count() > 0;
        }
      }
      return false;
    });
  }


}

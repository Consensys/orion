package net.consensys.athena.impl.config;

import net.consensys.athena.api.config.Config;
import org.junit.Test;

import java.io.File;
import java.io.InputStream;

import static org.junit.Assert.*;

public class TomlConfigBuilderTest {

    @Test
    public void testFullFileRead() throws Exception {

        InputStream configAsStream = this.getClass().getClassLoader().getResourceAsStream("sample.toml");
        //File configFile = new File("src/test/resources/sample.toml");

        TomlConfigBuilder configBuilder = new TomlConfigBuilder();

        Config testConf = configBuilder.build(configAsStream);

        assertEquals(9001, testConf.port());
        assertEquals("dir:storage", testConf.storage());
        assertEquals("strict", testConf.tls());
        assertEquals("ca-or-tofu", testConf.tlsClientTrust());
        assertEquals(1, testConf.verbosity());
        //assertEquals("http://127.0.0.1:9001/", testConf.url());
        
    }

}
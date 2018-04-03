package net.consensys.orion.impl.http.handlers;

import static junit.framework.TestCase.assertEquals;
import static junit.framework.TestCase.assertTrue;
import static net.consensys.orion.impl.http.server.HttpContentType.CBOR;
import static org.junit.Assert.assertArrayEquals;

import net.consensys.orion.api.enclave.Enclave;
import net.consensys.orion.api.enclave.EncryptedPayload;
import net.consensys.orion.api.enclave.HashAlgorithm;
import net.consensys.orion.impl.enclave.sodium.LibSodiumSettings;
import net.consensys.orion.impl.enclave.sodium.SodiumEncryptedPayload;
import net.consensys.orion.impl.enclave.sodium.SodiumPublicKey;
import net.consensys.orion.impl.helpers.StubEnclave;
import net.consensys.orion.impl.http.server.HttpContentType;
import net.consensys.orion.impl.utils.Base64;
import net.consensys.orion.impl.utils.Serializer;

import java.security.PublicKey;
import java.util.Map;
import java.util.Random;

import com.muquit.libsodiumjna.SodiumKeyPair;
import com.muquit.libsodiumjna.SodiumLibrary;
import okhttp3.Request;
import okhttp3.Response;
import okhttp3.mockwebserver.MockResponse;
import okhttp3.mockwebserver.RecordedRequest;
import org.junit.Before;
import org.junit.Test;

public class SendHandlerWithNodeKeysTest extends SendHandlerTest {

  @Override
  @Before
  public void setUp() throws Exception {
    SodiumLibrary.setLibraryPath(LibSodiumSettings.defaultLibSodiumPath());
    super.setUp();
  }

  @Override
  protected Enclave buildEnclave() {
    return new StubEnclave() {
      @Override
      public PublicKey[] nodeKeys() {
        try {
          SodiumKeyPair keyPair = SodiumLibrary.cryptoBoxKeyPair();
          SodiumPublicKey publicKey = new SodiumPublicKey(keyPair.getPublicKey());
          return new PublicKey[] {publicKey};
        } catch (Throwable t) {
          throw new RuntimeException(t);
        }
      }
    };
  }

  @Test
  public void sendWithNoFrom() throws Exception {

    // generate random byte content
    byte[] toEncrypt = new byte[342];
    new Random().nextBytes(toEncrypt);

    // encrypt it here to compute digest
    EncryptedPayload encryptedPayload = enclave.encrypt(toEncrypt, null, null);
    String digest = Base64.encode(enclave.digest(HashAlgorithm.SHA_512_256, encryptedPayload.cipherText()));

    // create fake peer
    FakePeer fakePeer = new FakePeer(new MockResponse().setBody(digest));
    networkNodes.addNode(fakePeer.publicKey, fakePeer.getURL());

    String[] to = new String[] {Base64.encode(fakePeer.publicKey.getEncoded())};

    Map<String, Object> sendRequest = buildRequest(to, toEncrypt, null);
    Request request = buildPrivateAPIRequest("/send", HttpContentType.JSON, sendRequest);

    // execute request
    Response resp = httpClient.newCall(request).execute();

    // ensure it comes back OK.
    assertEquals(200, resp.code());

    // ensure pear actually got the EncryptedPayload
    RecordedRequest recordedRequest = fakePeer.server.takeRequest();

    // check method and path
    assertEquals("/push", recordedRequest.getPath());
    assertEquals("POST", recordedRequest.getMethod());

    // check header
    assertTrue(recordedRequest.getHeader("Content-Type").contains(CBOR.httpHeaderValue));

    // ensure cipher text is same.
    SodiumEncryptedPayload receivedPayload =
        Serializer.deserialize(CBOR, SodiumEncryptedPayload.class, recordedRequest.getBody().readByteArray());
    assertArrayEquals(receivedPayload.cipherText(), encryptedPayload.cipherText());
  }
}

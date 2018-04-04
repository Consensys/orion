package net.consensys.orion.impl.http.handlers;

import static java.nio.charset.StandardCharsets.UTF_8;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import net.consensys.orion.impl.http.handler.send.SendRequest;
import net.consensys.orion.impl.utils.Base64;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.Test;

public class SendRequestTest {

  @Test
  public void invalidBase64Payload() {
    SendRequest request = new SendRequest("something", "foo", new String[] {"foo"});
    assertFalse(request.isValid());
  }

  @Test
  public void noFromValid() {
    SendRequest request = new SendRequest("something".getBytes(UTF_8), null, new String[] {"foo"});
    assertTrue(request.isValid());
  }

  @Test
  public void emptyFromInvalid() {
    SendRequest request = new SendRequest("something".getBytes(UTF_8), "", new String[] {"foo"});
    assertFalse(request.isValid());
  }

  @Test
  public void missingPayload() {
    SendRequest request = new SendRequest((byte[]) null, "foo", new String[] {"foo"});
    assertFalse(request.isValid());
  }

  @Test
  public void emptyPayload() {
    SendRequest request = new SendRequest("".getBytes(UTF_8), "foo", new String[] {"foo"});
    assertFalse(request.isValid());
  }

  @Test
  public void emptyToAddresses() {
    SendRequest request = new SendRequest("something".getBytes(UTF_8), "foo", new String[0]);
    assertFalse(request.isValid());
  }

  @Test
  public void nullToAddresses() {
    SendRequest request = new SendRequest("something".getBytes(UTF_8), "foo", null);
    assertFalse(request.isValid());
  }

  @Test
  public void toAddressesContainNull() {
    SendRequest request = new SendRequest("something".getBytes(UTF_8), "foo", new String[] {null, "foo"});
    assertFalse(request.isValid());
  }

  @Test
  public void jsonToObject() throws Exception {

    String json = "{\"payload\":\"" + Base64.encode("foo".getBytes(UTF_8)) + "\", \"from\":\"foo\", \"to\":[\"foo\"]}";
    ObjectMapper mapper = new ObjectMapper();
    SendRequest req = mapper.readerFor(SendRequest.class).readValue(json);

    assertEquals("foo", req.from().get());
    assertEquals("foo", req.to()[0]);
  }

}

package net.consensys.orion.impl.http.handlers;

import static java.nio.charset.StandardCharsets.UTF_8;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import net.consensys.orion.impl.http.handler.send.SendRequest;
import net.consensys.orion.impl.utils.Base64;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;

class SendRequestTest {

  @Test
  void invalidBase64Payload() {
    SendRequest request = new SendRequest("something", "foo", new String[] {"foo"});
    assertFalse(request.isValid());
  }

  @Test
  void noFromValid() {
    SendRequest request = new SendRequest("something".getBytes(UTF_8), null, new String[] {"foo"});
    assertTrue(request.isValid());
  }

  @Test
  void emptyFromInvalid() {
    SendRequest request = new SendRequest("something".getBytes(UTF_8), "", new String[] {"foo"});
    assertFalse(request.isValid());
  }

  @Test
  void missingPayload() {
    SendRequest request = new SendRequest((byte[]) null, "foo", new String[] {"foo"});
    assertFalse(request.isValid());
  }

  @Test
  void emptyPayload() {
    SendRequest request = new SendRequest("".getBytes(UTF_8), "foo", new String[] {"foo"});
    assertFalse(request.isValid());
  }

  @Test
  void emptyToAddresses() {
    SendRequest request = new SendRequest("something".getBytes(UTF_8), "foo", new String[0]);
    assertFalse(request.isValid());
  }

  @Test
  void nullToAddresses() {
    SendRequest request = new SendRequest("something".getBytes(UTF_8), "foo", null);
    assertFalse(request.isValid());
  }

  @Test
  void toAddressesContainNull() {
    SendRequest request = new SendRequest("something".getBytes(UTF_8), "foo", new String[] {null, "foo"});
    assertFalse(request.isValid());
  }

  @Test
  void jsonToObject() throws Exception {

    String json = "{\"payload\":\"" + Base64.encode("foo".getBytes(UTF_8)) + "\", \"from\":\"foo\", \"to\":[\"foo\"]}";
    ObjectMapper mapper = new ObjectMapper();
    SendRequest req = mapper.readerFor(SendRequest.class).readValue(json);

    assertEquals("foo", req.from().get());
    assertEquals("foo", req.to()[0]);
  }
}

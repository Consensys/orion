//<<<<<<< HEAD
////package net.consensys.athena.impl.http.server.netty;
////
////import static org.junit.Assert.assertEquals;
////import static org.junit.Assert.assertNotNull;
////
////import net.consensys.athena.impl.http.server.HttpContentType;
////import net.consensys.athena.impl.http.data.EmptyPayload;
////import net.consensys.athena.impl.http.data.Request;
////import net.consensys.athena.impl.http.data.Result;
////import net.consensys.athena.impl.http.data.ResultImpl;
////import net.consensys.athena.impl.http.data.Serializer;
////import net.consensys.athena.impl.http.server.Controller;
////import net.consensys.athena.impl.http.server.Router;
////
////import java.net.URI;
////import java.net.URISyntaxException;
////import java.nio.charset.StandardCharsets;
////import java.util.Optional;
////
////import com.fasterxml.jackson.annotation.JsonCreator;
////import com.fasterxml.jackson.annotation.JsonProperty;
////import io.netty.buffer.Unpooled;
////import io.netty.channel.embedded.EmbeddedChannel;
////import io.netty.handler.codec.http.DefaultFullHttpRequest;
////import io.netty.handler.codec.http.DefaultHttpHeaders;
////import io.netty.handler.codec.http.FullHttpRequest;
////import io.netty.handler.codec.http.FullHttpResponse;
////import io.netty.handler.codec.http.HttpHeaderNames;
////import io.netty.handler.codec.http.HttpHeaders;
////import io.netty.handler.codec.http.HttpMethod;
////import io.netty.handler.codec.http.HttpRequest;
////import io.netty.handler.codec.http.HttpResponseStatus;
////import io.netty.handler.codec.http.HttpVersion;
////import org.junit.Before;
////import org.junit.Test;
////
////public class RequestDispatcherTest {
////
////  private final Serializer serializer = new Serializer();
////  private final Router router = new MockRouter();
////  private final RequestDispatcher requestDispatcher = new RequestDispatcher(router, serializer);
////
////  @Before
////  public void setUp() {}
////
////  @Test
////  public void testResponseWithEmptyController() {
////
////    // build an embedded channel to unit test our pipeline
////    EmbeddedChannel ch = new EmbeddedChannel(new RequestDispatcherHandler(requestDispatcher));
////
////    // fake http request
////    HttpRequest fakeHttpRequest =
////        new DefaultFullHttpRequest(HttpVersion.HTTP_1_1, HttpMethod.GET, "/emptypayload");
////
////    // let our channel process it, and ensure we got a result
////    ch.writeInbound(fakeHttpRequest);
////    FullHttpResponse httpResponse = ch.readOutbound();
////    assertNotNull(httpResponse);
////
////    // ensure we got an empty response
////    assertEquals(HttpResponseStatus.OK, httpResponse.status());
////    assertEquals(0, httpResponse.content().readableBytes());
////    assertEquals("header", httpResponse.headers().get("extra"));
////
////    ch.finish();
////  }
////
////  @Test
////  public void testBadURIProducesHttp500() {
////
////    // build an embedded channel to unit test our pipeline
////    EmbeddedChannel ch = new EmbeddedChannel(new RequestDispatcherHandler(requestDispatcher));
////
////    // fake http request
////    HttpRequest fakeHttpRequest =
////        new DefaultFullHttpRequest(HttpVersion.HTTP_1_1, HttpMethod.GET, "/baduri");
////
////    // let our channel process it, and ensure we got a result
////    ch.writeInbound(fakeHttpRequest);
////    FullHttpResponse httpResponse = ch.readOutbound();
////    assertNotNull(httpResponse);
////
////    // ensure we got an empty response
////    assertEquals(HttpResponseStatus.INTERNAL_SERVER_ERROR, httpResponse.status());
////  }
////
////  @Test
////  public void testUnexpectedEmptyPayloadProducesHttp500() {
////    // build an embedded channel to unit test our pipeline
////    EmbeddedChannel ch = new EmbeddedChannel(new RequestDispatcherHandler(requestDispatcher));
////
////    // fake http request
////    HttpRequest fakeHttpRequest =
////        new DefaultFullHttpRequest(HttpVersion.HTTP_1_1, HttpMethod.GET, "/failing");
////
////    // let our channel process it, and ensure we got a result
////    ch.writeInbound(fakeHttpRequest);
////    FullHttpResponse httpResponse = ch.readOutbound();
////    assertNotNull(httpResponse);
////
////    // ensure we got the proper error
////    assertEquals(HttpResponseStatus.INTERNAL_SERVER_ERROR, httpResponse.status());
////    assertEquals(
////        "{\"error\":\"did expect payload\"}",
////        httpResponse.content().toString(StandardCharsets.UTF_8));
////  }
////
////  @Test
////  public void testErrorFormatWhenControllerThrowsException() {
////    // build an embedded channel to unit test our pipeline
////    EmbeddedChannel ch = new EmbeddedChannel(new RequestDispatcherHandler(requestDispatcher));
////
////    // fake http request with payload
////    FullHttpRequest fakeHttpRequest =
////        new DefaultFullHttpRequest(HttpVersion.HTTP_1_1, HttpMethod.GET, "/failing");
////    byte[] payload = serializer.serialize(HttpContentType.CBOR, new MockDummyPayload(42, "toto"));
////    fakeHttpRequest = fakeHttpRequest.replace(Unpooled.copiedBuffer(payload));
////    fakeHttpRequest.headers().add(HttpHeaderNames.CONTENT_TYPE, HttpContentType.CBOR.httpHeaderValue);
////
////    // let our channel process it, and ensure we got a result
////    ch.writeInbound(fakeHttpRequest);
////    FullHttpResponse httpResponse = ch.readOutbound();
////    assertNotNull(httpResponse);
////
////    // ensure we got the proper error
////    assertEquals(HttpResponseStatus.INTERNAL_SERVER_ERROR, httpResponse.status());
////    assertEquals(
////        "{\"error\":\"I'm failing.\"}", httpResponse.content().toString(StandardCharsets.UTF_8));
////  }
////
////  //  @Test TODO we may need this if we decide to do strict-er HTML.
////  //  public void testErrorFormatWhenContentEncodingNotSet() {
////  //    // build an embedded channel to unit test our pipeline
////  //    EmbeddedChannel ch = new EmbeddedChannel(new RequestDispatcherHandler(requestDispatcher));
////  //
////  //    // fake http request with payload
////  //    FullHttpRequest fakeHttpRequest = new DefaultFullHttpRequest(HttpVersion.HTTP_1_1, HttpMethod.GET,
////  //        "/failing");
////  //    byte[] payload = serializer.serialize(HttpContentType.CBOR, new MockDummyPayload(42, "toto"));
////  //    fakeHttpRequest = fakeHttpRequest.replace(Unpooled.copiedBuffer(payload));
////  //
////  //    // let our channel process it, and ensure we got a result
////  //    ch.writeInbound(fakeHttpRequest);
////  //    FullHttpResponse httpResponse = ch.readOutbound();
////  //    assertNotNull(httpResponse);
////  //
////  //    // ensure we got the proper error
////  //    assertEquals(HttpResponseStatus.INTERNAL_SERVER_ERROR, httpResponse.status());
////  //    assertEquals("{\"error\":\"Missing content encoding header\"}", httpResponse.content().toString(StandardCharsets.UTF_8));
////  //  }
////}
////
////// -------------------------------------------------------------------------------------------------
////// Mock objects
////
////class MockRouter implements Router {
////
////  @Override
////  public Controller lookup(HttpRequest request) {
////    try {
////      URI uri = new URI(request.uri());
////      if (uri.getPath().startsWith("/emptypayload")) {
////        return new MockEmptyController();
////      } else if (uri.getPath().startsWith("/failing")) {
////        return new MockFailingController();
////      }
////      throw new RuntimeException("Unsupported uri: " + uri);
////    } catch (URISyntaxException ui) {
////      throw new RuntimeException(ui.getMessage());
////    }
////  }
////}
////
////class MockEmptyController implements Controller {
////
////  @Override
////  public Result handle(Request request) {
////    HttpHeaders extraHeaders = new DefaultHttpHeaders().add("extra", "header");
////    return new ResultImpl(HttpContentType.JSON, Optional.empty(), HttpResponseStatus.OK, extraHeaders);
////  }
////
////  @Override
////  public Class<?> expectedRequest() {
////    return EmptyPayload.class;
////  }
////}
////
////class MockDummyPayload {
////  public int age;
////  public String name;
////
////  @JsonCreator
////  public MockDummyPayload(@JsonProperty("age") int age, @JsonProperty("name") String name) {
////    this.age = age;
////    this.name = name;
////  }
////}
////
////class MockFailingController implements Controller {
////
////  @Override
////  public Result handle(Request request) {
////    throw new RuntimeException("I'm failing.");
////  }
////
////  @Override
////  public Class<?> expectedRequest() {
////    return MockDummyPayload.class;
////  }
////}
//=======
//package net.consensys.athena.impl.http.server.netty;
//
//import static org.junit.Assert.assertEquals;
//import static org.junit.Assert.assertNotNull;
//
//import net.consensys.athena.impl.http.data.ContentType;
//import net.consensys.athena.impl.http.data.EmptyPayload;
//import net.consensys.athena.impl.http.data.Request;
//import net.consensys.athena.impl.http.data.Result;
//import net.consensys.athena.impl.http.data.ResultImpl;
//import net.consensys.athena.impl.http.data.Serializer;
//import net.consensys.athena.impl.http.server.Controller;
//import net.consensys.athena.impl.http.server.Router;
//
//import java.net.URI;
//import java.net.URISyntaxException;
//import java.nio.charset.StandardCharsets;
//import java.util.Optional;
//
//import com.fasterxml.jackson.annotation.JsonCreator;
//import com.fasterxml.jackson.annotation.JsonProperty;
//import io.netty.buffer.Unpooled;
//import io.netty.channel.embedded.EmbeddedChannel;
//import io.netty.handler.codec.http.DefaultFullHttpRequest;
//import io.netty.handler.codec.http.DefaultHttpHeaders;
//import io.netty.handler.codec.http.FullHttpRequest;
//import io.netty.handler.codec.http.FullHttpResponse;
//import io.netty.handler.codec.http.HttpHeaderNames;
//import io.netty.handler.codec.http.HttpHeaders;
//import io.netty.handler.codec.http.HttpMethod;
//import io.netty.handler.codec.http.HttpRequest;
//import io.netty.handler.codec.http.HttpResponseStatus;
//import io.netty.handler.codec.http.HttpVersion;
//import org.junit.Before;
//import org.junit.Test;
//
//public class RequestDispatcherTest {
//
//  private final Serializer serializer = new Serializer();
//  private final Router router = new MockRouter();
//  private final RequestDispatcher requestDispatcher = new RequestDispatcher(router, serializer);
//
//  @Before
//  public void setUp() {}
//
//  @Test
//  public void testResponseWithEmptyController() {
//
//    // build an embedded channel to unit test our pipeline
//    EmbeddedChannel ch = new EmbeddedChannel(new RequestDispatcherHandler(requestDispatcher));
//
//    // fake http request
//    HttpRequest fakeHttpRequest =
//        new DefaultFullHttpRequest(HttpVersion.HTTP_1_1, HttpMethod.GET, "/emptypayload");
//
//    // let our channel process it, and ensure we got a result
//    ch.writeInbound(fakeHttpRequest);
//    FullHttpResponse httpResponse = ch.readOutbound();
//    assertNotNull(httpResponse);
//
//    // ensure we got an empty response
//    assertEquals(HttpResponseStatus.OK, httpResponse.status());
//    assertEquals(0, httpResponse.content().readableBytes());
//    assertEquals("header", httpResponse.headers().get("extra"));
//
//    ch.finish();
//  }
//
//  @Test
//  public void testBadURIProducesHttp500() {
//
//    // build an embedded channel to unit test our pipeline
//    EmbeddedChannel ch = new EmbeddedChannel(new RequestDispatcherHandler(requestDispatcher));
//
//    // fake http request
//    HttpRequest fakeHttpRequest =
//        new DefaultFullHttpRequest(HttpVersion.HTTP_1_1, HttpMethod.GET, "/baduri");
//
//    // let our channel process it, and ensure we got a result
//    ch.writeInbound(fakeHttpRequest);
//    FullHttpResponse httpResponse = ch.readOutbound();
//    assertNotNull(httpResponse);
//
//    // ensure we got an empty response
//    assertEquals(HttpResponseStatus.INTERNAL_SERVER_ERROR, httpResponse.status());
//  }
//
//  @Test
//  public void testUnexpectedEmptyPayloadProducesHttp500() {
//    // build an embedded channel to unit test our pipeline
//    EmbeddedChannel ch = new EmbeddedChannel(new RequestDispatcherHandler(requestDispatcher));
//
//    // fake http request
//    HttpRequest fakeHttpRequest =
//        new DefaultFullHttpRequest(HttpVersion.HTTP_1_1, HttpMethod.GET, "/failing");
//
//    // let our channel process it, and ensure we got a result
//    ch.writeInbound(fakeHttpRequest);
//    FullHttpResponse httpResponse = ch.readOutbound();
//    assertNotNull(httpResponse);
//
//    // ensure we got the proper error
//    assertEquals(HttpResponseStatus.INTERNAL_SERVER_ERROR, httpResponse.status());
//    assertEquals(
//        "{\"error\":\"did expect payload\"}",
//        httpResponse.content().toString(StandardCharsets.UTF_8));
//  }
//
//  @Test
//  public void testErrorFormatWhenControllerThrowsException() {
//    // build an embedded channel to unit test our pipeline
//    EmbeddedChannel ch = new EmbeddedChannel(new RequestDispatcherHandler(requestDispatcher));
//
//    // fake http request with payload
//    FullHttpRequest fakeHttpRequest =
//        new DefaultFullHttpRequest(HttpVersion.HTTP_1_1, HttpMethod.GET, "/failing");
//    byte[] payload = serializer.serialize(ContentType.CBOR, new MockDummyPayload(42, "toto"));
//    fakeHttpRequest = fakeHttpRequest.replace(Unpooled.copiedBuffer(payload));
//    fakeHttpRequest.headers().add(HttpHeaderNames.CONTENT_TYPE, ContentType.CBOR.httpHeaderValue);
//
//    // let our channel process it, and ensure we got a result
//    ch.writeInbound(fakeHttpRequest);
//    FullHttpResponse httpResponse = ch.readOutbound();
//    assertNotNull(httpResponse);
//
//    // ensure we got the proper error
//    assertEquals(HttpResponseStatus.INTERNAL_SERVER_ERROR, httpResponse.status());
//    assertEquals(
//        "{\"error\":\"I'm failing.\"}", httpResponse.content().toString(StandardCharsets.UTF_8));
//  }
//
//  //  @Test TODO we may need this if we decide to do strict-er HTML.
//  //  public void testErrorFormatWhenContentEncodingNotSet() {
//  //    // build an embedded channel to unit test our pipeline
//  //    EmbeddedChannel ch = new EmbeddedChannel(new RequestDispatcherHandler(requestDispatcher));
//  //
//  //    // fake http request with payload
//  //    FullHttpRequest fakeHttpRequest = new DefaultFullHttpRequest(HttpVersion.HTTP_1_1, HttpMethod.GET,
//  //        "/failing");
//  //    byte[] payload = serializer.serialize(ContentType.CBOR, new MockDummyPayload(42, "toto"));
//  //    fakeHttpRequest = fakeHttpRequest.replace(Unpooled.copiedBuffer(payload));
//  //
//  //    // let our channel process it, and ensure we got a result
//  //    ch.writeInbound(fakeHttpRequest);
//  //    FullHttpResponse httpResponse = ch.readOutbound();
//  //    assertNotNull(httpResponse);
//  //
//  //    // ensure we got the proper error
//  //    assertEquals(HttpResponseStatus.INTERNAL_SERVER_ERROR, httpResponse.status());
//  //    assertEquals("{\"error\":\"Missing content encoding header\"}", httpResponse.content().toString(StandardCharsets.UTF_8));
//  //  }
//}
//
//// -------------------------------------------------------------------------------------------------
//// Mock objects
//
//class MockRouter implements Router {
//
//  @Override
//  public Controller lookup(HttpRequest request) {
//    try {
//      URI uri = new URI(request.uri());
//      if (uri.getPath().startsWith("/emptypayload")) {
//        return new MockEmptyController();
//      } else if (uri.getPath().startsWith("/failing")) {
//        return new MockFailingController();
//      }
//      throw new RuntimeException("Unsupported uri: " + uri);
//    } catch (URISyntaxException ui) {
//      throw new RuntimeException(ui.getMessage());
//    }
//  }
//}
//
//class MockEmptyController implements Controller {
//
//  @Override
//  public Result handle(Request request) {
//    HttpHeaders extraHeaders = new DefaultHttpHeaders().add("extra", "header");
//    return new ResultImpl(ContentType.JSON, Optional.empty(), HttpResponseStatus.OK, extraHeaders);
//  }
//
//  @Override
//  public Class<?> expectedRequest() {
//    return EmptyPayload.class;
//  }
//}
//
//class MockDummyPayload {
//  public int age;
//  public String name;
//
//  @JsonCreator
//  public MockDummyPayload(@JsonProperty("age") int age, @JsonProperty("name") String name) {
//    this.age = age;
//    this.name = name;
//  }
//}
//
//class MockFailingController implements Controller {
//
//  @Override
//  public Result handle(Request request) {
//    throw new RuntimeException("I'm failing.");
//  }
//
//  @Override
//  public Class<?> expectedRequest() {
//    return MockDummyPayload.class;
//  }
//}
//>>>>>>> master

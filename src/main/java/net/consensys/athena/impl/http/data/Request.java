//package net.consensys.athena.impl.http.data;
//
//import static java.util.Optional.empty;
//
//import java.util.Map;
//import java.util.Optional;
//
//import io.netty.handler.codec.http.HttpHeaders;
//
//public interface Request {
//
//  <U> Optional<U> getPayload();
//
//  default Optional<HttpHeaders> getExtraHeaders() {
//    return empty();
//  }
//
//  default Optional<Map<String, String>> getParams() {
//    return empty();
//  }
//}

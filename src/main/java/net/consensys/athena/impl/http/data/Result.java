//package net.consensys.athena.impl.http.data;
//
//import static java.util.Optional.*;
//
//import java.util.Optional;
//
//import io.netty.handler.codec.http.HttpHeaders;
//import io.netty.handler.codec.http.HttpResponseStatus;
//import org.jetbrains.annotations.NotNull;
//
//public interface Result<T> {
//
//  @NotNull
//  static <U> Result<U> ok(ContentType defaultContentType, U payload) {
//    return new ResultImpl<>(defaultContentType, of(payload), HttpResponseStatus.OK);
//  }
//
//  @NotNull
//  static Result<ApiError> badRequest(String reason) {
//    return new ResultImpl<>(
//        ContentType.JSON, of(new ApiError(reason)), HttpResponseStatus.BAD_REQUEST);
//  }
//
//  @NotNull
//  static Result<ApiError> notImplemented() {
//    return new ResultImpl<>(
//        ContentType.JSON, of(new ApiError("not implemented")), HttpResponseStatus.NOT_IMPLEMENTED);
//  }
//
//  @NotNull
//  static Result<ApiError> notFound(String reason) {
//    return new ResultImpl<>(
//        ContentType.JSON, of(new ApiError(reason)), HttpResponseStatus.NOT_FOUND);
//  }
//
//  @NotNull
//  static Result<ApiError> internalServerError(String reason) {
//    return new ResultImpl<>(
//        ContentType.JSON, of(new ApiError(reason)), HttpResponseStatus.INTERNAL_SERVER_ERROR);
//  }
//
//  Optional<T> getPayload();
//
//  ContentType defaultContentType();
//
//  default HttpResponseStatus getStatus() {
//    return HttpResponseStatus.OK;
//  }
//
//  default Optional<HttpHeaders> getExtraHeaders() {
//    return empty();
//  }
//}

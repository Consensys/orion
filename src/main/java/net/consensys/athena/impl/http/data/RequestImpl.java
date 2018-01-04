//package net.consensys.athena.impl.http.data;
//
//import java.util.Optional;
//
//public class RequestImpl implements Request {
//
//  private final Object payload;
//
//  public RequestImpl(Object payload) {
//    this.payload = payload;
//  }
//
//  public RequestImpl() {
//    this.payload = null;
//  }
//
//  @Override
//  public <U> Optional<U> getPayload() {
//    // TODO find a nicer way to return a generic optional with clean get API
//    return Optional.ofNullable((U) payload);
//  }
//}

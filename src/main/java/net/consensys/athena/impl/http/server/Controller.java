//package net.consensys.athena.impl.http.server;
//
//import net.consensys.athena.impl.http.data.EmptyPayload;
//import net.consensys.athena.impl.http.data.Request;
//import net.consensys.athena.impl.http.data.Result;
//
//public interface Controller {
//  Result handle(Request request);
//
//  // returns the expected request class, used for deserialization purposes
//  default Class<?> expectedRequest() {
//    return EmptyPayload.class;
//  }
//}

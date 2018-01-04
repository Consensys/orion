//<<<<<<< HEAD
//package net.consensys.athena.impl.http.controllers;
//
//import static org.junit.Assert.assertEquals;
//
//import net.consensys.athena.api.enclave.Enclave;
//import net.consensys.athena.api.enclave.EncryptedPayload;
//import net.consensys.athena.api.storage.Storage;
//import net.consensys.athena.api.storage.StorageKeyBuilder;
//import net.consensys.athena.impl.enclave.sodium.LibSodiumEnclaveStub;
//import net.consensys.athena.impl.http.data.RequestImpl;
//import net.consensys.athena.impl.http.data.Result;
//import net.consensys.athena.impl.http.server.Controller;
//import net.consensys.athena.impl.storage.EncryptedPayloadStorage;
//import net.consensys.athena.impl.storage.Sha512_256StorageKeyBuilder;
//import net.consensys.athena.impl.storage.memory.MemoryStorage;
//
//import java.util.Optional;
//import java.util.Random;
//
//import io.netty.handler.codec.http.HttpResponseStatus;
//import org.junit.Test;
//
//public class PushControllerTest {
//
//  private final Enclave enclave = new LibSodiumEnclaveStub();
//  private final StorageKeyBuilder keyBuilder = new Sha512_256StorageKeyBuilder(enclave);
//  private final Storage<EncryptedPayload> storage =
//      new EncryptedPayloadStorage(new MemoryStorage(), keyBuilder);
//
//  private final Controller controller = new PushController(storage);
//
//  @Test
//  public void testPayloadIsStored() throws Exception {
//    // generate random byte content
//    byte[] toEncrypt = new byte[342];
//    new Random().nextBytes(toEncrypt);
//
//    EncryptedPayload encryptedPayload = enclave.encrypt(toEncrypt, null, null);
//
//    // call controller
//    Result result = controller.handle(new RequestImpl(encryptedPayload));
//
//    // ensure we got a 200 OK back
//    assertEquals(result.getStatus().code(), HttpResponseStatus.OK.code());
//
//    // ensure result has a payload
//    assert (result.getPayload().isPresent());
//
//    // retrieve stored value
//    String controllerResponse = result.getPayload().get().toString();
//    Optional<EncryptedPayload> data = storage.get(controllerResponse);
//
//    // ensure we fetched something
//    assert (data.isPresent());
//
//    assertEquals(data.get(), encryptedPayload);
//  }
//}
//=======
////package net.consensys.athena.impl.http.controllers;
////
////import static org.junit.Assert.assertEquals;
////
////import net.consensys.athena.api.enclave.Enclave;
////import net.consensys.athena.api.enclave.EncryptedPayload;
////import net.consensys.athena.api.storage.Storage;
////import net.consensys.athena.api.storage.StorageKeyBuilder;
////import net.consensys.athena.impl.helpers.CesarEnclave;
////import net.consensys.athena.impl.http.data.RequestImpl;
////import net.consensys.athena.impl.http.data.Result;
////import net.consensys.athena.impl.http.server.Controller;
////import net.consensys.athena.impl.storage.EncryptedPayloadStorage;
////import net.consensys.athena.impl.storage.Sha512_256StorageKeyBuilder;
////import net.consensys.athena.impl.storage.memory.MemoryStorage;
////
////import java.util.Optional;
////import java.util.Random;
////
////import io.netty.handler.codec.http.HttpResponseStatus;
////import org.junit.Test;
////
////public class PushControllerTest {
////
////  private final Enclave enclave = new CesarEnclave();
////  private final StorageKeyBuilder keyBuilder = new Sha512_256StorageKeyBuilder(enclave);
////  private final Storage<EncryptedPayload> storage =
////      new EncryptedPayloadStorage(new MemoryStorage(), keyBuilder);
////
////  private final Controller controller = new PushController(storage);
////
////  @Test
////  public void testPayloadIsStored() throws Exception {
////    // generate random byte content
////    byte[] toEncrypt = new byte[342];
////    new Random().nextBytes(toEncrypt);
////
////    EncryptedPayload encryptedPayload = enclave.encrypt(toEncrypt, null, null);
////
////    // call controller
////    Result result = controller.handle(new RequestImpl(encryptedPayload));
////
////    // ensure we got a 200 OK back
////    assertEquals(result.getStatus().code(), HttpResponseStatus.OK.code());
////
////    // ensure result has a payload
////    assert (result.getPayload().isPresent());
////
////    // retrieve stored value
////    String controllerResponse = result.getPayload().get().toString();
////    Optional<EncryptedPayload> data = storage.get(controllerResponse);
////
////    // ensure we fetched something
////    assert (data.isPresent());
////
////    assertEquals(data.get(), encryptedPayload);
////  }
////}
//>>>>>>> moving to vertx. commented out tests, wip

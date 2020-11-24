package io.cube.agent.logger;

import io.cube.agent.Constants;
import org.java_websocket.client.WebSocketClient;
import org.java_websocket.enums.ReadyState;
import org.java_websocket.handshake.ServerHandshake;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.URI;
import java.net.URISyntaxException;
import java.nio.ByteBuffer;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.CountDownLatch;

/**
 * This example demonstrates how to create a websocket connection to a server. Only the most
 * important callbacks are overloaded.
 */
public class CubeWsClient extends WebSocketClient {

    private static Logger LOGGER = LoggerFactory.getLogger(CubeWsClient.class);
    private CountDownLatch sendLatch = new CountDownLatch(1);
    private boolean hasConnectedOnce = false;
    private Reconnector reconnector = new ExponentialDelayReconnector();


    public static CubeWsClient create(String uri , String token , String customerId) throws URISyntaxException {

        URI serverUri = new URI(uri);
        // check the protocol and other things
        if(!serverUri.getScheme().toLowerCase().startsWith("ws")){
            throw new URISyntaxException(uri , "not a websocket url ws://");
        }

        Map<String , String> authHeaders = new HashMap<>();
        authHeaders.put(Constants.AUTHORIZATION_HEADER , token.startsWith("Bearer") ? token : "Bearer "+token);
        authHeaders.put(Constants.CUSTOMERID_HEADER , customerId);

        return new CubeWsClient(serverUri , authHeaders);
    }

    public CubeWsClient(URI serverUri, Map<String, String> httpHeaders) {
        super(serverUri, httpHeaders);
    }

    @Override
    public void onOpen(ServerHandshake handshakedata) {
        LOGGER.debug("opened connection "+handshakedata.getHttpStatusMessage() + " "+handshakedata.getHttpStatus());
        hasConnectedOnce = true;
        if(sendLatch.getCount()>0){
            sendLatch.countDown();
        }
    }

    @Override
    public void onMessage(ByteBuffer message) {
        LOGGER.debug("received bytebuff "+message);
        this.onMessage(new String(message.array()));
    }

    @Override
    public void onMessage(String message) {
        LOGGER.debug("received: " + message);
    }

    @Override
    public void onClose(int code, String reason, boolean remote) {
        LOGGER.warn("Connection closed by " + (remote ? "remote peer" : "us") + " Code: " + code + " Reason: " + reason);
        informErrorGlobally();
    }

    @Override
    public void onError(Exception ex) {
        LOGGER.error("connection error " , ex);
        reconnector.addErrorHistory(System.currentTimeMillis());
        informErrorGlobally();
    }

    private void informErrorGlobally(){
        if(!hasConnectedOnce){
            if(sendLatch.getCount()>0){
                sendLatch.countDown();
            }
            LOGGER.warn("disabling ws logging");
            CubeLogMgr.setLoggingEnabled(false);
        }
    }

    public void send(String message){
        this.send((Object)message);
    }
    public void send(byte[] message){
        this.send((Object)message);
    }

    public void send(Object message){

        if(ensureConnect()){
            try{

                if(message instanceof String) super.send((String) message);
                else if(message instanceof byte[]) super.send((byte[]) message);
                else throw new Exception("Unsupported Message Class "+message.getClass().getName());

                reconnector.clearErrorHistory();
            }catch (Exception e){
                LOGGER.error("Log send errror" , e);
                reconnector.addErrorHistory(System.currentTimeMillis());
            }

        }else{
            LOGGER.error("ws socket not connected.Ignoring msg ");
        }
    }

    private boolean ensureConnect() {

        if(!CubeLogMgr.isLoggingEnabled()) return false;

        if(!this.isOpen() && reconnector.enableReconnection(System.currentTimeMillis())){
            try{
                if(this.getReadyState() == ReadyState.NOT_YET_CONNECTED){
                    this.sendLatch.await();
                }else if(this.isClosing()){
                    this.closeBlocking();
                    this.reconnectBlocking();
                }else if(this.isClosed()){
                    this.reconnectBlocking();
                }
            }catch(Exception e){
                LOGGER.error("Logging ws client connection error ",e);
                return false;
            }
        }
        return this.isOpen();
    }
}


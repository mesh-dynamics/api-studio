/*
 * Copyright 2021 MeshDynamics.
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *     http://www.apache.org/licenses/LICENSE-2.0
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package io.cube.agent.logger;

import io.cube.agent.Constants;
import org.java_websocket.client.WebSocketClient;
import org.java_websocket.drafts.Draft_6455;
import org.java_websocket.enums.ReadyState;
import org.java_websocket.framing.Framedata;
import org.java_websocket.handshake.ServerHandshake;
import org.java_websocket.server.DefaultSSLWebSocketServerFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLSocketFactory;
import java.net.URI;
import java.net.URISyntaxException;
import java.nio.ByteBuffer;
import java.security.KeyManagementException;
import java.security.NoSuchAlgorithmException;
import java.util.*;
import java.util.concurrent.CountDownLatch;
import java.util.stream.Collectors;

/**
 * This example demonstrates how to create a websocket connection to a server. Only the most
 * important callbacks are overloaded.
 */
public class CubeWsClient extends WebSocketClient {

    private static final Logger LOGGER = LoggerFactory.getLogger(CubeWsClient.class);

    private final CountDownLatch sendLatch = new CountDownLatch(1);

    private boolean hasConnectedOnce = false;

    private final Reconnector reconnector = new ExponentialDelayReconnector();

    private final LinkedList<Framedata> framedataBuff = new LinkedList<>();

    private final Draft_6455 draft = new Draft_6455();

    private static final long MAX_BUFFER_SIZE = 8*1024*1024 ; //8MB
    private static final int PRUNE_SIZE = 200; //Prune this much messages when buffer is full

    private long buffSize = 0;

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
            LOGGER.error("disabling cube ws logging");
            CubeLogMgr.setLoggingEnabled(false);
        }
    }

    @Override
    public void send(String message){
        addToFrameBuff(draft.createFrames( message, true).get(0) , message.length());
        this.send(Optional.empty());
    }
    @Override
    public void send(byte[] message){
        addToFrameBuff(draft.createFrames(ByteBuffer.wrap(message), true).get(0) , message.length);
        this.send(Optional.empty());
    }

    private synchronized  void addToFrameBuff(Framedata frame , int size){
        framedataBuff.add(frame);
        buffSize += size;
        if(buffSize > MAX_BUFFER_SIZE){
            LOGGER.error("Discarding the old logs");
            //prune the buffer
            for(int i=0 ; i<Math.min(PRUNE_SIZE , framedataBuff.size()) ;i++){
                Framedata discarded = framedataBuff.removeFirst();
                buffSize -= discarded.getPayloadData().capacity();
            }
        }

    }
    private synchronized void clearFrameBuff(){
        framedataBuff.clear();
        buffSize = 0;
    }

    private synchronized List<Framedata> getFrames(){
        //create a copy for sending
        return (List<Framedata>)framedataBuff.clone();
    }

    public void send(Optional<Object> data){

        if(ensureConnect()){
            try{

                if(data.isPresent()){
                    Object message = data.get();
                    if(message instanceof String) super.send((String) message);
                    else if(message instanceof byte[]) super.send((byte[]) message);
                    else if(message instanceof ByteBuffer) super.send((ByteBuffer) message);
                    else if(message instanceof Framedata) super.sendFrame((Framedata)message);
                    else if(message instanceof Collection) super.sendFrame((Collection<Framedata>)message);
                    else throw new Exception("Unsupported Message Class "+message.getClass().getName());
                }else{
                    //send it from the frame buffer
                    super.sendFrame(getFrames());
                    clearFrameBuff();
                }

                reconnector.clearErrorHistory();
            }catch (Exception e){
                LOGGER.error("Log send error" , e);
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
                boolean connectAgain = false;
                if(this.getReadyState() == ReadyState.NOT_YET_CONNECTED){
                    LOGGER.warn("waiting for connection to connect");
                    //this.sendLatch.await();
                }else if(this.isClosing()){
                    LOGGER.warn("waiting for connection to close");
                    this.close();
                    //this.closeBlocking();
                    //connectAgain = true;
                    //this.reconnectBlocking();
                }else if(this.isClosed()){
                    connectAgain = true;
                    LOGGER.warn("connection is closed. Connecting again");
                    //this.reconnectBlocking();
                }
                if(connectAgain) this.reconnect(); // This is async and does not block
            }catch(Exception e){
                LOGGER.error("Logging ws client connection error ",e);
                return false;
            }
        }

        return this.isOpen();
    }
}


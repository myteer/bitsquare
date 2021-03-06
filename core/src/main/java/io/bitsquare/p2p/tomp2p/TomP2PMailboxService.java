/*
 * This file is part of Bitsquare.
 *
 * Bitsquare is free software: you can redistribute it and/or modify it
 * under the terms of the GNU Affero General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or (at
 * your option) any later version.
 *
 * Bitsquare is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or
 * FITNESS FOR A PARTICULAR PURPOSE. See the GNU Affero General Public
 * License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * along with Bitsquare. If not, see <http://www.gnu.org/licenses/>.
 */

package io.bitsquare.p2p.tomp2p;

import io.bitsquare.common.handlers.FaultHandler;
import io.bitsquare.common.handlers.ResultHandler;
import io.bitsquare.crypto.KeyRing;
import io.bitsquare.crypto.PubKeyRing;
import io.bitsquare.crypto.SealedAndSignedMessage;
import io.bitsquare.p2p.MailboxMessagesResultHandler;
import io.bitsquare.p2p.MailboxService;
import io.bitsquare.trade.offer.OfferBookService;

import java.io.IOException;

import java.security.KeyPair;
import java.security.PublicKey;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import javax.inject.Inject;

import net.tomp2p.dht.FutureGet;
import net.tomp2p.dht.FuturePut;
import net.tomp2p.dht.FutureRemove;
import net.tomp2p.futures.BaseFuture;
import net.tomp2p.futures.BaseFutureAdapter;
import net.tomp2p.futures.BaseFutureListener;
import net.tomp2p.peers.Number160;
import net.tomp2p.peers.Number640;
import net.tomp2p.storage.Data;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class TomP2PMailboxService extends TomP2PDHTService implements MailboxService {
    private static final Logger log = LoggerFactory.getLogger(TomP2PMailboxService.class);
    private static final int TTL = 21 * 24 * 60 * 60;    // the message is default 21 days valid, as a max trade period might be about 2 weeks.

    private final List<OfferBookService.Listener> offerRepositoryListeners = new ArrayList<>();
    private final KeyPair dhtSignatureKeyPair;

    @Inject
    public TomP2PMailboxService(TomP2PNode tomP2PNode, KeyRing keyRing) {
        super(tomP2PNode, keyRing);

        dhtSignatureKeyPair = keyRing.getDhtSignatureKeyPair();
    }

    @Override
    public void bootstrapCompleted() {
        super.bootstrapCompleted();
    }

    @Override
    public void shutDown() {
        super.shutDown();
    }

    @Override
    public void addMessage(PubKeyRing pubKeyRing, SealedAndSignedMessage message, ResultHandler resultHandler, FaultHandler faultHandler) {
        try {
            final Data data = new Data(message);
            data.ttlSeconds(TTL);
            Number160 locationKey = getLocationKey(pubKeyRing.getDhtSignaturePubKey());
            log.trace("Add message to DHT requested. Added data: [locationKey: " + locationKey +
                    ", hash: " + data.hash().toString() + "]");

            openRequestsUp();
            FuturePut futurePut = addDataToMapOfProtectedDomain(locationKey,
                    data, pubKeyRing.getDhtSignaturePubKey());
            futurePut.addListener(new BaseFutureListener<BaseFuture>() {
                @Override
                public void operationComplete(BaseFuture future) throws Exception {
                    openRequestsDown();
                    if (future.isSuccess()) {
                        executor.execute(() -> {
                            log.trace("Add message to mailbox was successful. Added data: [locationKey: " + locationKey + ", value: " + data + "]");
                            resultHandler.handleResult();
                        });
                    }
                    else {
                        // Seems to be a bug in TomP2P that when one peer shuts down the expected nr of peers and the delivered are not matching
                        // As far tested the storage succeeded, so seems to be a wrong message.
                        //Future (compl/canc):true/false, FAILED, Expected 3 result, but got 2 

                        log.warn("Ignoring isSuccess=false case. failedReason: {}", future.failedReason());
                        resultHandler.handleResult();
                    }
                }

                @Override
                public void exceptionCaught(Throwable ex) throws Exception {
                    openRequestsDown();
                    executor.execute(() -> faultHandler.handleFault("Add message to mailbox failed.", ex));
                }
            });
        } catch (IOException ex) {
            openRequestsDown();
            executor.execute(() -> faultHandler.handleFault("Add message to mailbox failed.", ex));
        }
    }

    @Override
    public void getAllMessages(MailboxMessagesResultHandler resultHandler) {
        log.trace("Get messages from DHT requested for locationKey: " + getLocationKey(dhtSignatureKeyPair.getPublic()));
        FutureGet futureGet = getDataFromMapOfMyProtectedDomain(getLocationKey(dhtSignatureKeyPair.getPublic()));
        futureGet.addListener(new BaseFutureAdapter<BaseFuture>() {
            @Override
            public void operationComplete(BaseFuture future) throws Exception {
                if (future.isSuccess()) {
                    final Map<Number640, Data> dataMap = futureGet.dataMap();
                    List<SealedAndSignedMessage> messages = new ArrayList<>();
                    if (dataMap != null) {
                        for (Data messageData : dataMap.values()) {
                            try {
                                Object messageDataObject = messageData.object();
                                if (messageDataObject instanceof SealedAndSignedMessage) {
                                    messages.add((SealedAndSignedMessage) messageDataObject);
                                }
                            } catch (ClassNotFoundException | IOException e) {
                                e.printStackTrace();
                            }
                        }
                        executor.execute(() -> resultHandler.handleResult(messages));
                    }

                    log.trace("Get messages from DHT was successful. Stored data: [key: " + getLocationKey(dhtSignatureKeyPair.getPublic())
                            + ", values: " + futureGet.dataMap() + "]");
                }
                else {
                    final Map<Number640, Data> dataMap = futureGet.dataMap();
                    if (dataMap == null || dataMap.size() == 0) {
                        log.trace("Get messages from DHT delivered empty dataMap.");
                        executor.execute(() -> resultHandler.handleResult(new ArrayList<>()));
                    }
                    else {
                        log.error("Get messages from DHT  was not successful with reason:" + future.failedReason());
                    }
                }
            }
        });
    }

    @Override
    public void removeAllMessages(ResultHandler resultHandler, FaultHandler faultHandler) {
        log.trace("Remove all messages from DHT requested. locationKey: " + getLocationKey(dhtSignatureKeyPair.getPublic()));
        FutureRemove futureRemove = removeAllDataFromMapOfMyProtectedDomain(getLocationKey(dhtSignatureKeyPair.getPublic()));
        futureRemove.addListener(new BaseFutureListener<BaseFuture>() {
            @Override
            public void operationComplete(BaseFuture future) throws Exception {
                // We don't test futureRemove.isSuccess() as this API does not fit well to that operation, 
                // it might change in future to something like foundAndRemoved and notFound
                // See discussion at: https://github.com/tomp2p/TomP2P/issues/57#issuecomment-62069840
                log.trace("isRemoved? " + futureRemove.isRemoved());
                executor.execute(resultHandler::handleResult);
            }

            @Override
            public void exceptionCaught(Throwable t) throws Exception {
                log.error("Remove all messages from DHT failed. Error: " + t.getMessage());
                faultHandler.handleFault("Remove all messages from DHT failed. Error: " + t.getMessage(), t);
            }
        });
    }


    private Number160 getLocationKey(PublicKey p2pSigPubKey) {
        return Number160.createHash("mailbox" + p2pSigPubKey.hashCode());
    }
}
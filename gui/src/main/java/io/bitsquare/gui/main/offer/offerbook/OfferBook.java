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

package io.bitsquare.gui.main.offer.offerbook;

import io.bitsquare.fiat.FiatAccount;
import io.bitsquare.locale.Country;
import io.bitsquare.locale.CurrencyUtil;
import io.bitsquare.trade.TradeManager;
import io.bitsquare.trade.offer.Offer;
import io.bitsquare.trade.offer.OfferBookService;
import io.bitsquare.user.User;
import io.bitsquare.util.Utilities;

import java.util.List;
import java.util.Timer;

import javax.inject.Inject;

import javafx.beans.value.ChangeListener;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Holds and manages the unsorted and unfiltered offerbook list of both buy and sell offers.
 * It is handled as singleton by Guice and is used by 2 instances of OfferBookDataModel (one for Buy one for Sell).
 * As it is used only by the Buy and Sell UIs we treat it as local UI model.
 * It also use OfferRepository.Listener as the lists items class and we don't want to get any dependency out of the
 * package for that.
 */
public class OfferBook {

    private static final Logger log = LoggerFactory.getLogger(OfferBook.class);
    private static final int POLLING_INTERVAL = 2000; // in ms

    private final OfferBookService offerBookService;
    private final User user;
    private final ChangeListener<FiatAccount> bankAccountChangeListener;
    private final ChangeListener<Number> invalidationListener;
    private final OfferBookService.Listener offerBookServiceListener;

    private final ObservableList<OfferBookListItem> offerBookListItems = FXCollections.observableArrayList();

    private String fiatCode;
    private Timer pollingTimer;
    private Country country;


    ///////////////////////////////////////////////////////////////////////////////////////////
    // Constructor
    ///////////////////////////////////////////////////////////////////////////////////////////

    @Inject
    OfferBook(OfferBookService offerBookService, User user, TradeManager tradeManager) {
        this.offerBookService = offerBookService;
        this.user = user;

        bankAccountChangeListener = (observableValue, oldValue, newValue) -> setBankAccount(newValue);
        invalidationListener = (ov, oldValue, newValue) -> offerBookService.getOffers(fiatCode);

        offerBookServiceListener = new OfferBookService.Listener() {
            @Override
            public void onOfferAdded(Offer offer) {
                addOfferToOfferBookListItems(offer);
            }

            @Override
            public void onOffersReceived(List<Offer> offers) {
                //TODO use deltas instead replacing the whole list
                offerBookListItems.clear();
                offers.stream().forEach(OfferBook.this::addOfferToOfferBookListItems);
            }

            @Override
            public void onOfferRemoved(Offer offer) {
                // Update state in case that that offer is used in the take offer screen, so it gets updated correctly
                offer.setState(Offer.State.REMOVED);

                // clean up possible references in openOfferManager 
                tradeManager.onOfferRemovedFromRemoteOfferBook(offer);

                offerBookListItems.removeIf(item -> item.getOffer().getId().equals(offer.getId()));
            }
        };
    }


    ///////////////////////////////////////////////////////////////////////////////////////////
    // API
    ///////////////////////////////////////////////////////////////////////////////////////////

    void startPolling() {
        addListeners();
        setBankAccount(user.currentFiatAccountProperty().get());
        pollingTimer = Utilities.setInterval(POLLING_INTERVAL, () -> offerBookService.requestInvalidationTimeStampFromDHT(fiatCode));
        offerBookService.getOffers(fiatCode);
    }

    void stopPolling() {
        pollingTimer.cancel();
        removeListeners();
    }

    private void addListeners() {
        user.currentFiatAccountProperty().addListener(bankAccountChangeListener);
        offerBookService.addListener(offerBookServiceListener);
        offerBookService.invalidationTimestampProperty().addListener(invalidationListener);
    }

    private void removeListeners() {
        user.currentFiatAccountProperty().removeListener(bankAccountChangeListener);
        offerBookService.removeListener(offerBookServiceListener);
        offerBookService.invalidationTimestampProperty().removeListener(invalidationListener);
    }


    ///////////////////////////////////////////////////////////////////////////////////////////
    // Getter
    ///////////////////////////////////////////////////////////////////////////////////////////

    ObservableList<OfferBookListItem> getOfferBookListItems() {
        return offerBookListItems;
    }


    ///////////////////////////////////////////////////////////////////////////////////////////
    // Utils
    ///////////////////////////////////////////////////////////////////////////////////////////

    private void setBankAccount(FiatAccount fiatAccount) {
        log.debug("setBankAccount " + fiatAccount);
        if (fiatAccount != null) {
            country = fiatAccount.country;
            fiatCode = fiatAccount.currencyCode;

            // TODO check why that was used (probably just for update triggering, if so refactor that)
            //offerBookListItems.stream().forEach(e -> e.setBankAccountCountry(country));
        }
        else {
            fiatCode = CurrencyUtil.getDefaultCurrencyAsCode();
        }
    }

    private void addOfferToOfferBookListItems(Offer offer) {
        if (offer != null) {
            offerBookListItems.add(new OfferBookListItem(offer, country));
        }
    }
}

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

package io.bitsquare.app;

import io.bitsquare.BitsquareModule;
import io.bitsquare.arbitration.ArbitratorModule;
import io.bitsquare.arbitration.tomp2p.TomP2PArbitratorModule;
import io.bitsquare.btc.BitcoinModule;
import io.bitsquare.crypto.CryptoModule;
import io.bitsquare.crypto.KeyRing;
import io.bitsquare.crypto.KeyStorage;
import io.bitsquare.gui.GuiModule;
import io.bitsquare.p2p.P2PModule;
import io.bitsquare.p2p.tomp2p.TomP2PModule;
import io.bitsquare.storage.Storage;
import io.bitsquare.trade.TradeModule;
import io.bitsquare.trade.offer.OfferModule;
import io.bitsquare.trade.offer.tomp2p.TomP2POfferModule;
import io.bitsquare.user.AccountSettings;
import io.bitsquare.user.Preferences;
import io.bitsquare.user.User;

import com.google.inject.Injector;
import com.google.inject.Singleton;

import java.io.File;

import javafx.stage.Stage;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import org.springframework.core.env.Environment;

import static com.google.inject.name.Names.named;

class BitsquareAppModule extends BitsquareModule {
    private static final Logger log = LoggerFactory.getLogger(BitsquareAppModule.class);

    private final Stage primaryStage;

    public BitsquareAppModule(Environment env, Stage primaryStage) {
        super(env);
        this.primaryStage = primaryStage;
    }

    @Override
    protected void configure() {
        bind(KeyStorage.class).in(Singleton.class);
        bind(KeyRing.class).in(Singleton.class);
        bind(User.class).in(Singleton.class);
        bind(Preferences.class).in(Singleton.class);
        bind(AccountSettings.class).in(Singleton.class);

        File storageDir = new File(env.getRequiredProperty(Storage.DIR_KEY));
        bind(File.class).annotatedWith(named(Storage.DIR_KEY)).toInstance(storageDir);

        File keyStorageDir = new File(env.getRequiredProperty(KeyStorage.DIR_KEY));
        bind(File.class).annotatedWith(named(KeyStorage.DIR_KEY)).toInstance(keyStorageDir);

        bind(BitsquareEnvironment.class).toInstance((BitsquareEnvironment) env);
        bind(UpdateProcess.class).in(Singleton.class);

        // ordering is used for shut down sequence
        install(tradeModule());
        install(cryptoModule());
        install(arbitratorModule());
        install(offerModule());
        install(p2pModule());
        install(bitcoinModule());
        install(guiModule());
    }

    protected TradeModule tradeModule() {
        return new TradeModule(env);
    }

    protected CryptoModule cryptoModule() {
        return new CryptoModule(env);
    }

    protected ArbitratorModule arbitratorModule() {
        return new TomP2PArbitratorModule(env);
    }

    protected OfferModule offerModule() {
        return new TomP2POfferModule(env);
    }

    protected P2PModule p2pModule() {
        return new TomP2PModule(env);
    }

    protected BitcoinModule bitcoinModule() {
        return new BitcoinModule(env);
    }

    protected GuiModule guiModule() {
        return new GuiModule(env, primaryStage);
    }

    @Override
    protected void doClose(Injector injector) {
        log.trace("doClose " + getClass().getSimpleName());
    }
}

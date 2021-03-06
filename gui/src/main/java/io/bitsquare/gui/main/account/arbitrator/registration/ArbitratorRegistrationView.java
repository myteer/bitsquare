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

package io.bitsquare.gui.main.account.arbitrator.registration;

import io.bitsquare.arbitration.Arbitrator;
import io.bitsquare.arbitration.ArbitratorService;
import io.bitsquare.btc.WalletService;
import io.bitsquare.gui.common.view.ActivatableView;
import io.bitsquare.gui.common.view.FxmlView;
import io.bitsquare.gui.components.confidence.ConfidenceProgressIndicator;
import io.bitsquare.gui.util.BSFormatter;
import io.bitsquare.gui.util.GUIUtil;
import io.bitsquare.locale.BSResources;
import io.bitsquare.locale.LanguageUtil;

import org.bitcoinj.core.Coin;
import org.bitcoinj.core.ECKey;
import org.bitcoinj.core.Transaction;
import org.bitcoinj.core.Wallet;
import org.bitcoinj.core.WalletEventListener;
import org.bitcoinj.script.Script;

import java.util.ArrayList;
import java.util.EnumSet;
import java.util.List;

import javax.inject.Inject;

import javafx.collections.FXCollections;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import javafx.stage.Stage;
import javafx.util.StringConverter;

import de.jensd.fx.fontawesome.AwesomeDude;
import de.jensd.fx.fontawesome.AwesomeIcon;

@FxmlView
public class ArbitratorRegistrationView extends ActivatableView<AnchorPane, Void> {

    @FXML Accordion accordion;
    @FXML TextArea descriptionTextArea;
    @FXML Button saveProfileButton, paymentDoneButton;
    @FXML Label nameLabel, infoLabel, copyIcon, confirmationLabel;
    @FXML ComboBox<String> languageComboBox;
    @FXML ComboBox<Arbitrator.ID_TYPE> idTypeComboBox;
    @FXML ComboBox<Arbitrator.METHOD> methodsComboBox;
    @FXML ConfidenceProgressIndicator progressIndicator;
    @FXML ComboBox<Arbitrator.ID_VERIFICATION> idVerificationsComboBox;
    @FXML TitledPane profileTitledPane, paySecurityDepositTitledPane;
    @FXML TextField nameTextField, idTypeTextField, languagesTextField, maxTradeVolumeTextField,
            passiveServiceFeeTextField, minPassiveServiceFeeTextField, arbitrationFeeTextField,
            minArbitrationFeeTextField, methodsTextField, idVerificationsTextField, webPageTextField,
            securityDepositAddressTextField, balanceTextField;

    private boolean isEditMode;
    private Arbitrator.ID_TYPE idType;

    private List<String> languageCodes = new ArrayList<>();
    private List<Arbitrator.METHOD> methodList = new ArrayList<>();
    private List<Arbitrator.ID_VERIFICATION> idVerificationList = new ArrayList<>();

    // TODO not set
    private Arbitrator arbitrator;

    private final WalletService walletService;
    private final ArbitratorService arbitratorService;
    private final BSFormatter formatter;


    @Inject
    private ArbitratorRegistrationView(WalletService walletService,
                                       ArbitratorService arbitratorService, BSFormatter formatter) {
        this.walletService = walletService;
        this.arbitratorService = arbitratorService;
        this.formatter = formatter;
    }

    @Override
    public void initialize() {
        accordion.setExpandedPane(profileTitledPane);

        applyArbitrator();

        languageComboBox.setItems(FXCollections.observableArrayList(LanguageUtil.getAllLanguageLocaleCodes()));
        languageComboBox.setConverter(new StringConverter<String>() {
            @Override
            public String toString(String code) {
                return LanguageUtil.getDisplayName(code);
            }

            @Override
            public String fromString(String s) {
                return null;
            }
        });

        idTypeComboBox.setItems(FXCollections.observableArrayList(
                new ArrayList<>(EnumSet.allOf(Arbitrator.ID_TYPE.class))));
        idTypeComboBox.setConverter(new StringConverter<Arbitrator.ID_TYPE>() {
            @Override
            public String toString(Arbitrator.ID_TYPE item) {
                return BSResources.get(item.toString());
            }

            @Override
            public Arbitrator.ID_TYPE fromString(String s) {
                return null;
            }
        });

        methodsComboBox.setItems(FXCollections.observableArrayList(new ArrayList<>(EnumSet.allOf(Arbitrator.METHOD
                .class))));
        methodsComboBox.setConverter(new StringConverter<Arbitrator.METHOD>() {
            @Override
            public String toString(Arbitrator.METHOD item) {
                return BSResources.get(item.toString());
            }

            @Override
            public Arbitrator.METHOD fromString(String s) {
                return null;
            }
        });

        idVerificationsComboBox.setItems(
                FXCollections.observableArrayList(new ArrayList<>(EnumSet.allOf(Arbitrator.ID_VERIFICATION.class))));
        idVerificationsComboBox.setConverter(new StringConverter<Arbitrator.ID_VERIFICATION>() {
            @Override
            public String toString(Arbitrator.ID_VERIFICATION item) {
                return BSResources.get(item.toString());
            }

            @Override
            public Arbitrator.ID_VERIFICATION fromString(String s) {
                return null;
            }
        });
    }

    public void setEditMode(boolean isEditMode) {
        this.isEditMode = isEditMode;

        if (isEditMode) {
            saveProfileButton.setText("Save");
            profileTitledPane.setCollapsible(false);
            paySecurityDepositTitledPane.setVisible(false);
        }
    }

    @FXML
    public void onSelectIDType() {
        idType = idTypeComboBox.getSelectionModel().getSelectedItem();
        if (idType != null) {
            idTypeTextField.setText(BSResources.get(idType.toString()));

            String name = "";
            switch (idType) {
                case REAL_LIFE_ID:
                    name = "Name:";
                    break;
                case NICKNAME:
                    name = "Nickname:";
                    break;
                case COMPANY:
                    name = "Company:";
                    break;
            }
            nameLabel.setText(name);

            idTypeComboBox.getSelectionModel().clearSelection();
        }
    }

    @FXML
    public void onAddLanguage() {
        String item = languageComboBox.getSelectionModel().getSelectedItem();
        if (!languageCodes.contains(item) && item != null) {
            languageCodes.add(item);
            languagesTextField.setText(formatter.languageCodesToString(languageCodes));
            languageComboBox.getSelectionModel().clearSelection();
        }
    }

    @FXML
    public void onClearLanguages() {
        languageCodes.clear();
        languagesTextField.setText("");
    }

    @FXML
    public void onAddMethod() {
        Arbitrator.METHOD item = methodsComboBox.getSelectionModel().getSelectedItem();
        if (!methodList.contains(item) && item != null) {
            methodList.add(item);
            methodsTextField.setText(formatter.arbitrationMethodsToString(methodList));
            methodsComboBox.getSelectionModel().clearSelection();
        }
    }

    @FXML
    public void onClearMethods() {
        methodList.clear();
        methodsTextField.setText("");
    }

    @FXML
    public void onAddIDVerification() {
        Arbitrator.ID_VERIFICATION idVerification = idVerificationsComboBox.getSelectionModel().getSelectedItem();
        if (idVerification != null) {
            if (!idVerificationList.contains(idVerification)) {
                idVerificationList.add(idVerification);
                idVerificationsTextField.setText(
                        formatter.arbitrationIDVerificationsToString(idVerificationList));
            }
        }

        idVerificationsComboBox.getSelectionModel().clearSelection();
    }

    @FXML
    public void onClearIDVerifications() {
        idVerificationList.clear();
        idVerificationsTextField.setText("");
    }

    @FXML
    public void onSaveProfile() {
        arbitrator.setWebUrl(webPageTextField.getText());
        arbitrator.setFee(formatter.parseToCoin(arbitrationFeeTextField.getText()));
        arbitrator.setIdType(idType);
        arbitrator.setIdVerifications(idVerificationList);
        arbitrator.setLanguageCodes(languageCodes);
        arbitrator.setArbitrationMethods(methodList);
        arbitrator.save();

        if (isEditMode) {
            close();
        }
        else {
            setupPaySecurityDepositScreen();
            accordion.setExpandedPane(paySecurityDepositTitledPane);
        }

        arbitratorService.addArbitrator(arbitrator,
                () -> {
                    // log.debug("arbitrator added successfully " + arbitratorService.getAllArbitrators().size());
                },
                log::error);
    }

    @FXML
    public void onPaymentDone() {
    }

    private void setupPaySecurityDepositScreen() {
        infoLabel.setText("You need to pay 2 x the max. trading volume as security deposit.\n\nThat payment will be " +
                "locked into a MultiSig fund and be refunded when you leave the arbitration pool.\nIn case of fraud " +
                "(collusion, not fulfilling the min. dispute quality requirements) you will lose your security " +
                "deposit.\n" +
                "If you have a negative feedback from your clients you will lose a part of the security deposit,\n" +
                "depending on the overall relation of negative to positive ratings you received after a dispute " +
                "resolution.\n\nPlease pay in 2 BTC");


        String securityDepositAddress = walletService.getRegistrationAddressEntry() != null ?
                walletService.getRegistrationAddressEntry().toString() : "";
        securityDepositAddressTextField.setText(securityDepositAddress);

        AwesomeDude.setIcon(copyIcon, AwesomeIcon.COPY);
        copyIcon.setOnMouseClicked(e -> GUIUtil.copyToClipboard(securityDepositAddress));

        paymentDoneButton.setDisable(walletService.getArbitratorDepositBalance().isZero());
        log.debug("getArbitratorDepositBalance " + walletService.getArbitratorDepositBalance());
        walletService.getWallet().addEventListener(new WalletEventListener() {
            @Override
            public void onCoinsReceived(Wallet wallet, Transaction tx, Coin prevBalance, Coin newBalance) {
                paymentDoneButton.setDisable(newBalance.isZero());
            }

            @Override
            public void onCoinsSent(Wallet wallet, Transaction tx, Coin prevBalance, Coin newBalance) {

            }

            @Override
            public void onReorganize(Wallet wallet) {

            }

            @Override
            public void onTransactionConfidenceChanged(Wallet wallet, Transaction tx) {

            }

            @Override
            public void onWalletChanged(Wallet wallet) {

            }

            @Override
            public void onScriptsChanged(Wallet var1, List<Script> scripts, boolean isAddingScripts) {
            }

            @Override
            public void onKeysAdded(List<ECKey> keys) {

            }
        });
    }

    private void applyArbitrator() {
        if (arbitrator != null) {
            String name = "";
            switch (arbitrator.getIdType()) {
                case REAL_LIFE_ID:
                    name = "Name:";
                    break;
                case NICKNAME:
                    name = "Nickname:";
                    break;
                case COMPANY:
                    name = "Company:";
                    break;
            }
            nameLabel.setText(name);

            nameTextField.setText(arbitrator.getName());
            idTypeTextField.setText(BSResources.get(arbitrator.getIdType().toString()));
            // languagesTextField.setText(formatter.languageLocalesToString(arbitrator.getLanguages()));
            arbitrationFeeTextField.setText(String.valueOf(arbitrator.getFee()));
            methodsTextField.setText(formatter.arbitrationMethodsToString(arbitrator.getArbitrationMethods()));
            idVerificationsTextField.setText(
                    formatter.arbitrationIDVerificationsToString(arbitrator.getIdVerifications()));
            webPageTextField.setText(arbitrator.getWebUrl());
            descriptionTextArea.setText(arbitrator.getDescription());

            idType = arbitrator.getIdType();
            languageCodes = arbitrator.getLanguageCodes();
            methodList = arbitrator.getArbitrationMethods();
            idVerificationList = arbitrator.getIdVerifications();
        }
    }

    private void close() {
        Stage stage = (Stage) root.getScene().getWindow();
        stage.close();
    }
}


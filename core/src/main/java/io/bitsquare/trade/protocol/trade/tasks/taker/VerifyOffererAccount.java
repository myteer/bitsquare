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

package io.bitsquare.trade.protocol.trade.tasks.taker;

import io.bitsquare.common.taskrunner.TaskRunner;
import io.bitsquare.trade.Trade;
import io.bitsquare.trade.protocol.trade.tasks.TradeTask;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class VerifyOffererAccount extends TradeTask {
    private static final Logger log = LoggerFactory.getLogger(VerifyOffererAccount.class);

    public VerifyOffererAccount(TaskRunner taskHandler, Trade trade) {
        super(taskHandler, trade);
    }

    @Override
    protected void run() {
        try {
            runInterceptHook();
            if (processModel.getBlockChainService().verifyAccountRegistration()) {
                if (processModel.getBlockChainService().isAccountBlackListed(processModel.tradingPeer.getAccountId(),
                        processModel.tradingPeer.getFiatAccount())) {
                    failed("Taker is blacklisted.");
                }
                else {
                    complete();
                }
            }
            else {
                failed("Account registration validation for peer faultHandler.onFault.");
            }
        } catch (Throwable t) {
            failed(t);
        }
    }
}

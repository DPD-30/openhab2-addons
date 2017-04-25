package org.openhab.binding.omnilink.handler;

import org.eclipse.smarthome.core.library.types.DecimalType;
import org.eclipse.smarthome.core.thing.ChannelUID;
import org.eclipse.smarthome.core.thing.Thing;
import org.eclipse.smarthome.core.thing.UID;
import org.eclipse.smarthome.core.types.Command;
import org.eclipse.smarthome.core.types.RefreshType;
import org.eclipse.smarthome.core.types.State;
import org.openhab.binding.omnilink.OmnilinkBindingConstants;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.digitaldan.jomnilinkII.MessageTypes.statuses.UnitStatus;
import com.google.common.util.concurrent.FutureCallback;
import com.google.common.util.concurrent.Futures;

public class FlagHandler extends AbstractOmnilinkHandler implements UnitHandler {

    public FlagHandler(Thing thing) {
        super(thing);
    }

    private Logger logger = LoggerFactory.getLogger(FlagHandler.class);

    @Override
    public void handleUpdate(ChannelUID channelUID, State newState) {
        super.handleUpdate(channelUID, newState);
    }

    @Override
    public void handleCommand(ChannelUID channelUID, Command command) {
        logger.debug("handleCommand called for channel:{}, command:{}", channelUID, command);
        final String[] channelParts = channelUID.getAsString().split(UID.SEPARATOR);
        if (command instanceof RefreshType) {
            logger.debug("Handling refresh");
            Futures.addCallback(getOmnilinkBridgeHander().getUnitStatus(Integer.parseInt(channelParts[2])),
                    new FutureCallback<UnitStatus>() {
                        @Override
                        public void onFailure(Throwable arg0) {
                            logger.error("Failed retrieving status for unit #: {}, error: {}", channelParts[2], arg0);
                        }

                        @Override
                        public void onSuccess(UnitStatus status) {
                            handleUnitStatus(status);
                        }
                    });
        } else if (command instanceof DecimalType) {
            logger.debug("updating omnilink flag change: {}, command: {}", channelUID, command);
            getOmnilinkBridgeHander().sendOmnilinkCommand(OmniLinkCmd.CMD_UNIT_ON.getNumber(),
                    ((DecimalType) command).intValue(), Integer.parseInt(channelParts[2]));
        } else {
            logger.warn("Must handle command: {}", command);
        }
    }

    @Override
    public void handleUnitStatus(UnitStatus unitStatus) {
        logger.debug("need to handle status update{}", unitStatus);
        int status = unitStatus.getStatus();
        State newState = DecimalType.valueOf(Integer.toString(status));
        updateState(OmnilinkBindingConstants.CHANNEL_FLAG, newState);

    }
}
package org.openhab.binding.isy.handler;

import org.eclipse.smarthome.core.library.types.OnOffType;
import org.eclipse.smarthome.core.thing.ChannelUID;
import org.eclipse.smarthome.core.thing.Thing;
import org.eclipse.smarthome.core.thing.ThingStatus;
import org.eclipse.smarthome.core.types.Command;
import org.eclipse.smarthome.core.types.RefreshType;
import org.openhab.binding.isy.config.IsyProgramConfiguration;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class IsyProgramHandler extends AbtractIsyThingHandler implements IsyThingHandler {
    private static final Logger logger = LoggerFactory.getLogger(IsyProgramHandler.class);

    public IsyProgramHandler(Thing thing) {
        super(thing);
    }

    @Override
    public void handleCommand(ChannelUID channelUID, Command command) {
        if (RefreshType.REFRESH.equals(command)) {
            logger.debug("Refresh not implemented for programs");
        } else {
            if (command instanceof OnOffType) {
                IsyProgramConfiguration var_config = getThing().getConfiguration().as(IsyProgramConfiguration.class);
                getBridgeHandler().getInsteonClient().changeProgramState(var_config.id, channelUID.getId());
            } else {
                logger.warn("Unsupported command for variable handleCommand: " + command.toFullString());
            }
        }

    }

    @Override
    public void handleUpdate(Object... parameters) {
        // TODO Auto-generated method stub
        logger.warn("Must handle update for program");
    }

    @Override
    public void initialize() {
        // TODO: Initialize the thing. If done set status to ONLINE to indicate proper working.
        // Long running initialization should be done asynchronously in background.
        updateStatus(ThingStatus.ONLINE);

        // Note: When initialization can NOT be done set the status with more details for further
        // analysis. See also class ThingStatusDetail for all available status details.
        // Add a description to give user information to understand why thing does not work
        // as expected. E.g.
        // updateStatus(ThingStatus.OFFLINE, ThingStatusDetail.CONFIGURATION_ERROR,
        // "Can not access device as username and/or password are invalid");
    }
}

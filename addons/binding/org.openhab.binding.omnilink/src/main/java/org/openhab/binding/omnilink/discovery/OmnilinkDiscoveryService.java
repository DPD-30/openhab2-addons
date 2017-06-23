package org.openhab.binding.omnilink.discovery;

import java.io.IOException;
import java.math.BigInteger;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import org.eclipse.smarthome.config.discovery.AbstractDiscoveryService;
import org.eclipse.smarthome.config.discovery.DiscoveryResult;
import org.eclipse.smarthome.config.discovery.DiscoveryResultBuilder;
import org.eclipse.smarthome.core.thing.ThingTypeUID;
import org.eclipse.smarthome.core.thing.ThingUID;
import org.openhab.binding.omnilink.OmnilinkBindingConstants;
import org.openhab.binding.omnilink.handler.BridgeOfflineException;
import org.openhab.binding.omnilink.handler.OmnilinkBridgeHandler;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.digitaldan.jomnilinkII.Message;
import com.digitaldan.jomnilinkII.OmniInvalidResponseException;
import com.digitaldan.jomnilinkII.OmniNotConnectedException;
import com.digitaldan.jomnilinkII.OmniUnknownMessageTypeException;
import com.digitaldan.jomnilinkII.MessageTypes.ObjectProperties;
import com.digitaldan.jomnilinkII.MessageTypes.SystemInformation;
import com.digitaldan.jomnilinkII.MessageTypes.properties.AreaProperties;
import com.digitaldan.jomnilinkII.MessageTypes.properties.ButtonProperties;
import com.digitaldan.jomnilinkII.MessageTypes.properties.ConsoleProperties;
import com.digitaldan.jomnilinkII.MessageTypes.properties.UnitProperties;
import com.digitaldan.jomnilinkII.MessageTypes.properties.ZoneProperties;
import com.google.common.collect.ImmutableSet;

public class OmnilinkDiscoveryService extends AbstractDiscoveryService {
    private static final Logger logger = LoggerFactory.getLogger(OmnilinkDiscoveryService.class);
    private static final int DISCOVER_TIMEOUT_SECONDS = 30;
    private OmnilinkBridgeHandler bridgeHandler;
    private boolean isLumina = false;
    private List<AreaProperties> areas;

    /**
     * Creates an OmnilinkDiscoveryService.
     */
    public OmnilinkDiscoveryService(OmnilinkBridgeHandler bridgeHandler) {
        super(ImmutableSet.of(new ThingTypeUID(OmnilinkBindingConstants.BINDING_ID, "-")), DISCOVER_TIMEOUT_SECONDS,
                false);
        this.bridgeHandler = bridgeHandler;
    }

    public void activate() {
        bridgeHandler.registerDiscoveryService(this);
    }

    /**
     * Deactivates the Discovery Service.
     */
    @Override
    public void deactivate() {
        bridgeHandler.unregisterDiscoveryService();
    }

    @Override
    protected void startScan() {
        logger.debug("Starting scan");
        try {
            SystemInformation info = bridgeHandler.reqSystemInformation();
            isLumina = info.getModel() == 36 || info.getModel() == 37;
            areas = discoverAreas();
            discoverUnits();
            discoverZones();
            discoverButtons();
            discoverThermostats();
            discoverAudioZones();
            // generate consoles is throwing and error
            // generateConsoles();
        } catch (OmniInvalidResponseException | OmniUnknownMessageTypeException | BridgeOfflineException e) {
            logger.debug("Received error during discovery", e);
        }
    }

    /**
     * Calculate the area filter the a supplied area
     *
     * @param area Area to calculate filter for.
     * @return Calculated Bit Filter for the supplied area. Bit 0 is area 1, bit 2 is area 2 and so on.
     */
    private static int bitFilterForArea(AreaProperties areaProperties) {
        return BigInteger.ZERO.setBit(areaProperties.getNumber() - 1).intValue();
    }

    private void discoverButtons()
            throws OmniInvalidResponseException, OmniUnknownMessageTypeException, BridgeOfflineException {

        for (AreaProperties areaProperties : areas) {

            int areaFilter = bitFilterForArea(areaProperties);

            ObjectPropertyRequest objectPropertyRequest = ObjectPropertyRequest
                    .builder(bridgeHandler, Message.OBJ_TYPE_BUTTON).selectNamed().areaFilter(areaFilter).build();

            for (ObjectProperties objectProperties : objectPropertyRequest) {

                ButtonProperties buttonProperties = ((ButtonProperties) objectProperties);
                int objnum = buttonProperties.getNumber();
                Map<String, Object> properties = new HashMap<>();
                ThingUID thingUID = new ThingUID(OmnilinkBindingConstants.THING_TYPE_BUTTON,
                        bridgeHandler.getThing().getUID(), Integer.toString(objnum));
                properties.put(OmnilinkBindingConstants.THING_PROPERTIES_NUMBER, objnum);
                properties.put(OmnilinkBindingConstants.THING_PROPERTIES_NAME, buttonProperties.getName());
                properties.put(OmnilinkBindingConstants.THING_PROPERTIES_AREA, areaProperties.getNumber());

                DiscoveryResult discoveryResult = DiscoveryResultBuilder.create(thingUID).withProperties(properties)
                        .withBridge(this.bridgeHandler.getThing().getUID()).withLabel(buttonProperties.getName())
                        .build();
                thingDiscovered(discoveryResult);
            }
        }
    }

    private void generateConsoles()
            throws OmniInvalidResponseException, OmniUnknownMessageTypeException, BridgeOfflineException {

        for (AreaProperties areaProperties : areas) {
            int areaFilter = bitFilterForArea(areaProperties);

            ObjectPropertyRequest objectPropertyRequest = ObjectPropertyRequest
                    .builder(bridgeHandler, Message.OBJ_TYPE_CONSOLE).areaFilter(areaFilter).build();

            for (ObjectProperties objectProperties : objectPropertyRequest) {

                ConsoleProperties consoleProperties = ((ConsoleProperties) objectProperties);
                int objnum = consoleProperties.getNumber();
                Map<String, Object> properties = new HashMap<>();
                ThingUID thingUID = new ThingUID(OmnilinkBindingConstants.THING_TYPE_CONSOLE,
                        bridgeHandler.getThing().getUID(), Integer.toString(objnum));
                properties.put(OmnilinkBindingConstants.THING_PROPERTIES_NUMBER, objnum);
                properties.put(OmnilinkBindingConstants.THING_PROPERTIES_AREA, areaProperties.getNumber());

                DiscoveryResult discoveryResult = DiscoveryResultBuilder.create(thingUID).withProperties(properties)
                        .withBridge(this.bridgeHandler.getThing().getUID()).withLabel(consoleProperties.getName())
                        .build();
                thingDiscovered(discoveryResult);
            }
        }
    }

    private void discoverAudioZones()
            throws OmniInvalidResponseException, OmniUnknownMessageTypeException, BridgeOfflineException {

        ObjectPropertyRequest objectPropertyRequest = ObjectPropertyRequest
                .builder(bridgeHandler, Message.OBJ_TYPE_AUDIO_ZONE).selectNamed().build();

        for (ObjectProperties objectProperties : objectPropertyRequest) {

            ThingUID thingUID = new ThingUID(OmnilinkBindingConstants.THING_TYPE_AUDIO_ZONE,
                    Integer.toString(objectProperties.getNumber()));

            Map<String, Object> properties = new HashMap<>();
            properties.put(OmnilinkBindingConstants.THING_PROPERTIES_NUMBER, objectProperties.getNumber());
            properties.put(OmnilinkBindingConstants.THING_PROPERTIES_NAME, objectProperties.getName());

            DiscoveryResult discoveryResult = DiscoveryResultBuilder.create(thingUID).withProperties(properties)
                    .withBridge(this.bridgeHandler.getThing().getUID()).withLabel(objectProperties.getName()).build();
            thingDiscovered(discoveryResult);
        }
    }

    private void discoverThermostats()
            throws OmniInvalidResponseException, OmniUnknownMessageTypeException, BridgeOfflineException {

        for (AreaProperties areaProperties : areas) {
            int areaFilter = bitFilterForArea(areaProperties);

            ObjectPropertyRequest objectPropertyRequest = ObjectPropertyRequest
                    .builder(bridgeHandler, Message.OBJ_TYPE_THERMO).selectNamed().areaFilter(areaFilter).build();

            for (ObjectProperties objectProperties : objectPropertyRequest) {

                ThingUID thingUID = new ThingUID(OmnilinkBindingConstants.THING_TYPE_THERMOSTAT,
                        Integer.toString(objectProperties.getNumber()));

                Map<String, Object> properties = new HashMap<>();
                properties.put(OmnilinkBindingConstants.THING_PROPERTIES_NUMBER, objectProperties.getNumber());
                properties.put(OmnilinkBindingConstants.THING_PROPERTIES_NAME, objectProperties.getName());
                properties.put(OmnilinkBindingConstants.THING_PROPERTIES_AREA, areaProperties.getNumber());

                DiscoveryResult discoveryResult = DiscoveryResultBuilder.create(thingUID).withProperties(properties)
                        .withBridge(this.bridgeHandler.getThing().getUID()).withLabel(objectProperties.getName())
                        .build();
                thingDiscovered(discoveryResult);
            }
        }
    }

    private List<AreaProperties> discoverAreas()
            throws OmniInvalidResponseException, OmniUnknownMessageTypeException, BridgeOfflineException {

        ObjectPropertyRequest objectPropertyRequest = ObjectPropertyRequest
                .builder(bridgeHandler, Message.OBJ_TYPE_AREA).build();

        List<AreaProperties> areas = new LinkedList<>();

        for (ObjectProperties objectProperties : objectPropertyRequest) {

            AreaProperties areaProperties = ((AreaProperties) objectProperties);
            int objnum = areaProperties.getNumber();

            // it seems that simple configurations of an omnilink have 1 area, without a name. So if there is no name
            // for
            // the first area, we will call that Main. If other areas name is blank, we will not create a thing
            String areaName = areaProperties.getName();
            if (areaProperties.getNumber() == 1 && "".equals(areaName)) {
                areaName = "Main Area";
            } else if ("".equals(areaName)) {
                break;
            }

            Map<String, Object> properties = new HashMap<>();
            ThingUID thingUID;
            if (isLumina) {
                thingUID = new ThingUID(OmnilinkBindingConstants.THING_TYPE_LUMINA_AREA,
                        bridgeHandler.getThing().getUID(), Integer.toString(objnum));
            } else {
                thingUID = new ThingUID(OmnilinkBindingConstants.THING_TYPE_OMNI_AREA,
                        bridgeHandler.getThing().getUID(), Integer.toString(objnum));
            }
            properties.put(OmnilinkBindingConstants.THING_PROPERTIES_NUMBER, objnum);
            properties.put(OmnilinkBindingConstants.THING_PROPERTIES_NAME, areaName);

            DiscoveryResult discoveryResult = DiscoveryResultBuilder.create(thingUID).withProperties(properties)
                    .withBridge(this.bridgeHandler.getThing().getUID()).withLabel(areaName).build();
            thingDiscovered(discoveryResult);
            areas.add(areaProperties);
        }
        return areas;
    }

    private void discoverUnits()
            throws OmniInvalidResponseException, OmniUnknownMessageTypeException, BridgeOfflineException {
        ThingUID bridgeUID = this.bridgeHandler.getThing().getUID();
        // Group Lights_GreatRoom "Great Room" (Lights)
        // String groupString = "Group\t%s\t\"%s\"\t(%s)\n";

        // Dimmer Lights_GreatRoom_MainLights_Switch "Main Lights [%d%%]" (Lights_GreatRoom) {omnilink="unit:10"}
        // String itemString = "%s\t%s\t\"%s\"\t(%s)\t{omnilink=\"unit:%d\"}\n";

        // String groupName = "Lights";

        for (AreaProperties areaProperties : areas) {
            int areaFilter = bitFilterForArea(areaProperties);

            ObjectPropertyRequest objectPropertyRequest = ObjectPropertyRequest
                    .builder(bridgeHandler, Message.OBJ_TYPE_UNIT).selectNamed().areaFilter(areaFilter).selectAnyLoad()
                    .build();

            for (ObjectProperties objectProperties : objectPropertyRequest) {

                UnitProperties unitProperties = ((UnitProperties) objectProperties);
                int objnum = unitProperties.getNumber();
                int currentRoom = 0;
                String currentRoomName = "";

                // boolean isInRoom = false;
                boolean isRoomController = false;
                logger.debug("Unit type: {}", unitProperties.getUnitType());
                if (unitProperties.getUnitType() == UnitProperties.UNIT_TYPE_HLC_ROOM
                        || unitProperties.getObjectType() == UnitProperties.UNIT_TYPE_VIZIARF_ROOM) {
                    currentRoom = objnum;
                    currentRoomName = unitProperties.getName();
                    isRoomController = true;
                } else if (objnum < currentRoom + 8) {
                    // isInRoom = true;
                }

                String thingLabel = unitProperties.getName();
                String thingID = Integer.toString(objnum);

                Map<String, Object> properties = new HashMap<>();

                properties.put(OmnilinkBindingConstants.THING_PROPERTIES_NUMBER, objnum);
                properties.put(OmnilinkBindingConstants.THING_PROPERTIES_NAME, unitProperties.getName());
                properties.put(OmnilinkBindingConstants.THING_PROPERTIES_AREA, areaProperties.getNumber());

                Optional<DiscoveryResult> discoveryResult = Optional.empty();
                if (isRoomController) {
                    discoveryResult = Optional.of(DiscoveryResultBuilder
                            .create(new ThingUID(OmnilinkBindingConstants.THING_TYPE_ROOM,
                                    bridgeHandler.getThing().getUID(), thingID))
                            .withProperties(properties).withBridge(bridgeUID).withLabel(thingLabel).build());
                } else {
                    ThingUID thingUID = null;
                    if (unitProperties.getUnitType() == UnitProperties.UNIT_TYPE_FLAG) {
                        thingUID = new ThingUID(OmnilinkBindingConstants.THING_TYPE_FLAG,
                                bridgeHandler.getThing().getUID(), thingID);

                    } else {
                        if (unitProperties.getUnitType() == UnitProperties.UNIT_TYPE_UPB
                                || unitProperties.getUnitType() == UnitProperties.UNIT_TYPE_HLC_LOAD) {
                            thingUID = new ThingUID(OmnilinkBindingConstants.THING_TYPE_UNIT_UPB,
                                    bridgeHandler.getThing().getUID(), thingID);
                        } else {
                            logger.debug("Unsupported unit type: {}", unitProperties.getUnitType());
                        }
                        // let's prepend room name to unit name for label
                        // TODO could make this configurable
                        thingLabel = currentRoomName + ": " + unitProperties.getName();
                    }
                    if (thingUID != null) {
                        discoveryResult = Optional.of(DiscoveryResultBuilder.create(thingUID).withProperties(properties)
                                .withBridge(bridgeUID).withLabel(thingLabel).build());
                    }
                }
                if (discoveryResult.isPresent()) {
                    thingDiscovered(discoveryResult.get());
                }
            }
        }
    }

    /**
     * Generates zone items
     *
     * @throws IOException
     * @throws OmniNotConnectedException
     * @throws OmniInvalidResponseException
     * @throws OmniUnknownMessageTypeException
     * @throws BridgeOfflineException
     */
    private void discoverZones()
            throws OmniInvalidResponseException, OmniUnknownMessageTypeException, BridgeOfflineException {
        ThingUID bridgeUID = this.bridgeHandler.getThing().getUID();
        // String groupString = "Group\t%s\t\"%s\"\t(%s)\n";
        // String itemString = "%s\t%s\t\"%s\"\t(%s)\t{omnilink=\"%s:%d\"}\n";
        // String groupName = "Zones";

        for (AreaProperties areaProperties : areas) {

            int areaFilter = bitFilterForArea(areaProperties);

            ObjectPropertyRequest objectPropertyRequest = ObjectPropertyRequest
                    .builder(bridgeHandler, Message.OBJ_TYPE_ZONE).selectNamed().areaFilter(areaFilter).build();

            for (ObjectProperties objectProperties : objectPropertyRequest) {

                ZoneProperties zoneProperties = ((ZoneProperties) objectProperties);
                int objnum = zoneProperties.getNumber();

                ThingUID thingUID = null;
                String thingID = Integer.toString(objnum);
                String thingLabel = zoneProperties.getName();

                Map<String, Object> properties = new HashMap<>();
                thingUID = new ThingUID(OmnilinkBindingConstants.THING_TYPE_ZONE, bridgeHandler.getThing().getUID(),
                        thingID);
                properties.put(OmnilinkBindingConstants.THING_PROPERTIES_NUMBER, objnum);
                properties.put(OmnilinkBindingConstants.THING_PROPERTIES_NAME, thingLabel);
                properties.put(OmnilinkBindingConstants.THING_PROPERTIES_AREA, areaProperties.getNumber());

                DiscoveryResult discoveryResult = DiscoveryResultBuilder.create(thingUID).withProperties(properties)
                        .withBridge(bridgeUID).withLabel(thingLabel).build();
                thingDiscovered(discoveryResult);
            }
        }
    }

}

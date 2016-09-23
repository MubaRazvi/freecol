/**
 *  Copyright (C) 2002-2016   The FreeCol Team
 *
 *  This file is part of FreeCol.
 *
 *  FreeCol is free software: you can redistribute it and/or modify
 *  it under the terms of the GNU General Public License as published by
 *  the Free Software Foundation, either version 2 of the License, or
 *  (at your option) any later version.
 *
 *  FreeCol is distributed in the hope that it will be useful,
 *  but WITHOUT ANY WARRANTY; without even the implied warranty of
 *  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *  GNU General Public License for more details.
 *
 *  You should have received a copy of the GNU General Public License
 *  along with FreeCol.  If not, see <http://www.gnu.org/licenses/>.
 */
package net.sf.freecol.common.networking;

import net.sf.freecol.common.model.Colony;
import net.sf.freecol.common.model.Game;
import net.sf.freecol.common.model.Player;
import net.sf.freecol.common.model.Unit;
import net.sf.freecol.common.model.WorkLocation;
import net.sf.freecol.server.FreeColServer;
import net.sf.freecol.server.model.ServerPlayer;

import org.w3c.dom.Element;

import java.util.Objects;


/**
 * The message sent to handle changes in work location.
 */
public class WorkMessage extends DOMMessage {

    public static final String TAG = "work";
    private static final String UNIT_TAG = "unit";
    private static final String WORK_LOCATION_TAG = "workLocation";

    /** The identifier of the unit. */
    private final String unitId;

    /** The identifier of the work location.  */
    private final String workLocationId;


    /**
     * Create a new {@code WorkMessage} for the supplied unit and
     * work location.
     *
     * @param unit The {@code Unit} to change the work location of.
     * @param workLocation The {@code WorkLocation} to change to.
     */
    public WorkMessage(Unit unit, WorkLocation workLocation) {
        super(getTagName());

        this.unitId = unit.getId();
        this.workLocationId = workLocation.getId();
    }

    /**
     * Create a new {@code WorkMessage} from a supplied element.
     *
     * @param game The {@code Game} this message belongs to.
     * @param element The {@code Element} to use to create the message.
     */
    public WorkMessage(Game game, Element element) {
        super(getTagName());

        this.unitId = getStringAttribute(element, UNIT_TAG);
        this.workLocationId = getStringAttribute(element, WORK_LOCATION_TAG);
    }


    /**
     * Handle a "work"-message.
     *
     * @param server The {@code FreeColServer} handling the message.
     * @param player The {@code Player} the message applies to.
     * @param connection The {@code Connection} message received on.
     * @return An update encapsulating the work location change or an
     *     error {@code Element} on failure.
     */
    public Element handle(FreeColServer server, Player player,
                          Connection connection) {
        final ServerPlayer serverPlayer = server.getPlayer(connection);
        final Game game = server.getGame();

        Unit unit;
        try {
            unit = player.getOurFreeColGameObject(this.unitId, Unit.class);
        } catch (RuntimeException e) {
            return serverPlayer.clientError(e.getMessage())
                .build(serverPlayer);
        }

        if (!unit.hasTile()) {
            return serverPlayer.clientError("Unit is not on the map: "
                + this.unitId)
                .build(serverPlayer);
        }

        Colony colony = unit.getTile().getColony();
        if (colony == null) {
            return serverPlayer.clientError("Unit is not at a colony: "
                + this.unitId)
                .build(serverPlayer);
        }

        WorkLocation workLocation = game
            .getFreeColGameObject(this.workLocationId, WorkLocation.class);
        if (workLocation == null) {
            return serverPlayer.clientError("Not a work location: "
                + this.workLocationId)
                .build(serverPlayer);
        } else if (!Objects.equals(workLocation.getColony(), colony)) {
            return serverPlayer.clientError("Work location is not in colony"
                + colony.getId() + " where the unit is: "
                + this.workLocationId)
                .build(serverPlayer);
        } else if (!workLocation.canAdd(unit)) {
            return serverPlayer.clientError("Can not add " + unit
                + " to " + workLocation
                + ": " + workLocation.getNoAddReason(unit))
                .build(serverPlayer);
        }

        // Work.
        return server.getInGameController()
            .work(serverPlayer, unit, workLocation)
            .build(serverPlayer);
    }

    /**
     * Convert this WorkMessage to XML.
     *
     * @return The XML representation of this message.
     */
    @Override
    public Element toXMLElement() {
        return new DOMMessage(getTagName(),
            UNIT_TAG, unitId,
            WORK_LOCATION_TAG, workLocationId).toXMLElement();
    }

    /**
     * The tag name of the root element representing this object.
     *
     * @return "work".
     */
    public static String getTagName() {
        return TAG;
    }
}

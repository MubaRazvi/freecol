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

import net.sf.freecol.common.model.Game;
import net.sf.freecol.common.model.Direction;
import net.sf.freecol.common.model.Player;
import net.sf.freecol.common.model.Tile;
import net.sf.freecol.common.model.Unit;
import net.sf.freecol.common.model.Unit.MoveType;
import net.sf.freecol.server.FreeColServer;
import net.sf.freecol.server.model.ServerPlayer;
import net.sf.freecol.server.model.ServerUnit;

import org.w3c.dom.Element;


/**
 * The message sent when moving a unit.
 */
public class MoveMessage extends DOMMessage {

    public static final String TAG = "move";
    private static final String DIRECTION_TAG = "direction";
    private static final String UNIT_TAG = "unit";

    /** The identifier of the object to be moved. */
    private final String unitId;

    /** The direction to move. */
    private final String directionString;


    /**
     * Create a new {@code MoveMessage} for the supplied unit and
     * direction.
     *
     * @param unit The {@code Unit} to move.
     * @param direction The {@code Direction} to move in.
     */
    public MoveMessage(Unit unit, Direction direction) {
        super(getTagName());

        this.unitId = unit.getId();
        this.directionString = String.valueOf(direction);
    }

    /**
     * Create a new {@code MoveMessage} from a
     * supplied element.
     *
     * @param game The {@code Game} this message belongs to.
     * @param element The {@code Element} to use to create the message.
     */
    public MoveMessage(Game game, Element element) {
        super(getTagName());

        this.unitId = getStringAttribute(element, UNIT_TAG);
        this.directionString = getStringAttribute(element, DIRECTION_TAG);
    }


    /**
     * Handle a "move"-message.
     *
     * @param server The {@code FreeColServer} handling the message.
     * @param player The {@code Player} the message applies to.
     * @param connection The {@code Connection} message was received on.
     * @return An update containing the moved unit, or an error
     *     {@code Element} on failure.
     */
    public Element handle(FreeColServer server, Player player,
                          Connection connection) {
        final ServerPlayer serverPlayer = server.getPlayer(connection);

        ServerUnit unit;
        try {
            unit = player.getOurFreeColGameObject(this.unitId, ServerUnit.class);
        } catch (RuntimeException e) {
            return serverPlayer.clientError(e.getMessage())
                .build(serverPlayer);
        }

        Tile tile;
        try {
            tile = unit.getNeighbourTile(this.directionString);
        } catch (RuntimeException e) {
            return serverPlayer.clientError(e.getMessage())
                .build(serverPlayer);
        }

        MoveType moveType = unit.getMoveType(tile);
        if (!moveType.isProgress()) {
            return serverPlayer.clientError("Illegal move for: " + this.unitId
                + " type: " + moveType
                + " from: " + unit.getLocation().getId()
                + " to: " + tile.getId())
                .build(serverPlayer);
        }

        // Proceed to move.
        return server.getInGameController()
            .move(serverPlayer, unit, tile)
            .build(serverPlayer);
    }

    /**
     * Convert this MoveMessage to XML.
     *
     * @return The XML representation of this message.
     */
    @Override
    public Element toXMLElement() {
        return new DOMMessage(getTagName(),
            UNIT_TAG, this.unitId,
            DIRECTION_TAG, this.directionString).toXMLElement();
    }

    /**
     * The tag name of the root element representing this object.
     *
     * @return "move".
     */
    public static String getTagName() {
        return TAG;
    }
}

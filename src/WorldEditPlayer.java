// $Id$
/*
 * WorldEditLibrary
 * Copyright (C) 2010 sk89q <http://www.sk89q.com>
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program. If not, see <http://www.gnu.org/licenses/>.
*/

import com.sk89q.worldedit.Vector;
import com.sk89q.worldedit.blocks.BlockType;

/**
 *
 * @author sk89q
 */
public class WorldEditPlayer {
    /**
     * Stores the player.
     */
    private Player player;

    /**
     * Construct the object.
     * 
     * @param player
     */
    public WorldEditPlayer(Player player) {
        this.player = player;
    }

    /**
     * Returns true if the player is holding a pick axe.
     *
     * @return whether a pick axe is held
     */
    public boolean isHoldingPickAxe() {
        int item = getItemInHand();
        return item == 257 || item == 270 || item == 274 || item == 278
                || item == 285;
    }

    /**
     * Move the player.
     *
     * @param pos
     */
    public void setPosition(Vector pos) {
        setPosition(pos, (float)getPitch(), (float)getYaw());
    }

    /**
     * Find a position for the player to stand that is not inside a block.
     * Blocks above the player will be iteratively tested until there is
     * a series of two free blocks. The player will be teleported to
     * that free position.
     *
     * @param searchPos search position
     */
    public void findFreePosition(Vector searchPos) {
        int x = searchPos.getBlockX();
        int y = Math.max(0, searchPos.getBlockY());
        int origY = y;
        int z = searchPos.getBlockZ();

        byte free = 0;

        while (y <= 129) {
            if (BlockType.canPassThrough(etc.getServer().getBlockIdAt(x, y, z))) {
                free++;
            } else {
                free = 0;
            }

            if (free == 2) {
                if (y - 1 != origY) {
                    setPosition(new Vector(x + 0.5, y - 1, z + 0.5));
                }

                return;
            }

            y++;
        }
    }

    /**
     * Find a position for the player to stand that is not inside a block.
     * Blocks above the player will be iteratively tested until there is
     * a series of two free blocks. The player will be teleported to
     * that free position.
     */
    public void findFreePosition() {
        findFreePosition(getBlockIn());
    }

    /**
     * Go up one level to the next free space above.
     *
     * @return true if a spot was found
     */
    public boolean ascendLevel() {
        Vector pos = getBlockIn();
        int x = pos.getBlockX();
        int y = Math.max(0, pos.getBlockY());
        int z = pos.getBlockZ();

        byte free = 0;
        byte spots = 0;

        while (y <= 129) {
            if (BlockType.canPassThrough(ServerInterface.getBlockType(new Vector(x, y, z)))) {
                free++;
            } else {
                free = 0;
            }

            if (free == 2) {
                spots++;
                if (spots == 2) {
                    int type = ServerInterface.getBlockType(new Vector(x, y - 2, z));
                    
                    // Don't get put in lava!
                    if (type == 10 || type == 11) {
                        return false;
                    }

                    setPosition(new Vector(x + 0.5, y - 1, z + 0.5));
                    return true;
                }
            }

            y++;
        }

        return false;
    }

    /**
     * Go up one level to the next free space above.
     *
     * @return true if a spot was found
     */
    public boolean descendLevel() {
        Vector pos = getBlockIn();
        int x = pos.getBlockX();
        int y = Math.max(0, pos.getBlockY() - 1);
        int z = pos.getBlockZ();

        byte free = 0;

        while (y >= 1) {
            if (BlockType.canPassThrough(ServerInterface.getBlockType(new Vector(x, y, z)))) {
                free++;
            } else {
                free = 0;
            }

            if (free == 2) {
                // So we've found a spot, but we have to drop the player
                // lightly and also check to see if there's something to
                // stand upon
                while (y >= 0) {
                    int type = ServerInterface.getBlockType(new Vector(x, y, z));

                    // Don't want to end up in lava
                    if (type != 0 && type != 10 && type != 11) {
                        // Found a block!
                        setPosition(new Vector(x + 0.5, y + 1, z + 0.5));
                        return true;
                    }
                    
                    y--;
                }

                return false;
            }

            y--;
        }

        return false;
    }

    /**
     * Ascend to the ceiling above.
     * 
     * @param clearance
     * @return whether the player was moved
     */
    public boolean ascendToCeiling(int clearance) {
        Vector pos = getBlockIn();
        int x = pos.getBlockX();
        int initialY = Math.max(0, pos.getBlockY());
        int y = Math.max(0, pos.getBlockY() + 2);
        int z = pos.getBlockZ();
        
        // No free space above
        if (ServerInterface.getBlockType(new Vector(x, y, z)) != 0) {
            return false;
        }

        while (y <= 127) {
            // Found a ceiling!
            if (!BlockType.canPassThrough(ServerInterface.getBlockType(new Vector(x, y, z)))) {
                int platformY = Math.max(initialY, y - 3 - clearance);
                ServerInterface.setBlockType(new Vector(x, platformY, z),
                        BlockType.GLASS.getID());
                setPosition(new Vector(x + 0.5, platformY + 1, z + 0.5));
                return true;
            }

            y++;
        }

        return false;
    }

    /**
     * Just go up.
     *
     * @param distance
     * @return whether the player was moved
     */
    public boolean ascendUpwards(int distance) {
        Vector pos = getBlockIn();
        int x = pos.getBlockX();
        int initialY = Math.max(0, pos.getBlockY());
        int y = Math.max(0, pos.getBlockY() + 1);
        int z = pos.getBlockZ();
        int maxY = Math.min(128, initialY + distance);

        while (y <= 129) {
            if (!BlockType.canPassThrough(ServerInterface.getBlockType(new Vector(x, y, z)))) {
                break; // Hit something
            } else if (y > maxY + 1) {
                break;
            } else if (y == maxY + 1) {
                ServerInterface.setBlockType(new Vector(x, y - 2, z),
                        BlockType.GLASS.getID());
                setPosition(new Vector(x + 0.5, y - 1, z + 0.5));
                return true;
            }

            y++;
        }

        return false;
    }

    /**
     * Returns true if equal.
     *
     * @param other
     * @return whether the other object is equivalent
     */
    @Override
    public boolean equals(Object other) {
        if (!(other instanceof WorldEditPlayer)) {
            return false;
        }
        WorldEditPlayer other2 = (WorldEditPlayer)other;
        return other2.getName().equals(getName());
    }

    /**
     * Gets the hash code.
     *
     * @return hash code
     */
    @Override
    public int hashCode() {
        return getName().hashCode();
    }

    /**
     * Get the point of the block that is being stood in.
     *
     * @return point
     */
    public Vector getBlockIn() {
        return Vector.toBlockPoint(player.getX(), player.getY(), player.getZ());
    }

    /**
     * Get the point of the block that is being stood upon.
     *
     * @return point
     */
    public Vector getBlockOn() {
        return Vector.toBlockPoint(player.getX(), player.getY() - 1, player.getZ());
    }

    /**
     * Get the point of the block being looked at. May return null.
     *
     * @param range
     * @return point
     */
    public Vector getBlockTrace(int range) {
        HitBlox hitBlox = new HitBlox(player, range, 0.2);
        Block block = hitBlox.getTargetBlock();
        if (block == null) {
            return null;
        }
        return new Vector(block.getX(), block.getY(), block.getZ());
    }

    /**
     * Get the point of the block being looked at. May return null.
     *
     * @param range
     * @return point
     */
    public Vector getSolidBlockTrace(int range) {
        HitBlox hitBlox = new HitBlox(player, range, 0.2);
        Block block = null;

        while (hitBlox.getNextBlock() != null
                && BlockType.canPassThrough(hitBlox.getCurBlock().getType()));

        block = hitBlox.getCurBlock();

        if (block == null) {
            return null;
        }
        return new Vector(block.getX(), block.getY(), block.getZ());
    }

    /**
     * Get the player's cardinal direction (N, W, NW, etc.).
     *
     * @return
     */
    public String getCardinalDirection() {
        // From hey0's code
        double rot = (getYaw() - 90) % 360;
        if (rot < 0) {
            rot += 360.0;
        }
        return etc.getCompassPointForDirection(rot).toLowerCase();
    }

    /**
     * Get the ID of the item that the player is holding.
     *
     * @return
     */
    /**
     * Get the ID of the item that the player is holding.
     *
     * @return
     */
    public int getItemInHand() {
        return player.getItemInHand();
    }

    /**
     * Get the name of the player.
     *
     * @return String
     */
    public String getName() {
        return player.getName();
    }

    /**
     * Get the player's view pitch.
     *
     * @return pitch
     */
    /**
     * Get the player's view pitch.
     *
     * @return pitch
     */
    public double getPitch() {
        return player.getPitch();
    }

    /**
     * Get the player's position.
     *
     * @return point
     */
    public Vector getPosition() {
        return new Vector(player.getX(), player.getY(), player.getZ());
    }

    /**
     * Get the player's view yaw.
     *
     * @return yaw
     */
    /**
     * Get the player's view yaw.
     *
     * @return yaw
     */
    public double getYaw() {
        return player.getRotation();
    }

    /**
     * Gives the player an item.
     *
     * @param type
     * @param amt
     */
    /**
     * Gives the player an item.
     *
     * @param type
     * @param amt
     */
    public void giveItem(int type, int amt) {
        player.giveItem(type, amt);
    }

    /**
     * Pass through the wall that you are looking at.
     *
     * @param range
     * @return whether the player was pass through
     */
    public boolean passThroughForwardWall(int range) {
        boolean foundNext = false;
        int searchDist = 0;
        HitBlox hitBlox = new HitBlox(player, range, 0.2);
        Block block;
        while ((block = hitBlox.getNextBlock()) != null) {
            searchDist++;
            if (searchDist > 20) {
                return false;
            }
            if (block.getType() == 0) {
                if (foundNext) {
                    Vector v = new Vector(block.getX(), block.getY() - 1, block.getZ());
                    if (ServerInterface.getBlockType(v) == 0) {
                        setPosition(v.add(0.5, 0, 0.5));
                        return true;
                    }
                }
            } else {
                foundNext = true;
            }
        }
        return false;
    }

    /**
     * Print a WorldEdit message.
     *
     * @param msg
     */
    public void print(String msg) {
        player.sendMessage(Colors.LightPurple + msg);
    }

    /**
     * Print a WorldEdit error.
     *
     * @param msg
     */
    public void printError(String msg) {
        player.sendMessage(Colors.Rose + msg);
    }

    /**
     * Move the player.
     *
     * @param pos
     * @param pitch
     * @param yaw
     */
    public void setPosition(Vector pos, float pitch, float yaw) {
        Location loc = new Location();
        loc.x = pos.getX();
        loc.y = pos.getY();
        loc.z = pos.getZ();
        loc.rotX = (float) yaw;
        loc.rotY = (float) pitch;
        player.teleportTo(loc);
    }

    /**
     * Get a player's list of groups.
     * 
     * @return
     */
    public String[] getGroups() {
        return player.getGroups();
    }
}

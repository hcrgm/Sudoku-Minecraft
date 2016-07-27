package net.windit.mcpl.sudoku;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.configuration.serialization.ConfigurationSerializable;
import sudoku.SudokuBoard;

import java.awt.*;
import java.util.*;
import java.util.List;

import static net.windit.mcpl.sudoku.Main.config;

/**
 * Mapping Minecraft's block to Sudoku's point. / Board
 * TODO: Developing...
 */
public class BoardPoint implements ConfigurationSerializable {
    private String worldName;
    private int startX;
    private int startY;
    private int startZ;
    private int endX;
    private int endY;
    private int endZ;

    public BoardPoint(String worldName, int startX, int startY, int startZ, int endX, int endY, int endZ) {
        this.worldName = worldName;
        this.startX = Math.max(startX, endX);
        this.endX = Math.min(startX, endX);
        this.startY = Math.max(startY, endY);
        this.endY = Math.min(startY, endY);
        this.startZ = Math.max(startZ, endZ);
        this.endZ = Math.min(startZ, endZ);
    }

    public BoardPoint(Location l1, Location l2) throws IllegalArgumentException {
        if (!l1.getWorld().getName().equals(l2.getWorld().getName()))
            throw new IllegalArgumentException("Locations must be on the same world");
        this.worldName = l1.getWorld().getName();
        this.startX = Math.min(l1.getBlockX(), l2.getBlockX());
        this.endX = Math.max(l1.getBlockX(), l2.getBlockX());
        this.startY = Math.max(l1.getBlockY(), l2.getBlockY());
        this.endY = Math.min(l1.getBlockY(), l2.getBlockY());
        this.startZ = Math.min(l1.getBlockZ(), l2.getBlockZ());
        this.endZ = Math.max(l1.getBlockZ(), l2.getBlockZ());
    }

    public BoardPoint(Map<String, Object> map) {
        this.worldName = (String) map.get("worldName");
        this.startX = (Integer) map.get("startX");
        this.startY = (Integer) map.get("startY");
        this.startZ = (Integer) map.get("startZ");
        this.endX = (Integer) map.get("endX");
        this.endY = (Integer) map.get("endY");
        this.endZ = (Integer) map.get("endZ");
    }

    @Deprecated
    public Block getBlockAtPoint(Point point) throws IllegalArgumentException {
        int x = (int) point.getX();
        int y = (int) point.getY();
        World world = Bukkit.getWorld(worldName);
        if ((x > 8 || y > 8) || (x < 0 || y < 0))
            throw new IllegalArgumentException("x and y must be less than or equal to 8, more than or equal to 0");
        if (startY == endY) {
            if (startX < endX) {
                if (startZ < endZ) {
                    if (config.debug) System.out.println("1sz<ez" + (startX + x) + "," + startY + "," + (startZ + y));
                    return world.getBlockAt(startX + x, startY, startZ + y); //// TODO: 16-7-17 Not check yet
                } else {
                    if (config.debug) System.out.println("2sz>ez" + (startX + y) + "," + startY + "," + (startZ - x));
                    return world.getBlockAt(startX + y, startY, startZ - x); //// TODO: 16-7-16 Not check yet
                }
            } else {
                if (startZ > endZ) {
                    if (config.debug) System.out.println("3sz>ez" + (startX - x) + "," + startY + "," + (startZ - y));
                    return world.getBlockAt(startX - x, startY, startZ - y); //// TODO: 16-7-16 Not check yet
                } else {
                    if (config.debug) System.out.println("4sz<ez" + (startX - y) + "," + startY + "," + (startZ + x));
                    return world.getBlockAt(startX - y, startY, startZ + x); //// TODO: 16-7-16 Not check yet
                }
            }
        } else if (startX == endX) {
            if (startZ > endZ) {
                return world.getBlockAt(startX, startY - y, startZ - x);
            } else {
                return world.getBlockAt(startX, startY - y, startZ + x);
            }
        } else if (startZ == endZ) {
            if (startX > endX) {
                return world.getBlockAt(startX - x, startY - y, startZ);
            } else {
                return world.getBlockAt(startX + x, startY - y, startZ);
            }
        }
        throw new IllegalArgumentException("Input is invalid. Is it a square?");
    }

    public Block getBlockAtPoint(int x, int y, SudokuBoard board) {
        return getBlocks().get(board.pointToCell(x, y));
    }

    public Block getBlockAtPoint(Point point, SudokuBoard board) {
        return getBlockAtPoint((int) point.getX(), (int) point.getY(), board);
    }

    public List<Block> getBlocks() {
        Iterator<Block> iterator = iterator();
        List<Block> blocks = new ArrayList<>();
        while (iterator.hasNext())
            blocks.add(iterator.next());
        return blocks;
    }

    public boolean contains(int x, int y, int z) {
        return x >= this.startX && x <= this.endX && y <= this.startY && y >= this.endY && z >= this.startZ && z <= this.endZ;
    }

    public boolean contains(Location location) {
        return this.worldName.equals(location.getWorld().getName()) && contains(location.getBlockX(), location.getBlockY(), location.getBlockZ());
    }

    @Override
    public Map<String, Object> serialize() {
        Map<String, Object> map = new HashMap<>();
        map.put("worldName", worldName);
        map.put("startX", startX);
        map.put("startY", startY);
        map.put("startZ", startZ);
        map.put("endX", endX);
        map.put("endY", endY);
        map.put("endZ", endZ);
        return map;
    }

    public Iterator<Block> iterator() {
        return new BoardIterator(getWorld(), startX, startY, startZ, endX, endY, endZ);
    }

    public World getWorld() throws IllegalStateException {
        World world = Bukkit.getWorld(worldName);
        if (world == null) throw new IllegalStateException("World '" + worldName + "' is not loaded");
        return world;
    }

    /**
     * Gets all the blocks in this board.
     */
    public class BoardIterator implements Iterator<Block> {

        private World world;
        private int baseX, baseY, baseZ, sizeX, sizeY, sizeZ, x, y, z;

        public BoardIterator(World world, int startX, int startY, int startZ, int endX, int endY, int endZ) {
            this.world = world;
            this.baseX = startX;
            this.baseY = startY;
            this.baseZ = startZ;
            this.sizeX = Math.abs(startX - endX) + 1;
            this.sizeY = Math.abs(startY - endY) + 1;
            this.sizeZ = Math.abs(startZ - endZ) + 1;
        }

        @Override
        public boolean hasNext() {
            return this.x < this.sizeX && this.y < this.sizeY && this.z < this.sizeZ;
        }

        @Override
        public Block next() {
            Block block = world.getBlockAt(this.baseX + this.x, this.baseY - this.y, this.baseZ + this.z);
            if (++x >= this.sizeX) {
                this.x = 0;
                if (++this.y >= this.sizeY) {
                    this.y = 0;
                    ++this.z;
                }
            }
            return block;
        }

        @Override
        public void remove() {
            throw new UnsupportedOperationException("Doesn't support block removal");
        }
    }
}

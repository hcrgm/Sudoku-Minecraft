package net.windit.mcpl.sudoku;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.block.Sign;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.AsyncPlayerChatEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.scheduler.BukkitTask;
import sudoku.SudokuBoard;
import sudoku.backtrack.BacktrackSolver;
import sudoku.generate.DeductionGenerator;
import sudoku.generate.SudokuGenerator;

import java.awt.*;
import java.io.IOException;
import java.util.*;
import java.util.List;
import java.util.logging.Level;

import static net.windit.mcpl.sudoku.Main.config;
import static net.windit.mcpl.sudoku.Main.logger;

public class EventListener implements Listener {
    private final Main plugin;
    private Player playingPlayer;
    private SudokuBoard sudokuBoard;
    private SudokuBoard solvedBoard;
    private SudokuBoard initialBoard;
    private int stage;
    private Player settingPlayer;
    private BukkitTask restoreTask;
    private Location topLeftWoolLoc;
    private Location bottomRightWoolLoc;
    private GameState state;
    private Block selectedWool;
    private Point selectedPoint;
    private boolean generating;
    private Date lastGenerate;

    public EventListener(Main plugin) {
        this.plugin = plugin;
    }

    @EventHandler
    public void onPlayerInteract(final PlayerInteractEvent event) {
        if (!config.alreadySet && settingPlayer == null) return;
        Player player = event.getPlayer();
        Block block = event.getClickedBlock();
        if (event.getClickedBlock() == null) return;
        Location location = block.getLocation();
        if (settingPlayer != null && player.equals(settingPlayer)) { // setting mode
            if (stage == 0 && block.getType() == Material.WOOL) {
                topLeftWoolLoc = location;
                stage = 1;
                player.sendMessage("§d左上角羊毛已选.下一步:选择右下角羊毛.");
                return;
            }
            if (stage == 1 && block.getType() == Material.WOOL) {
                bottomRightWoolLoc = location;
                stage = 2;
                player.sendMessage("§d右下角羊毛已选.下一步:选择用于开始/重新开始游戏的木牌.");
                return;
            }
            if (stage == 2 && block.getState() instanceof Sign) {
                config.sign1 = location;
                stage = 3;
                Sign sign = (Sign) block.getState();
                sign.setLine(0, "§a§l[数独](重新)开始");
                sign.update();
                player.sendMessage("§d木牌1已选.下一步:选择用于验证数独的木牌.");
                return;
            }
            if (stage == 3 && block.getState() instanceof Sign) {
                config.sign2 = location;
                stage = 4;
                Sign sign = (Sign) block.getState();
                sign.setLine(0, "§a§l[数独]验证");
                sign.update();
                player.sendMessage("§d木牌2已选.最后一步:选择用于结束游戏的木牌.");
                return;
            }
            if (stage == 4 && block.getState() instanceof Sign) {
                config.sign3 = location;
                stage = 0;
                Sign sign = (Sign) block.getState();
                sign.setLine(0, "§a§l[数独]结束游戏");
                sign.update();
                settingPlayer = null;
                config.board = new BoardPoint(topLeftWoolLoc, bottomRightWoolLoc);
                try {
                    config.saveConfig();
                    player.sendMessage("§a设置已完成~");
                } catch (IOException e) {
                    logger.log(Level.SEVERE, "Can't save the config file.", e);
                }
            }
        } else if (block.getState() instanceof Sign) {
            switch (getAction(block, player)) {
                case START:
                    if (generating) {
                        player.sendMessage("§c正在努力生成数独中，别着急......");
                    } else if (lastGenerate != null && ((new Date().getTime() - lastGenerate.getTime()) / 1000) < config.generateCooldown) {
                        player.sendMessage("§c手不要太快了哦");
                    } else {
                        state = GameState.READY;
                        selectedWool = block;
                        player.sendRawMessage("§a请输入你要挑战的难度 (0:简单 1:一般 2:困难 3: 疯狂)");
                    }
                    break;
                case RESTART:
                    restartGame();
                    break;
                case VERIFY:
                    if (restoreTask != null) {
                        player.sendMessage("§c错误结果展示中...别着急");
                    } else {
                        verifySudoku();
                    }
                    break;
                case END:
                    resetGame();
                    player.sendMessage("§d您已结束游戏");
                    break;
            }
        } else if (block.getType() == Material.WOOL && config.board.contains(location)) {
            if (isPlaying(player)) {
                List<Block> blocks = config.board.getBlocks();
                for (int i = 0; i < blocks.size(); i++) {
                    if (blocks.get(i).getLocation().equals(location)) {
                        if (config.debug) System.out.println(sudokuBoard.cellToPoint(i));
                        if (!sudokuBoard.isModifiable(i)) {
                            player.sendMessage("§c那不是一个可编辑的方格!");
                            event.setCancelled(true);
                            break;
                        }
                        selectedWool = blocks.get(i);
                        selectedPoint = sudokuBoard.cellToPoint(i);
                        state = GameState.INPUT;
                        player.sendMessage("§d请输入你要填写的数字.");
                        break;
                    }
                }
            }
        }
    }

    @EventHandler
    @SuppressWarnings("deprecation")
    public void onChat(AsyncPlayerChatEvent event) {
        if (!config.alreadySet) return;
        Player player = event.getPlayer();
        String message = event.getMessage();
        if (state == GameState.READY) {
            if (message.matches("[0-3]")) {
                if (!config.alreadySet) {
                    return;
                }
                if (!canStart(player)) {
                    return;
                }
                startGame(player, Diffculty.getDiffcultyById(Integer.parseInt(message)));
                event.setCancelled(true);
            }
        } else if (state == GameState.INPUT && selectedWool != null && selectedPoint != null) {
            if (message.matches("[0-9]")) {
                if (!config.alreadySet) {
                    return;
                }
                if (!isPlaying(player)) {
                    return;
                }
                selectedWool.setData(Byte.parseByte(message));
                sudokuBoard.setCell(sudokuBoard.pointToCell(selectedPoint), Integer.parseInt(message));
                checkWin(player);
                state = null;
                event.setCancelled(true);
            }
        } else if (message.matches("[1-9],[1-9],[0-9]")) {
            if (!config.alreadySet) {
                return;
            }
            if (!isPlaying(player)) {
                return;
            }
            Iterator<Block> iterator = config.board.iterator();
            if (config.debug) {
                while (iterator.hasNext()) {
                    System.out.println(iterator.next());
                }
            }
            String[] info = message.split(",");
            int x = Integer.parseInt(info[0]) - 1;
            int y = Integer.parseInt(info[1]) - 1;
            byte data = (byte) Integer.parseInt(info[2]);
            if (!sudokuBoard.isModifiable(sudokuBoard.pointToCell(x, y))) {
                player.sendMessage("§c那不是一个可编辑的方格!");
                event.setCancelled(true);
                return;
            }
            Block block = config.board.getBlockAtPoint(x, y, sudokuBoard);
            block.setData(data);
            sudokuBoard.setCell(x, y, data);
            checkWin(player);
            event.setCancelled(true);
        }
    }

    @EventHandler
    public void playerQuit(PlayerQuitEvent event) {
        if (!config.alreadySet) return;
        if (event.getPlayer() == playingPlayer) {
            Bukkit.broadcastMessage("§e玩家:§d" + event.getPlayer().getName() + "§e由于退出而放弃了游戏.");
            resetGame();
        }
    }

    private boolean isPlaying(Player player) {
        return player.equals(playingPlayer);
    }

    public void checkWin(Player player) {
        if (sudokuBoard.isSolved()) {
            player.sendRawMessage("§e» §5恭喜你完成了这个数独!");
            resetGame();
        }
    }

    private boolean canStart(Player player) {
        return playingPlayer == null || isPlaying(player);
    }

    @SuppressWarnings("deprecation")
    private void startGame(final Player player, final Diffculty diff) {
        generating = true;
        Bukkit.getServer().getScheduler().runTaskAsynchronously(plugin, new Runnable() {
            public void run() {
                SudokuGenerator generator = new DeductionGenerator(3, 3);
                final SudokuBoard sudokuBoard = generator.getProblem();
                final SudokuBoard solvedBoard = new BacktrackSolver().solve(sudokuBoard);
                Util.randomFillBoardWithDiffculty(sudokuBoard, solvedBoard, diff);
                EventListener.this.playingPlayer = player;
                EventListener.this.sudokuBoard = sudokuBoard;
                EventListener.this.solvedBoard = solvedBoard;
                EventListener.this.initialBoard = sudokuBoard.clone();
                lastGenerate = new Date();
                Bukkit.getServer().getScheduler().runTask(plugin, new Runnable() {
                    @Override
                    public void run() {
                        for (int i = 0; i < 81; i++) {
                            Block blk = config.board.getBlockAtPoint(sudokuBoard.cellToPoint(i), sudokuBoard);
                            blk.setData((byte) sudokuBoard.getCell(i));
                        }
                        player.sendMessage("§a开始游戏吧~");
                    }
                });
                if (config.debug) {
                    logger.log(Level.INFO, "The generated Sudoku is:\n{0}\nThe solved Sudoku is:\n{1}", new String[]{sudokuBoard.toString(), solvedBoard.toString()});
                }
                state = null;
                generating = false;
            }
        });
    }

    public void resetGame() {
        resetBoard();
        this.sudokuBoard = null;
        this.solvedBoard = null;
        this.initialBoard = null;
        this.playingPlayer = null;
    }

    @SuppressWarnings("deprecation")
    private void restartGame() {
        if (restoreTask != null) {
            restoreTask.cancel();
            restoreTask = null;
        }
        for (int i = 0; i < 81; i++) {
            Block blk = config.board.getBlockAtPoint(sudokuBoard.cellToPoint(i), sudokuBoard);
            blk.setData((byte) initialBoard.getCell(i));
        }
        sudokuBoard = initialBoard.clone();
    }

    @SuppressWarnings("deprecation")
    private void verifySudoku() {
        final Map<Block, Byte> errBlks = new HashMap<>();
        for (int x = 0; x < 9; x++) {
            for (int y = 0; y < 9; y++) {
                if (sudokuBoard.getCell(x, y) != solvedBoard.getCell(x, y)) {
                    config.board.getBlockAtPoint(x, y, sudokuBoard).setData((byte) 14);
                    errBlks.put(config.board.getBlockAtPoint(x, y, sudokuBoard), (byte) sudokuBoard.getCell(x, y));
                }
            }
        }
        restoreTask = Bukkit.getScheduler().runTaskLater(plugin, new Runnable() {
            @Override
            public void run() {
                for (Map.Entry<Block, Byte> entry : errBlks.entrySet()) {
                    entry.getKey().setData(entry.getValue());
                }
                restoreTask = null;
            }
        }, 100L);
    }

    @SuppressWarnings("deprecation")
    private void resetBoard() {
        if (restoreTask != null) {
            restoreTask.cancel();
            restoreTask = null;
        }
        for (int x = 0; x < 9; x++) {
            for (int y = 0; y < 9; y++) {
                config.board.getBlockAtPoint(x, y, sudokuBoard).setData((byte) 0);
            }
        }
    }

    public void startSetting(Player player) {
        this.stage = 0;
        this.settingPlayer = player;
        if (config.board != null && sudokuBoard != null) {
            resetGame();
        }
    }

    private GameAction getAction(Block clickedBlock, Player player) {
        Location loc = clickedBlock.getLocation();
        if (isPlaying(player)) {
            if (loc.equals(config.sign1)) {
                return GameAction.RESTART;
            } else if (loc.equals(config.sign2)) {
                return GameAction.VERIFY;
            } else if (loc.equals(config.sign3)) {
                return GameAction.END;
            }
        } else if (canStart(player) && loc.equals(config.sign1)) {
            return GameAction.START;
        }
        return GameAction.NULL;
    }

    private enum GameAction {
        START, RESTART, END, VERIFY, NULL
    }

    private enum GameState {
        READY, INPUT
    }
}

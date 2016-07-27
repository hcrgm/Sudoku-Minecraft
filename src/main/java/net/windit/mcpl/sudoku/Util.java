package net.windit.mcpl.sudoku;

import sudoku.SudokuBoard;

public class Util {
    public static void randomFillBoardWithDiffculty(SudokuBoard board, SudokuBoard solvedBoard, Diffculty diff) {
        if (diff == Diffculty.CRAZY) return;
        int randomFillNums = 0;
        switch (diff) {
            case HARD:
                randomFillNums = randomWithinRange(5, 15);
                break;
            case MEDIUM:
                randomFillNums = randomWithinRange(16, 35);
                break;
            case EASY:
                randomFillNums = randomWithinRange(36, 45);
                break;
        }
        Integer[] emptyCells = new Integer[board.getEmptyCells().size()];
        board.getEmptyCells().toArray(emptyCells);
        if (emptyCells.length < randomFillNums) {
            randomFillNums = emptyCells.length;
        }
        for (int i = 0; i < randomFillNums; i++) {
            board.setCell(emptyCells[i], solvedBoard.getCell(emptyCells[i]));
            board.setModifiable(emptyCells[i], false);
        }
    }

    public static int randomWithinRange(int min, int max) {
        return (int) Math.round(Math.random() * (max - min) + min);
    }
}

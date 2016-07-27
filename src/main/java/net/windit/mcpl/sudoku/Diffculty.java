package net.windit.mcpl.sudoku;

/**
 * Sudoku's diffculty.
 */
public enum Diffculty {
    EASY, MEDIUM, HARD, CRAZY;

    public static Diffculty getDiffcultyById(int id) {
        switch (id) {
            case 0:
                return EASY;
            case 1:
                return MEDIUM;
            case 2:
                return HARD;
            case 3:
                return CRAZY;
            default:
                return null;
        }
    }
}

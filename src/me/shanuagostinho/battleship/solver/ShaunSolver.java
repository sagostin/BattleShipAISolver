package me.shanuagostinho.battleship.solver;

public class ShaunSolver {
    public static String makeGuess(char[][] guesses) {
        int row, col;
        String dS = diagonalSolve(guesses);
        row = Integer.parseInt(dS.split("-")[0]);
        col = Integer.parseInt(dS.split("-")[1]);

        char a = (char) ((int) 'A' + row);

        String guess = a + Integer.toString(col + 1);
        return guess;
    }

    /*
    TODO:
    - Start to look for boats diagonally, if it finds there is a
    boat that has been found, it will guess around that location.


     */

    public static String diagonalSolve(char[][] guesses) {
        String guess = "0-0";

        int guessSpacing = 4;

        // Center Diagonal

        return guess;
    }
}
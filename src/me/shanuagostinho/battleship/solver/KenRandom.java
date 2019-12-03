package me.shanuagostinho.battleship.solver;

public class KenRandom {
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
        for (int row = 0; row < guesses.length; row++) {
            if (guesses[row][row] == '.') {
                guess = row + "-" + row;
            }
        }

        // Fill in top side
        for (int row = 0; row < guesses.length; row++) {
            for (int col = 0; col < guesses[row].length; col += guessSpacing) {
                int column = (col + guessSpacing) + row;
                if (column < guesses[row].length) {
                    if (guesses[row][column] == '.') {
                        guess = row + "-" + column;
                    }
                }
            }
        }

        // Fill in bottom side
        for (int row = 0; row < guesses.length; row++) {
            for (int col = 0; col < guesses[row].length; col -= guessSpacing) {
                int column = (col - guessSpacing) + row;
                if (column >= 0) {
                    if (column < guesses[row].length) {
                        if (guesses[row][column] == '.') {
                            guess = row + "-" + column;
                        }
                    }
                }
            }
        }

        return guess;
    }
}
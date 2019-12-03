package me.shanuagostinho.battleship.solver;

//Written by Andrew Mitchell
//300310056
public class AndyV3LART {
    public static String makeGuess(char[][] guesses) {
        // if (fixedOpeningIfNoHits(guesses) != 0000) {
        // return (convertToGuess(fixedOpeningIfNoHits(guesses)));   
        // }
        double[][][][] shipMaps = new double[6][4][10][11];
        // shipMaps[i][ ][ ][ ] where i indicates shipNumber (1,2,3,4,5)
        // shipMaps[ ][i][ ][ ] where i indicates Map's state of refinement
        // 0 = basic poss fitting, 1 = contested cells updated, 2 = final weighting, 3 = only adjacent to X and only used in [0][3][][]
        // shipMaps[ ][ ][row][col] will hold the 2D arrays of Maps
        // shipMaps[ ][ ][ 0 ][10 ] will hold the sum of the entire board
        // shipMaps[0][2][][] = sum of all shipMaps[1-5][2][][] boards.  contains the odds that SOMEONE is currently using the space
        //shipMaps[1][1][0][10] = 1;
        //shipMaps[2][1][0][10] = 1;
        //shipMaps[3][1][0][10] = 1;
        //shipMaps[4][1][0][10] = 1;
        //shipMaps[5][1][0][10] = 1;

        int[][] ships = new int[6][2];
        // ships[i][ ] where i indicates shipNumber (1,2,3,4,5)
        // ships[ ][0] is the ship still floating? (0 = no, 1 = yes)
        // ships[ ][1] is the length of the ship (number of segments)
        ships[1][0] = 1;
        ships[2][0] = 1;
        ships[3][0] = 1;
        ships[4][0] = 1;
        ships[5][0] = 1;
        ships[1][1] = 2;
        ships[2][1] = 3;
        ships[3][1] = 3;
        ships[4][1] = 4;
        ships[5][1] = 5;

        int[][] floatingShips = new int[2][5];
        floatingShips[1][0] = -1;
        //floatingShips[0][] is a list of floating shipNumbers
        //floatingShips[1][0] is the index of the largest ship still floating, in the floatingShips[0][] array
        //floatingShips[1][1] is a count of ships still floating

        updateShipsAndFloatingShips(floatingShips, guesses, ships);
        generateBasicPossBoards(guesses, shipMaps, ships); // creates all my ships' [][0][][] boards
        generateB1sAndB2s(floatingShips, guesses, shipMaps, ships);

        int finalBoardStateToGuess = 2;
        if (guessesHasChar('X', guesses)) {
            finalBoardStateToGuess = 3;
            generateB3(guesses, shipMaps);
        }
        kenArbWeighting(finalBoardStateToGuess, guesses, shipMaps);
        skewDiagonally(finalBoardStateToGuess, guesses, shipMaps, ships); //LOWERS AVERAGE BUT ALSO LOWERS WINRATE! COMPRESSES RESULTS!
        int[] ties = new int[100];
        //ties[0] is the number of ties
        //ties[1-99] are the locations of ties, loaded from [1] first
        findLargestAndTestForTie(finalBoardStateToGuess, guesses, shipMaps, ties);

        ties = breakOnSmallestWrapper(floatingShips, shipMaps, ships, ties);

        //return (convertToGuess(ties[1]));
        //return (convertToGuess(ties[ties[0]]));
        return (convertToGuess(randomTieBreaker(ties)));
    }

    public static void updateShipsAndFloatingShips(int[][] floatingShips, char[][] guesses, int[][] ships) {
        //updates ships[i][0] values, where i is the found numeric value in guesses
        for (int row = 0; row < 10; row++) {
            for (int col = 0; col < 10; col++) {
                if (guesses[row][col] < '6' && guesses[row][col] > '0') {
                    ships[guesses[row][col] - '0'][0] = 0;
                }
            }
        }
        //update floatingShips
        int fsIndex = 0;
        for (int i = 1; i < 6; i++) {
            if (ships[i][0] == 1) {
                floatingShips[0][fsIndex] = i;
                fsIndex++;
                floatingShips[1][0]++;
                floatingShips[1][1]++;
            }
        }
    }

    //creates basic possibility boards, PER SHIP
    public static void generateBasicPossBoards(char[][] guesses, double[][][][] shipMaps, int[][] ships) {
        //test for n-sized boat locations
        //wrap inside a for loop to iterate through ships[1][0] to ships[5][0]
        for (int shipNumber = 5; shipNumber > 0; shipNumber--) {
            if (ships[shipNumber][0] == 1) {
                //test for laterally-oriented boat
                for (int row = 0; row < 10; row++) {
                    //test for fit at a single boat position
                    for (int col = 0; col < (11 - ships[shipNumber][1]); col++) {
                        boolean fits = true;
                        boolean onDot = false;
                        for (int i = 0; i < ships[shipNumber][1]; i++) {
                            if (!(guesses[row][col + i] == '.' || guesses[row][col + i] == 'X')) {
                                fits = false;
                            } else if (guesses[row][col + i] == '.') {
                                onDot = true;
                            }
                        }
                        //if it fits, update possBoard
                        if (fits && onDot) {
                            for (int i = 0; i < ships[shipNumber][1]; i++) {
                                shipMaps[shipNumber][0][row][col + i]++;
                            }
                        }
                    }
                }
                //test for vertically-oriented boat
                for (int row = 0; row < (11 - ships[shipNumber][1]); row++) {
                    //test for fit at a single boat position
                    for (int col = 0; col < 10; col++) {
                        boolean fits = true;
                        boolean onDot = false;
                        for (int i = 0; i < ships[shipNumber][1]; i++) {
                            if (!(guesses[row + i][col] == '.' || guesses[row + i][col] == 'X')) {
                                fits = false;
                            } else if (guesses[row + i][col] == '.') {
                                onDot = true;
                            }
                        }
                        //if it fits, update possBoard
                        if (fits && onDot) {
                            for (int i = 0; i < ships[shipNumber][1]; i++) {
                                shipMaps[shipNumber][0][row + i][col]++;
                            }
                        }
                    }
                }

            }
        }
    }

    //a method to manage building the B1 and B2 boards for all ships gracefully
    public static void generateB1sAndB2s(int[][] floatingShips, char[][] guesses, double[][][][] shipMaps, int[][] ships) {
        //update floating ships' B1s and B2s
        for (int fSITP = floatingShips[1][0]; fSITP > -1; fSITP--) { //fSITP is floating Ships Index To Process
            refineContestedCells(floatingShips, fSITP, shipMaps);
            apportionWeight(guesses, shipMaps, ships, floatingShips[0][fSITP]);
        }
    }

    //compares      shipMaps[shipNumber][0][][]'s per-cell values
    //against  (1 - shipMaps[next-largest still floating ship's index][2][][]) for all still-floating ships
    //meaning       shipMaps[shipNumber][1][][]'s per-cell values are the product of (chanceCellIsEmpty * its own [][0][][])
    //the logic being that the largest ships place first, then if they are not there, the next-largest ship is permitted to fit
    //shipMaps[i][2][][] not updated in this function
    public static void refineContestedCells(int[][] floatingShips, int fSITP, double[][][][] shipMaps) {
        //iterate through the board
        int shipNumber = floatingShips[0][fSITP];
        for (int row = 0; row < 10; row++) {
            for (int col = 0; col < 10; col++) {
                //could be bad code, feeling confused but pushing foward
                double chanceCellIsEmpty = 1;
                for (int offset = floatingShips[1][0] - fSITP; offset > 0; offset--) {
                    chanceCellIsEmpty *= (1 - shipMaps[shipNumber][2][row][col]);
                }
                double newCellValue = shipMaps[shipNumber][0][row][col] * chanceCellIsEmpty;
                shipMaps[shipNumber][1][row][col] = newCellValue;
                shipMaps[shipNumber][1][0][10] += newCellValue;
            }
        }
    }

    public static void apportionWeight(char[][] guesses, double[][][][] shipMaps, int[][] ships, int shipNumber) {
        double theDivisor = shipMaps[shipNumber][1][0][10] / ships[shipNumber][1];
        if (theDivisor == 0) {
            //System.out.println("theDivisor is equal to zero!! PROBLEM!!");
            //BattleShipTools.printBoard(guesses);
        }
        //iterate through the board
        for (int row = 0; row < 10; row++) {
            for (int col = 0; col < 10; col++) {
                shipMaps[shipNumber][2][row][col] = shipMaps[shipNumber][1][row][col] / theDivisor;
                shipMaps[0][2][row][col] += shipMaps[shipNumber][1][row][col] / theDivisor;
            }
        }
    }

    public static void findLargestAndTestForTie(int boardState, char[][] guesses, double[][][][] shipMaps, int[] ties) {
        double largestCell = 0;
        //iterate through the board
        for (int row = 0; row < 10; row++) {
            for (int col = 0; col < 10; col++) {
                if (shipMaps[0][boardState][row][col] > largestCell && guesses[row][col] != 'X') {
                    largestCell = shipMaps[0][boardState][row][col];
                    //reset ties array - MAKES NO DIFFERENCE IN TESTING
                    // for (int i = 0; i < 100; i++) {
                    // ties[i] = 0;
                    // }
                    ties[1] = (row * 10) + col;
                    ties[0] = 1;
                }
                if (shipMaps[0][boardState][row][col] == largestCell && guesses[row][col] != 'X') {
                    ties[0]++;
                    ties[ties[0]] = (row * 10) + col;
                }
            }
        }
    }

    public static boolean guessesHasChar(char theChar, char[][] guesses) {
        for (int row = 0; row < 10; row++) {
            for (int col = 0; col < 10; col++) {
                if (guesses[row][col] == theChar)
                    return (true);
            }
        }
        return (false);
    }

    public static void generateB3(char[][] guesses, double[][][][] shipMaps) {
        //description of method here
        for (int row = 0; row < 10; row++) {
            for (int col = 0; col < 10; col++) {
                //tests if pos below is inbounds AND an X
                if (row < 9 && guesses[row + 1][col] == 'X') {
                    shipMaps[0][3][row][col] = shipMaps[0][2][row][col];
                }
                //test if pos to the right is inbounds AND an X
                if (col < 9 && guesses[row][col + 1] == 'X') {
                    shipMaps[0][3][row][col] = shipMaps[0][2][row][col];
                }
                //test if pos above is inbounds AND an X
                if (row > 0 && guesses[row - 1][col] == 'X') {
                    shipMaps[0][3][row][col] = shipMaps[0][2][row][col];
                }
                //test if pos to the left is inbounds AND an X
                if (col > 0 && guesses[row][col - 1] == 'X') {
                    shipMaps[0][3][row][col] = shipMaps[0][2][row][col];
                }
            }
        }
    }

    public static void skewDiagonally(int finalBoardStateToGuess, char[][] guesses, double[][][][] shipMaps, int[][] ships) {
        //only execute while Patrol Boat floats AND no Xs on the board
        if (ships[1][0] == 1 && !guessesHasChar('X', guesses)) {
            //iterate through the board
            for (int row = 0; row < 10; row++) {
                for (int col = 0; col < 10; col++) {
                    //"odd" cells only while Patrol Boat floats
                    //even cells are found and set to zero
                    if (((row + col) % 2) == 0) {
                        shipMaps[0][finalBoardStateToGuess][row][col] = 0;
                    }
                }
            }
        }
    }

    public static String convertToGuess(int intGuess) {
        String toReturn = "";
        //be mindful that intGuess is index-0, and I need my output to be index-1
        toReturn += ((char) (intGuess / 10 + 64 + 1));
        toReturn += Integer.toString(intGuess % 10 + 1); //hacky
        //System.out.println("Should guess at " + intGuess);
        return toReturn;
    }

    public static int randomTieBreaker(int[] ties) {
        return (ties[(int) (Math.random() * ties[0]) + 1]);
    }

    public static void kenArbWeighting(int boardState, char[][] guesses, double[][][][] shipMaps) {
        //test for two consecutive Xs
        //iterate through the board
        //test for laterally-oriented consecutive Xs
        for (int row = 0; row < 10; row++) {
            for (int col = 0; col < 9; col++) {
                if (guesses[row][col] == 'X' && guesses[row][col + 1] == 'X') {
                    if (col > 0 && guesses[row][col - 1] == '.') {
                        shipMaps[0][boardState][row][col - 1]++;
                    }
                    if (col < 8 && guesses[row][col + 2] == '.') {
                        shipMaps[0][boardState][row][col + 2]++;
                    }
                }
            }
        }
        //test for vertically-oriented consecutive Xs
        for (int row = 0; row < 9; row++) {
            for (int col = 0; col < 10; col++) {
                if (guesses[row][col] == 'X' && guesses[row + 1][col] == 'X') {
                    if (row > 0 && guesses[row - 1][col] == '.') {
                        shipMaps[0][boardState][row - 1][col]++;
                    }
                    if (row < 8 && guesses[row + 2][col] == '.') {
                        shipMaps[0][boardState][row + 2][col]++;
                    }
                }
            }
        }
    }

    public static double[][][][] boardStateCompressed(int boardState, double[][][][] shipMaps) {
        double[][][][] compressedBoard = new double[0][0][10][11];
        //iterate through the arrays, per ship.  three for loops
        for (int shipNumber = 5; shipNumber > 0; shipNumber--) {
            for (int row = 0; row < 10; row++) {
                for (int col = 0; col < 10; col++) {
                    compressedBoard[0][0][row][col] = shipMaps[shipNumber][boardState][row][col];
                }
            }
        }
        return (compressedBoard);
    }

    public static void printBoard(int boardState, char[][] guesses, double[][][][] shipMaps, int shipNumber) {
        System.out.println();
        System.out.println("\nshipMaps of shipNumber: " + shipNumber + " in boardState: " + boardState + " (* 100)\n");
        for (int row = 0; row < 10; row++) {
            for (int col = 0; col < 10; col++) {
                System.out.printf("%9.4f", 100 * shipMaps[shipNumber][boardState][row][col]);
            }
            System.out.println();
        }
        System.out.println();
    }

    public static int fixedOpeningIfNoHits(char[][] guesses) {
        if (!guessesHasChar('X', guesses) && !guessesHasChar('1', guesses) && !guessesHasChar('2', guesses)
                && !guessesHasChar('3', guesses) && !guessesHasChar('4', guesses) && !guessesHasChar('5', guesses)) {
            if (guesses[5][4] == '.') {
                return (54);
            }
            if (guesses[4][5] == '.') {
                return (45);
            }
            if (guesses[6][3] == '.') {
                return (63);
            }
            if (guesses[3][6] == '.') {
                return (36);
            }
            if (guesses[7][2] == '.') {
                return (72);
            }
            if (guesses[2][7] == '.') {
                return (27);
            }

        }

        return (0000);

    }

    public static int[] breakOnSmallest(int[][] floatingShips, double[][][][] shipMaps, int[][] ships, int shipToTest, int[] ties) {
        int[] tiesNew = new int[100];
        double greatestValue = 0;
        //finds largest value amongst ties in shipToTest's 2board
        for (int i = ties[0]; i > 0; i--) {
            if (shipMaps[shipToTest][2][ties[i] / 10][ties[i] % 10] > greatestValue) {
                greatestValue = shipMaps[shipToTest][2][ties[i] / 10][ties[i] % 10];
            }
        }
        for (int i = ties[0]; i > 0; i--) {
            if (shipMaps[shipToTest][2][ties[i] / 10][ties[i] % 10] == greatestValue) {
                tiesNew[0]++;
                tiesNew[tiesNew[0]] = ties[i];
            }
        }
        return tiesNew;
    }

    public static int[] breakOnSmallestWrapper(int[][] floatingShips, double[][][][] shipMaps, int[][] ships, int[] ties) {
        int[] tiesNew = new int[100];
        if (ties[0] > 1) {
            for (int i = 0; i <= floatingShips[1][0]; i++) {
                tiesNew = breakOnSmallest(floatingShips, shipMaps, ships, i, ties);
                if (tiesNew[0] == 1) {
                    return tiesNew;
                }
            }
            return tiesNew;
        } else {
            return ties;
        }
    }

} 

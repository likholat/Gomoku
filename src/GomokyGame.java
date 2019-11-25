import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.Socket;

class GomokyGame {
    private static final int SIZE = 15;
    // a board of 15x15 squares
    private Player[][] board = new Player[SIZE][SIZE];
    //current player
    Player currentPlayer;
    private boolean connectionError = false;

    private boolean hasWinner() {
        //horizontally
        for (int i = 0; i <= SIZE - 5; i++) {
            for (int j = 0; j < SIZE; j++) {
                if (board[i][j] != null &&
                        board[i+1][j] == board[i][j] &&
                        board[i+2][j] == board[i][j] &&
                        board[i+3][j] == board[i][j] &&
                        board[i+4][j] == board[i][j]) {
                    return true;
                }
            }
        }

        //vertically
        for (int i = 0; i < SIZE; i++) {
            for (int j = 0; j <= SIZE - 5; j++) {
                if (board[i][j] != null &&
                        board[i][j+1] == board[i][j] &&
                        board[i][j+2] == board[i][j] &&
                        board[i][j+3] == board[i][j] &&
                        board[i][j+4] == board[i][j]) {
                    return true;
                }
            }
        }

        // Check first diagonal
        for (int i = 0; i <= SIZE - 5; i++) {
            for (int j = 0; j <= SIZE - 5; j++) {
                if (board[i][j] != null &&
                        board[i+1][j+1] == board[i][j] &&
                        board[i+2][j+2] == board[i][j] &&
                        board[i+3][j+3] == board[i][j] &&
                        board[i+4][j+4] == board[i][j]) {
                    return true;
                }
            }
        }

        // Check second diagonal
        for (int i = 0; i <= SIZE - 5 ; i++) {  // Rows 0..10
            for (int j = 4; j < SIZE; j++) {    // Cols 4..14
                if (board[i][j] != null &&
                        board[i+1][j-1] == board[i][j] &&
                        board[i+2][j-2] == board[i][j] &&
                        board[i+3][j-3] == board[i][j] &&
                        board[i+4][j-4] == board[i][j]) {
                    return true;
                }
            }
        }
        return false;
    }

    private boolean boardFilledUp() {
        for (Player[] aBoard : board) {
            for(Player bBoard: aBoard)
                if (bBoard == null) {
                  return false;
                }
        }
        return true;
    }

    private synchronized boolean legalMove(int locationX, int locationY, Player player) {
        if (player == currentPlayer && board[locationX][locationY] == null) {
            board[locationX][locationY] = currentPlayer;
            currentPlayer = currentPlayer.opponent;
            currentPlayer.otherPlayerMoved(locationX, locationY);
            return true;
        }
        return false;
    }

    class Player extends Thread {

        char mark;
        Player opponent;
        Socket socket;
        BufferedReader input;
        PrintWriter output;
        int PORT;


        Player(Socket socket, char mark, int PORT) {
            this.socket = socket;
            this.mark = mark;
            this.PORT = PORT;

            try {
                input = new BufferedReader(
                        new InputStreamReader(socket.getInputStream()));
                output = new PrintWriter(socket.getOutputStream(), true);
                output.println("WELCOME " + mark);
                output.println("MESSAGE Waiting for opponent to connect");
            } catch (IOException e) {
                output.println("ERROR opponent disconnected!");
                System.out.println("\nConnection with player: " + mark + " on PORT: " + socket.getLocalPort() + " LOST!!!");
            }
        }

        public void setOpponent(Player opponent) {
            this.opponent = opponent;
        }

        private void otherPlayerMoved(int locationX, int locationY) {
            output.println("OPPONENT_MOVED (" + locationX + "," + locationY + ")");
            output.println(hasWinner() ? "DEFEAT" : boardFilledUp() ? "TIE" : "");

        }

        public void run() {
            try {
                // The thread is only started after everyone connects.
                output.println("MESSAGE All players connected");

                // Tell the first player that it is his/her turn.
                if (mark == 'X') {
                    output.println("MESSAGE Your move");
                }

                while (true) {
                    String command = input.readLine();
                    if (command.startsWith("MOVE")) {

                        int bracketOpen = command.indexOf('(');
                        int bracketClose = command.indexOf(')');
                        int comma = command.indexOf(',');
                        int locationX = Integer.parseInt(command.substring(bracketOpen+1,comma));
                        int locationY = Integer.parseInt(command.substring(comma+1,bracketClose));
                        if (legalMove(locationX, locationY, this)) {
                            output.println("VALID_MOVE");
                            output.println(hasWinner() ? "VICTORY" : boardFilledUp() ? "TIE" : "");
                        } else {
                            if(!connectionError) {
                                output.println("MESSAGE ...");
                            }
                            else {
                                output.println("ERROR opponent disconnected!");
                            }
                        }
                    } else if (command.startsWith("QUIT")) {
                        return;
                    }
                }
            } catch (IOException e) {
                connectionError = true;

                System.out.println("\nConnection with player: " + mark + " on PORT: " + socket.getLocalPort() + " LOST!!!");
                System.out.print("\nDo you want to create new room? y/n: ");

            } finally {
                try {
                    socket.close();
                } catch (IOException e) {
                    System.out.println("Closing socket problem!");
                }
            }
        }
    }
}

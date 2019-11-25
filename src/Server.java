import java.io.IOException;
import java.net.ServerSocket;
import java.util.Scanner;
import java.util.regex.Pattern;

public class Server {
    private static int[] portList = new int[255];
    private static int[] playerNumber = new int[255];
    private static int listElements = 0;
    private static int roomNumber = 0;
    private static ServerSocket listener = null;
    private static boolean portIsCorrect = false;
    private static boolean portIsNotBusy = false;
    private static boolean setNewServerCorrect = false;
    private static Scanner scanner;

    public Server() {
    }

    private static void displayPORTs() {
        System.out.println("\nActive PORTs: ");

        for(int i = 0; i < listElements; ++i) {
            if (portList[i] != 0) {
                System.out.println(i + 1 + ". Room on PORT: " + portList[i] + "  -> " + playerNumber[i] + "/2 players.");
            }
        }

        System.out.println();
    }

    private static void displayPlayers(int room) {
        if (portList[room] != 0) {
            System.out.println(room + 1 + ". Room: " + playerNumber[room] + "/2 players.");
            if (playerNumber[room] < 2) {
                playerNumber[room]++;
            }
        }

    }

    private static void setPORT() {
        if (roomNumber != 0) {
            displayPORTs();
        }

        for(; !portIsCorrect || !portIsNotBusy; displayPORTs()) {
            System.out.print("\nPress the port number (1000 - 9999): ");
            String portAsString = scanner.nextLine();
            validatePORTValue(portAsString);
            if (portIsCorrect) {
                int portAsInt = Integer.parseInt(portAsString);
                validatePORTlist(portAsInt);
                if (portIsNotBusy) {
                    listElements = 0;

                    for(int i = 0; i < 255; ++i) {
                        if (portList[i] == 0) {
                            portList[i] = portAsInt;

                            for(int j = 0; j < 255; ++j) {
                                if (portList[j] != 0) {
                                    ++listElements;
                                }
                            }

                            playerNumber[i] = 0;
                            roomNumber = i;
                            break;
                        }
                    }
                }
            }
        }

        portIsCorrect = false;
        portIsNotBusy = false;
    }

    private static void validatePORTValue(String portAsString) {
        if (!Pattern.matches("[1-9][0-9][0-9][0-9]", portAsString)) {
            portIsCorrect = false;
            System.out.println("\nIncorrect PORT value!");
        } else {
            portIsCorrect = true;
        }

    }

    private static void validatePORTlist(int portAsInt) {
        portIsNotBusy = true;

        for(int i = 0; i <= listElements; ++i) {
            if (portList[i] == portAsInt) {
                portIsNotBusy = false;
                System.out.println("\nThis PORT is busy!");
            }
        }

    }

    private static ServerSocket setServerSocket() {
        try {
            listener = new ServerSocket(portList[listElements - 1]);
        } catch (IOException var1) {
            System.out.println("Setting new ServerSocket problem!");
            var1.printStackTrace();
        }
        return listener;
    }

    private static ServerSocket createNewRoom() {
        setPORT();
        listener = setServerSocket();
        System.out.println("Server is Running on PORT: " + portList[listElements - 1]);
        return listener;
    }

    private static void createMoreRooms(GomokyGame[] game) {
        while(true) {
            System.out.print("\nDo you want to create new room? y/n: ");
            String temp = scanner.nextLine();
            if (Pattern.matches("[yY]", temp)) {
                setNewServerCorrect = false;

                for(int i = 0; i < roomNumber; ++i) {
                    if (!game[i].currentPlayer.isAlive() || !game[i].currentPlayer.opponent.isAlive() || game[i].currentPlayer.PORT != portList[i]) {
                        releasePORT(portList[i]);
                    }
                }
            } else {
                if (!Pattern.matches("[nN]", temp)) {
                    System.out.println("Incorrect answer!");
                    continue;
                }

                setNewServerCorrect = true;
            }

            return;
        }
    }

    private static void releasePORT(int PORT) {
        for(int i = 0; i < listElements; ++i) {
            if (PORT == portList[i]) {
                System.out.println(portList[i]);
                portList[i] = 0;
            }
        }

    }

    public static void main(String[] args) throws Exception {
        GomokyGame[] game = new GomokyGame[255];

        for(int i = 0; i < 255; ++i) {
            game[i] = new GomokyGame();
            playerNumber[i] = 0;
            portList[i] = 0;
        }

        while(!setNewServerCorrect) {
            listener = createNewRoom();

            displayPlayers(roomNumber);
            GomokyGame.Player playerX = game[roomNumber].new Player(listener.accept(), 'X', portList[roomNumber]);
            displayPlayers(roomNumber);
            GomokyGame.Player playerO = game[roomNumber].new Player(listener.accept(), 'O', portList[roomNumber]);
            displayPlayers(roomNumber);

            listener.close();

            playerX.setOpponent(playerO);
            playerO.setOpponent(playerX);
            game[roomNumber].currentPlayer = playerX;
            //start threads
            playerX.start();
            playerO.start();

            createMoreRooms(game);
        }

    }

    static {
        scanner = new Scanner(System.in);
    }
}
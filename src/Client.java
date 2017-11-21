import java.io.*;
import java.net.Socket;
import java.util.Objects;
import java.util.Scanner;

/**
 * A client for the TicTacToe game, modified and extended from the
 * class presented in Deitel and Deitel "Java How to Program" book.
 * I made a bunch of enhancements and rewrote large sections of the
 * code.  In particular I created the TTTP (Tic Tac Toe Protocol)
 * which is entirely text based.  Here are the strings that are sent:
 *
 *  Client -> Server           Server -> Client
 *  ----------------           ----------------
 *  MOVE <n>  (0 <= n <= 8)    WELCOME <char>  (char in {X, O})
 *  QUIT                       VALID_MOVE
 *                             OTHER_PLAYER_MOVED <n>
 *                             VICTORY
 *                             DEFEAT
 *                             TIE
 *                             MESSAGE <text>
 *
 */

//Client class that contains the HangmanClient object
public class Client {


    private final byte[] readBuffer;
    private int numIncorrect;
    private Socket socket;
    private InputStream stream;
    private DataOutputStream writer;
    private Scanner scan;
    private int msgFlag;
    private String guesses;
    private String correctGuesses;
    private boolean runGame = false;

    // Client constructor
    //Sets up the client object
    private Client(String ip, int port) throws Exception{
        readBuffer = new byte[50];
        numIncorrect = 0;
        socket = new Socket(ip, port);
        stream = socket.getInputStream();
        writer = new DataOutputStream(socket.getOutputStream());
        //System.out.println("New Client Created");
        scan = new Scanner(System.in);
    }

    //Terminates the client-side connection
    private int endConnection() {
        try {
            stream.close();
            writer.close();
            socket.close();
        } catch (IOException e) {
            e.printStackTrace();
            return 1;
        }
        return 0;
    }

    //Main game loop
    private void playGame() throws Exception {
        initiateGame();
        while(runGame) {
            this.readMessage();
            this.guessLetter();
        }
    }

    //Sets up the game. Checks if the user wants to play the game
    private void initiateGame() throws Exception {
        System.out.printf("Ready to start the game? (y/n):");
        String message = askUserInput().toLowerCase();
        //Make sure the start game message is "y". Otherwise exit application
        while (!Objects.equals(message, "n")
                && !Objects.equals(message,"y")) {
            System.out.println("Please select either 'y' or 'n'.");
            message = askUserInput().toLowerCase();
        }
        runGame = (runGame || !message.equals("n")) && !runGame
                && message.equals("y");

        if (!runGame) {
            //noinspection ControlFlowStatementWithoutBraces,StatementWithEmptyBody
            while (endConnection() == 1);
            System.exit(0);
        } else {
            writeMessage("");
        }
    }

    //Asks the user to guess a letter and checks for proper input.
    // Writes to the server
    private void guessLetter() throws Exception {
        System.out.println("Letter To Guess: ");
        String message = this.askUserInput().toLowerCase();
        boolean goodGuess = false;
        while(!goodGuess) {
            if (message.length() != 1) {
                System.out.println("Please enter exactly 1 character.");
                message = askUserInput().toLowerCase();
            } else if (!Character.isLetter(message.charAt(0))) {
                System.out.println("Please enter a character a-z.");
                message = askUserInput().toLowerCase();
            } else if (guesses.contains(message)) {
                System.out.println("Please enter a character that you have " +
                        "not yet guessed.");
                message = askUserInput().toLowerCase();
            } else if (correctGuesses.contains(message)) {
                System.out.println("Please enter a character that you have " +
                        "not yet guessed.");
                message = askUserInput().toLowerCase();
            } else {
                goodGuess = true;
            }
        }
        this.writeMessage(message);
    }

    //Read message helper function.
    // Handles the output and formatting from the server
    @SuppressWarnings("ResultOfMethodCallIgnored")
    private void checkMessage() throws IOException {
        if (msgFlag > 0) {
            this.stream.read(this.readBuffer);
            String message = new String(readBuffer).substring(0,
                    msgFlag);
            System.out.println(message);
            this.endConnection();
            System.exit(0);
        } else {
            int wordLength = this.stream.read();
            //System.out.println("Word Length is " + wordLength);
            this.numIncorrect = this.stream.read();
            //System.out.println("Number Incorrect is " + numIncorrect);
            this.stream.read(this.readBuffer);
            String message = new String(readBuffer);
            guesses = message.substring(wordLength,
                    wordLength + numIncorrect);
            guesses = addSpaces(guesses);
            message = message.substring(0, wordLength);
            message = addSpaces(message);
            correctGuesses = message;
            System.out.println(message);
            System.out.println("Incorrect Guesses: " + guesses + "\n");
        }
    }

    //Helper method to add spaces to various Strings
    private String addSpaces(String message) {
        StringBuilder newString = new StringBuilder();
        for (int i = 0; i < message.length(); i++) {
            newString.append(message.charAt(i)).append(" ");
        }
        return newString.toString();
    }

    //Wrapper function to read messages from the server
    private void readMessage() throws Exception{
        msgFlag = this.stream.read();
        this.checkMessage();
    }

    //Function to write messages to the server.
    @SuppressWarnings("Duplicates")
    private void writeMessage(String message) throws Exception {
        byte msgLength = (byte) (message.length());
        byte[] messageBytes = message.getBytes();
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        out.write(msgLength);
        out.write(messageBytes);
        byte[] byteMessage = out.toByteArray();
        this.writer.write(byteMessage, 0, message.length() + 1);
    }

    //Helper method to ask for the user's input
    private String askUserInput() {
        return this.scan.nextLine();
    }

    //Main method.
    public static void main(String[] args) throws Exception {
        //System.out.println("Hangman Client is Running");
        Client client = new Client(args[0], Integer.parseInt(args[1]));
        //while loop to execute game play
        client.playGame();
    }
}
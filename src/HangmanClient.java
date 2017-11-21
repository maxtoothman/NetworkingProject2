package src;

import java.io.*;
import java.net.Socket;
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
public class HangmanClient {

    private byte[] readBuffer;
    private int numIncorrect;
    int wordLength;
    private Socket socket;
    private InputStream stream;
    private DataOutputStream writer;
    private Scanner scan;
    private int msgFlag;
    private String message;

    private boolean runGame = false;

    private HangmanClient() throws Exception{
        readBuffer = new byte[50];
        numIncorrect = 0;
        socket = new Socket("localhost", 8080);
        stream = socket.getInputStream();
        writer = new DataOutputStream(socket.getOutputStream());
        //System.out.println("New Client Created");
        scan = new Scanner(System.in);
    }

    private int endConnection() throws IOException {
        try {
            socket.close();
        } catch (IOException e) {
            e.printStackTrace();
            return 1;
        }
        return 0;
    }

    private void playGame() throws Exception {
        initiateGame();
        while(runGame) {
            this.readMessage();
            this.guessLetter();
        }
    }


    private void initiateGame() throws Exception {
        System.out.printf("Ready to start the game? (y/n):");
        String message = askUserInput();
        //Make sure the start game message is "y". Otherwise exit application
        runGame = (runGame || !message.equals("n")) && !runGame && message.equals("y");

        //TODO: add a check to ask again if start game response is not "y/Y" or "n/N"
        if (!runGame) {
            while (endConnection() == 1);
            System.exit(0);
        } else {
            writeMessage("");
        }
    }

    private void guessLetter() throws Exception {
        System.out.println("Next Letter To Guess: ");
        String message = this.askUserInput();
        this.writeMessage(message);
    }

    private void checkMessage() throws IOException {
        if (msgFlag > 0) {
            this.stream.read(this.readBuffer);
            message = new String(readBuffer).substring(0,
                    msgFlag);
            System.out.println(message);
            this.endConnection();
            System.exit(0);
        } else {
            this.wordLength = this.stream.read();
            //System.out.println("Word Length is " + wordLength);
            this.numIncorrect = this.stream.read();
            //System.out.println("Number Incorrect is " + numIncorrect);
            this.stream.read(this.readBuffer);
            String message = new String(readBuffer);
            String guesses = message.substring(wordLength,
                    wordLength + numIncorrect);
            message = message.substring(0,wordLength);
            System.out.println(message);
            System.out.println("Incorrect Guesses: " + guesses);
        }
    }


    private void readMessage() throws Exception{
        msgFlag = this.stream.read();
        this.checkMessage();
    }

    private void writeMessage(String message) throws Exception {
        byte msgLength = (byte) (message.length());
        byte[] messageBytes = message.getBytes();
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        out.write(msgLength);
        out.write(messageBytes);
        byte[] byteMessage = out.toByteArray();
        this.writer.write(byteMessage, 0, message.length() + 1);
    }

    private String askUserInput() {
        return this.scan.nextLine();
    }

    public static void main(String[] args) throws Exception {
        //System.out.println("Hangman Client is Running");
        HangmanClient client = new HangmanClient();
        //while loop to execute gameplay
        client.playGame();
    }
}
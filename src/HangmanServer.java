package src;

import java.io.*;
import java.net.InetSocketAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.Arrays;
import java.util.Random;
import java.util.Scanner;


public class HangmanServer {

    static String[] dictionary = {"banana", "mistake", "hopons", "her",
            "illusion", "marryme", "blueman", "bees", "cornball", "cousin",
            "family", "seal", "poppop", "freebie", "franklin"};


    public static void main(String[] args) throws Exception {
        ServerSocket listener = new ServerSocket();
        System.out.println("Hangman Server is Running");
        InetSocketAddress listenerAddress = new InetSocketAddress(8080);
        listener.bind(listenerAddress);
        while (true) {
            Socket clientSocket = listener.accept();
            System.out.println("Connection Accepted");
            Game hangmanGame = new Game(clientSocket);
            System.out.println("Word is " + hangmanGame.getWord());
            hangmanGame.startGame();
            hangmanGame.writeControl();
        }
    }
}

class Game {

    private byte[] readBuffer;
    private String word;
    private byte[] emptywordArray;
    private byte[] guessArray;
    private DataOutputStream writer;
    private int numIncorrect;
    private int wordLength;
    private Socket playerSocket;
    private InputStream stream;
    private Scanner scan;

    Game(Socket clientSocket) throws Exception{
        readBuffer = new byte[50];
        playerSocket = clientSocket;
        numIncorrect = 0;
        Random rand = new Random();
        int index = rand.nextInt(15);
        word = HangmanServer.dictionary[index];
        wordLength = word.length();
        guessArray = new byte[6];
        emptywordArray = new byte[wordLength];
        for (int i =0;i<wordLength;i++) {
            emptywordArray[i] = (byte)'_';
        }
        System.out.println("Empty word array is " + Arrays.toString(emptywordArray));
        writer = new DataOutputStream(playerSocket.getOutputStream());
        stream = playerSocket.getInputStream();
        scan = new Scanner(System.in);
    }

    String getWord() {
        return this.word;
    }

    void startGame() throws Exception {
        this.writeMessage("Ready to start game? (y/n):");
        System.out.println("Message Sent");
        String response = this.readMessage();
        System.out.println("Reply is " + response);
        if (response.equals("y")) {
            System.out.printf("Client is ready");
        } else {
            System.out.println("Client is not ready");
        }
    }

    private void writeMessage(String message) throws Exception{
        byte flag = (byte) (message.length());
        byte[] messageBytes = message.getBytes();
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        out.write(flag);
        out.write(messageBytes);
        byte[] byteMessage = out.toByteArray();
        //System.out.println(byteMessage);
        this.writer.write(byteMessage,0,message.length()+1);
    }

    void writeControl() throws Exception{
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        out.write((byte) 0);
        out.write((byte) wordLength);
        out.write((byte) numIncorrect);
        byte[] data = new String(this.emptywordArray).getBytes();
        out.write(data);
        out.write(guessArray,0,numIncorrect);
        byte[] controlMessage = out.toByteArray();
        this.writer.write(controlMessage,0,wordLength+3+numIncorrect);
    }

    private String readMessage() throws Exception {
        int msgFlag = this.stream.read();
        //System.out.println(readBuffer);
        //System.out.println(msgFlag);
        String message;
        if (msgFlag > 0) {
            int status = this.stream.read(this.readBuffer);
            System.out.println("Read message status: " + status);
            message = new String(readBuffer);
            return message.substring(0,msgFlag);
        }
        else {
            return "";
        }
    }

}
//
//
//        try {
//            while (true) {
//                Game game = new Game();
//                Game.Player playerX = game.new Player(listener.accept(), 'X');
//                Game.Player playerO = game.new Player(listener.accept(), 'O');
//                playerX.setOpponent(playerO);
//                playerO.setOpponent(playerX);
//                game.currentPlayer = playerX;
//                playerX.start();
//                playerO.start();
//            }
//        } finally {
//            listener.close();
//        }
//    }
//}
//
///**
// * A two-player game.
// */
//class Game {
//
//    /**
//     * A board has nine squares.  Each square is either unowned or
//     * it is owned by a player.  So we use a simple array of player
//     * references.  If null, the corresponding square is unowned,
//     * otherwise the array cell stores a reference to the player that
//     * owns it.
//     */
//    private Player[] board = {
//            null, null, null,
//            null, null, null,
//            null, null, null};
//
//    /**
//     * The current player.
//     */
//    Player currentPlayer;
//
//    /**
//     * Returns whether the current state of the board is such that one
//     * of the players is a winner.
//     */
//    public boolean hasWinner() {
//        return
//                (board[0] != null && board[0] == board[1] && board[0] == board[2])
//                        ||(board[3] != null && board[3] == board[4] && board[3] == board[5])
//                        ||(board[6] != null && board[6] == board[7] && board[6] == board[8])
//                        ||(board[0] != null && board[0] == board[3] && board[0] == board[6])
//                        ||(board[1] != null && board[1] == board[4] && board[1] == board[7])
//                        ||(board[2] != null && board[2] == board[5] && board[2] == board[8])
//                        ||(board[0] != null && board[0] == board[4] && board[0] == board[8])
//                        ||(board[2] != null && board[2] == board[4] && board[2] == board[6]);
//    }
//
//    /**
//     * Returns whether there are no more empty squares.
//     */
//    public boolean boardFilledUp() {
//        for (int i = 0; i < board.length; i++) {
//            if (board[i] == null) {
//                return false;
//            }
//        }
//        return true;
//    }
//
//    /**
//     * Called by the player threads when a player tries to make a
//     * move.  This method checks to see if the move is legal: that
//     * is, the player requesting the move must be the current player
//     * and the square in which she is trying to move must not already
//     * be occupied.  If the move is legal the game state is updated
//     * (the square is set and the next player becomes current) and
//     * the other player is notified of the move so it can update its
//     * client.
//     */
//    public synchronized boolean legalMove(int location, Player player) {
//        if (player == currentPlayer && board[location] == null) {
//            board[location] = currentPlayer;
//            currentPlayer = currentPlayer.opponent;
//            currentPlayer.otherPlayerMoved(location);
//            return true;
//        }
//        return false;
//    }
//
//    /**
//     * The class for the helper threads in this multithreaded server
//     * application.  A Player is identified by a character mark
//     * which is either 'X' or 'O'.  For communication with the
//     * client the player has a socket with its input and output
//     * streams.  Since only text is being communicated we use a
//     * reader and a writer.
//     */
//    class Player extends Thread {
//        char mark;
//        Player opponent;
//        Socket socket;
//        BufferedReader input;
//        PrintWriter output;
//
//        /**
//         * Constructs a handler thread for a given socket and mark
//         * initializes the stream fields, displays the first two
//         * welcoming messages.
//         */
//        public Player(Socket socket, char mark) {
//            this.socket = socket;
//            this.mark = mark;
//            try {
//                input = new BufferedReader(
//                        new InputStreamReader(socket.getInputStream()));
//                output = new PrintWriter(socket.getOutputStream(), true);
//                output.println("WELCOME " + mark);
//                output.println("MESSAGE Waiting for opponent to connect");
//            } catch (IOException e) {
//                System.out.println("Player died: " + e);
//            }
//        }
//
//        /**
//         * Accepts notification of who the opponent is.
//         */
//        public void setOpponent(Player opponent) {
//            this.opponent = opponent;
//        }
//
//        /**
//         * Handles the otherPlayerMoved message.
//         */
//        public void otherPlayerMoved(int location) {
//            output.println("OPPONENT_MOVED " + location);
//            output.println(
//                    hasWinner() ? "DEFEAT" : boardFilledUp() ? "TIE" : "");
//        }
//
//        /**
//         * The run method of this thread.
//         */
//        public void run() {
//            try {
//                // The thread is only started after everyone connects.
//                output.println("MESSAGE All players connected");
//
//                // Tell the first player that it is her turn.
//                if (mark == 'X') {
//                    output.println("MESSAGE Your move");
//                }
//
//                // Repeatedly get commands from the client and process them.
//                while (true) {
//                    String command = input.readLine();
//                    if (command.startsWith("MOVE")) {
//                        int location = Integer.parseInt(command.substring(5));
//                        if (legalMove(location, this)) {
//                            output.println("VALID_MOVE");
//                            output.println(hasWinner() ? "VICTORY"
//                                    : boardFilledUp() ? "TIE"
//                                    : "");
//                        } else {
//                            output.println("MESSAGE ?");
//                        }
//                    } else if (command.startsWith("QUIT")) {
//                        return;
//                    }
//                }
//            } catch (IOException e) {
//                System.out.println("Player died: " + e);
//            } finally {
//                try {socket.close();} catch (IOException e) {}
//            }
//        }
//    }
//}
import java.io.*;
import java.net.InetSocketAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Random;

//Server object containing the server administrative duties.
public class Server {


    static String[] dictionary = {"banana", "mistake", "hopons", "her",
            "illusion", "marryme", "blueman", "bees", "cornball", "cousin",
            "family", "seal", "poppop", "freebie", "franklin"};

    private ServerSocket listener;
    private final ArrayList<Game> games = new ArrayList<>();

    //Constructor requiring a port number
    private Server(int port) throws Exception {
        listener = new ServerSocket();
        InetSocketAddress listenerAddress = new InetSocketAddress(port);
        listener.bind(listenerAddress);
    }

    //Function that accepts connections coming into the server.
    private void acceptIncomingConnections() throws Exception {
        System.out.println("Hangman Server is Running");
        //noinspection InfiniteLoopStatement
        while (true) {
            Socket temp = listener.accept();
            int i = games.size() - 1;
            while (i > 0) {
                if (games.get(i).getState() != Thread.State.RUNNABLE) {
                    games.remove(i);
                }
                i--;
            }
            Game g = new Game(temp, games.size());
            if (games.size() < 3) {
                games.add(g);
                g.start();
            } else {
                g.start();
            }
        }
    }

    //main method
    public static void main(String[] args) throws Exception {
        if (args.length > 1) {
            readTextFile(args[1]);
        }
        Server server = new Server(Integer.parseInt(args[0]));
        server.acceptIncomingConnections();
    }

    //Static helper method to read in text file.
    private static void readTextFile(String file) throws IOException {
        boolean firstPass = true;
        int numWords;
        int counter = 0;
        for (String line : Files.readAllLines(Paths.get(file))) {
            if (firstPass) {
                String[] numbers = line.split(" ");
                numWords = Integer.parseInt(numbers[numbers.length - 1]);
                dictionary = new String[numWords];
                firstPass = false;
            } else {
                dictionary[counter] = line;
                counter++;
            }
        }
     }
}

//Game object that contains all game logic
class Game extends Thread {

    private final Socket clientSocket;
    private final byte[] readBuffer;
    private final String word;
    private final byte[] emptyWordArray;
    private final byte[] guessArray;
    private DataOutputStream writer;
    private int numIncorrect;
    private final int wordLength;
    private InputStream stream;
    private boolean  runGame = true;
    private final int gamesLength;
    private int msgFlag;

    //Game constructor that requires a socket and the number of ongoing games
    Game(Socket clientSocket, int gamesLength) {
        readBuffer = new byte[50];
        this.clientSocket = clientSocket;
        numIncorrect = 0;
        word = Server.dictionary[new Random().nextInt(15)];
        wordLength = word.length();
        guessArray = new byte[6];
        emptyWordArray = new byte[wordLength];
        for (int i =0;i<wordLength;i++) {
            emptyWordArray[i] = (byte)'_';
        }
        this.gamesLength =  gamesLength;
    }

    //Run method required to extend Thread
    //Contains the connection logic
    public void run() {
        try {
            System.out.println("Connection from "
                    + clientSocket.getInetAddress() + ":"
                    + clientSocket.getPort());
            writer = new DataOutputStream(clientSocket.getOutputStream());
            stream = clientSocket.getInputStream();

            if (gamesLength >= 3) {
                writeMessage("Server overloaded.");
            } else {
                this.playGame();
            }

            writer.close();
            stream.close();
            System.out.println("Terminating connection "
                    + clientSocket.getInetAddress()
                    + ":" +clientSocket.getPort());

            clientSocket.close();

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    //Initializes the game
    private void startGame() throws Exception {
        readMessage();
        if (msgFlag == 0) {
            System.out.println("Game Initiated");
            System.out.println("Word is: " + this.word);
            this.writeControl();
        } else {
            runGame = false;
        }

    }

    //The main game loop
    private void playGame() throws Exception {
        this.startGame();
        while (runGame) {
            this.updateGame(this.readMessage());
            if (this.didLose()) {
                this.writeMessage("The word was " + this.word + ".\nYou Lose!");
                runGame = false;
            } else if (this.didWin()) {
                this.writeMessage("The word was " + this.word + ".\nYou Win!");
                this.writeMessage("You Win");
                runGame = false;
            } else {
                this.writeControl();
            }
        }
    }

    //Checks if the game was won
    private boolean didWin(){
        for (byte anEmptyWordArray : this.emptyWordArray) {
            if ((char) anEmptyWordArray == '_') {
                return false;
            }
        }
        return true;
    }

    //Checks if the game was lost
    private boolean didLose() {
        return this.numIncorrect >= 6;
    }

    //Updates the game data based on a player's guess
    private void updateGame(String message) {
        for (int indexLocation :
                this.findCharactersInWord(message.charAt(0))) {
            this.emptyWordArray[indexLocation] = (byte) message.charAt(0);
        }
        if (this.findCharactersInWord(message.charAt(0)).size() == 0) {
            this.numIncorrect++;
            for (int i = 0; i < this.guessArray.length; i++) {
                if (this.guessArray[i] == 0) {
                    this.guessArray[i] = (byte) message.charAt(0);
                    break;
                }
            }
        }
    }

    //Finds all of the matching characters in the word
    //returns a list of their indices
    private ArrayList<Integer> findCharactersInWord(char character) {
        ArrayList<Integer> charArray = new ArrayList<>();
        for (int i = 0; i < word.length(); i++) {
            if (Character.toLowerCase(word.charAt(i))
                    == Character.toLowerCase(character)) {
                charArray.add(i);
            }
        }
        return charArray;
    }

    //Formats and writes a message to the client
    @SuppressWarnings("Duplicates")
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

    //Formats and writes a control message to the client
    private void writeControl() throws Exception{
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        out.write((byte) 0);
        out.write((byte) wordLength);
        out.write((byte) numIncorrect);
        byte[] data = new String(this.emptyWordArray).getBytes();
        out.write(data);
        out.write(guessArray,0,numIncorrect);
        byte[] controlMessage = out.toByteArray();
        this.writer.write(controlMessage,0,wordLength+3+numIncorrect);
    }

    //Reads and formats a message from the client
    private String readMessage() throws Exception {
        msgFlag = this.stream.read();
        //System.out.println(readBuffer);
        //System.out.println(msgFlag);
        String message;
        if (msgFlag > 0) {
            //noinspection ResultOfMethodCallIgnored
            this.stream.read(this.readBuffer);
            message = new String(readBuffer);
            return message.substring(0,msgFlag);
        }
        else {
            return "";
        }
    }

}

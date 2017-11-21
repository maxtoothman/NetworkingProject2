package src;

import java.io.*;
import java.net.InetSocketAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.ArrayList;
import java.util.Queue;
import java.util.Random;
import java.util.Scanner;


public class HangmanServer {

    static String[] dictionary = {"banana", "mistake", "hopons", "her",
            "illusion", "marryme", "blueman", "bees", "cornball", "cousin",
            "family", "seal", "poppop", "freebie", "franklin"};

    private ServerSocket listener;
    private boolean healthyConnection = true;


    private HangmanServer() throws Exception {
        listener = new ServerSocket();
        InetSocketAddress listenerAddress = new InetSocketAddress(8080);
        listener.bind(listenerAddress);
    }

    private void acceptIncomingConnetions() throws Exception {
        System.out.println("Hangman Server is Running");
        while (healthyConnection) {
            new Game(listener.accept()).start();
        }
    }




    public static void main(String[] args) throws Exception {
        HangmanServer server = new HangmanServer();
        server.acceptIncomingConnetions();
    }
}

class Game extends Thread {

    private Socket clientSocket;
    private byte[] readBuffer;
    private String word;
    private byte[] emptyWordArray;
    private byte[] guessArray;
    private DataOutputStream writer;
    private int numIncorrect;
    private int wordLength;
    private InputStream stream;
    private boolean  runGame = true;

    Game(Socket clientSocket) throws Exception{
        readBuffer = new byte[50];
        this.clientSocket = clientSocket;
        numIncorrect = 0;
        word = HangmanServer.dictionary[new Random().nextInt(15)];
        wordLength = word.length();
        guessArray = new byte[6];
        emptyWordArray = new byte[wordLength];
        for (int i =0;i<wordLength;i++) {
            emptyWordArray[i] = (byte)'_';
        }
        //System.out.println("Empty word array is " + Arrays.toString
                //(emptyWordArray));


    }

    public void run() {
        try {
            writer = new DataOutputStream(clientSocket.getOutputStream());
            stream = clientSocket.getInputStream();

            this.playGame();

            writer.close();
            stream.close();

            clientSocket.close();

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private String getWord() {
        return this.word;
    }

    private void startGame() throws Exception {
        if (readMessage().length() == 0) {
            System.out.println("Game Initiated");
            System.out.println("Word is: " + this.getWord());
            this.writeControl();
        } else {
            runGame = false;
        }

    }

    void playGame() throws Exception {
        this.startGame();
        while (runGame) {
            this.updateGame(this.readMessage());
            if (this.didLose()) {
                this.writeMessage("You Lose");
                runGame = false;
            } else if (this.didWin()) {
                this.writeMessage("You Win");
                runGame = false;
            } else {
                this.writeControl();
            }
        }
    }

    private boolean didWin(){
        for (byte anEmptyWordArray : this.emptyWordArray) {
            if ((char) anEmptyWordArray == '_') {
                return false;
            }
        }
        return true;
    }

    private boolean didLose() {
        return this.numIncorrect >= 6;
    }

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

    private ArrayList<Integer> findCharactersInWord(char character) {
        ArrayList<Integer> charArray = new ArrayList<>();
        for (int i = 0; i < word.length(); i++) {
            if (word.charAt(i) == character) {
                charArray.add(i);
            }
        }
        return charArray;
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
        byte[] data = new String(this.emptyWordArray).getBytes();
        out.write(data);
        out.write(guessArray,0,numIncorrect);
        byte[] controlMessage = out.toByteArray();
        this.writer.write(controlMessage,0,wordLength+3+numIncorrect);
    }

    String readMessage() throws Exception {
        int msgFlag = this.stream.read();
        //System.out.println(readBuffer);
        //System.out.println(msgFlag);
        String message;
        if (msgFlag > 0) {
            this.stream.read(this.readBuffer);
//            System.out.println("Read message status: " + status);
            message = new String(readBuffer);
            return message.substring(0,msgFlag);
        }
        else {
            return "";
        }
    }

}

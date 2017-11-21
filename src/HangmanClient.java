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
    private Message lastMessage = new Message();

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
        this.writeMessage(message);
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
        System.out.printf("Next Letter To Guess: ");
        String message = this.askUserInput();
        this.writeMessage(message);
    }

    private void checkMessage() throws IOException {
        if (lastMessage.messageFlag > 0) {
            this.stream.read(this.readBuffer);
            lastMessage.message = new String(readBuffer).substring(0,
                    lastMessage.messageFlag);
            System.out.println(lastMessage.message);
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
            System.out.println("Incorrect Guesses" + guesses);
        }
    }


    private void readMessage() throws Exception{
        lastMessage.messageFlag = this.stream.read();
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

class Message {
    int messageFlag;
    String message;

    public int getMessageFlag() {
        return messageFlag;
    }

    public void setMessageFlag(int messageFlag) {
        this.messageFlag = messageFlag;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }
}





//        in = new BufferedReader(new InputStreamReader(
//                socket.getInputStream()));
//        out = new PrintWriter(socket.getOutputStream(), true);
//
//        // Layout GUI
//        messageLabel.setBackground(Color.lightGray);
//        frame.getContentPane().add(messageLabel, "South");
//
//        JPanel boardPanel = new JPanel();
//        boardPanel.setBackground(Color.black);
//        boardPanel.setLayout(new GridLayout(3, 3, 2, 2));
//        for (int i = 0; i < board.length; i++) {
//            final int j = i;
//            board[i] = new Square();
//            board[i].addMouseListener(new MouseAdapter() {
//                public void mousePressed(MouseEvent e) {
//                    currentSquare = board[j];
//                    out.println("MOVE " + j);}});
//            boardPanel.add(board[i]);
//        }
//        frame.getContentPane().add(boardPanel, "Center");
//    }
//
//    /**
//     * The main thread of the client will listen for messages
//     * from the server.  The first message will be a "WELCOME"
//     * message in which we receive our mark.  Then we go into a
//     * loop listening for "VALID_MOVE", "OPPONENT_MOVED", "VICTORY",
//     * "DEFEAT", "TIE", "OPPONENT_QUIT or "MESSAGE" messages,
//     * and handling each message appropriately.  The "VICTORY",
//     * "DEFEAT" and "TIE" ask the user whether or not to play
//     * another game.  If the answer is no, the loop is exited and
//     * the server is sent a "QUIT" message.  If an OPPONENT_QUIT
//     * message is recevied then the loop will exit and the server
//     * will be sent a "QUIT" message also.
//     */
//    public void play() throws Exception {
//        System.out.println("Play");
//        String response;
//        try {
//            response = in.readLine();
//            if (response.startsWith("WELCOME")) {
//                char mark = response.charAt(8);
//                icon = new ImageIcon(mark == 'X' ? "x.gif" : "o.gif");
//                opponentIcon  = new ImageIcon(mark == 'X' ? "o.gif" : "x.gif");
//                frame.setTitle("Tic Tac Toe - Player " + mark);
//            }
//            while (true) {
//                response = in.readLine();
//                if (response.startsWith("VALID_MOVE")) {
//                    messageLabel.setText("Valid move, please wait");
//                    currentSquare.setIcon(icon);
//                    currentSquare.repaint();
//                } else if (response.startsWith("OPPONENT_MOVED")) {
//                    int loc = Integer.parseInt(response.substring(15));
//                    board[loc].setIcon(opponentIcon);
//                    board[loc].repaint();
//                    messageLabel.setText("Opponent moved, your turn");
//                } else if (response.startsWith("VICTORY")) {
//                    messageLabel.setText("You win");
//                    break;
//                } else if (response.startsWith("DEFEAT")) {
//                    messageLabel.setText("You lose");
//                    break;
//                } else if (response.startsWith("TIE")) {
//                    messageLabel.setText("You tied");
//                    break;
//                } else if (response.startsWith("MESSAGE")) {
//                    messageLabel.setText(response.substring(8));
//                }
//            }
//            out.println("QUIT");
//        }
//        finally {
//            socket.close();
//        }
//    }
//
//    private boolean wantsToPlayAgain() {
//        int response = JOptionPane.showConfirmDialog(frame,
//                "Want to play again?",
//                "Tic Tac Toe is Fun Fun Fun",
//                JOptionPane.YES_NO_OPTION);
//        frame.dispose();
//        return response == JOptionPane.YES_OPTION;
//    }
//
//    /**
//     * Graphical square in the client window.  Each square is
//     * a white panel containing.  A client calls setIcon() to fill
//     * it with an Icon, presumably an X or O.
//     */
//    static class Square extends JPanel {
//        JLabel label = new JLabel((Icon)null);
//
//        public Square() {
//            setBackground(Color.white);
//            add(label);
//        }
//
//        public void setIcon(Icon icon) {
//            label.setIcon(icon);
//        }
//    }
//
//    /**
//     * Runs the client as an application.
//     */
//    public static void main(String[] args) throws Exception {
//        while (true) {
//            String serverAddress = (args.length == 0) ? "localhost" : args[1];
//            TicTacToeClient client = new TicTacToeClient(serverAddress);
//            client.frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
//            client.frame.setSize(240, 160);
//            client.frame.setVisible(true);
//            client.frame.setResizable(false);
//            client.play();
//            if (!client.wantsToPlayAgain()) {
//                break;
//            }
//        }
//    }
//}
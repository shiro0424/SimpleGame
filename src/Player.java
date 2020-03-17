import java.awt.Container;
import java.awt.GridLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.net.Socket;

import javax.swing.JButton;
import javax.swing.JFrame;
import javax.swing.JTextArea;

public class Player extends JFrame {
	private int width;
	private int height;
	private Container contentPane;
	private JTextArea message;
	private JButton b1, b2, b3, b4;
	private int playerID;
	private int otherPlayer;
	private int[] values;
	private int maxTurns;
	private int turnsMade;
	private int myPoints;
	private int enemyPoints;
	private boolean buttonsEnabled;
	
	private ClientSideConnection csc;
	
	public Player(int w, int h) {
		width = w;
		height = h;
		contentPane = this.getContentPane();
		message = new JTextArea();
		b1 = new JButton("1");
		b2 = new JButton("2");
		b3 = new JButton("3");
		b4 = new JButton("4");
		values = new int[4];
		turnsMade = 0;
		myPoints = 0;
		enemyPoints = 0;
	}
	
	public void setupGUI() {
		this.setSize(width, height);
		this.setTitle("Player #" + playerID);
		this.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
		contentPane.setLayout(new GridLayout(1, 5));
		contentPane.add(message);
		message.setText("Creating a simple turn-based game in Java.");
		message.setWrapStyleWord(true);
		message.setLineWrap(true);
		message.setEditable(false);
		contentPane.add(b1);
		contentPane.add(b2);
		contentPane.add(b3);
		contentPane.add(b4);
		
		if(playerID == 1) {
			message.setText("You are player #1. You go first.");
			otherPlayer = 2;
			buttonsEnabled = true;
		}else {
			message.setText("You are player #2. Wait for your turn.");
			otherPlayer = 1;
			buttonsEnabled = false;
			Thread t = new Thread(new Runnable() {
				public void run() {
					updateTurn();
				}
			});
			t.start();
		}
		
		toggleButtons();
		
		this.setVisible(true);
	}
	
	public void connectToServer() {
		csc = new ClientSideConnection();
	}
	
	public void setupButtons() {
		ActionListener al = new ActionListener() {
			public void actionPerformed(ActionEvent ae) {
				JButton b = (JButton) ae.getSource();
				int bNum = Integer.parseInt(b.getText());
				
				message.setText("You clicked button #" + bNum + ". Now wait for player #" + otherPlayer);
				turnsMade++;
				System.out.println("Turns made: " + turnsMade);
				
				buttonsEnabled = false;
				toggleButtons();
				
				myPoints += values[bNum - 1];
				System.out.println("My points: " + myPoints);
				csc.sendButtonNum(bNum);
				
				if(playerID == 2 && turnsMade == maxTurns) {
					checkWinner();
				}else {
					Thread t = new Thread(new Runnable() {
						public void run() {
							updateTurn();
						}
					});
					t.start();
				}
				
			}
		};
		
		b1.addActionListener(al);
		b2.addActionListener(al);
		b3.addActionListener(al);
		b4.addActionListener(al);
	}
	
	public void toggleButtons() {
		b1.setEnabled(buttonsEnabled);
		b2.setEnabled(buttonsEnabled);
		b3.setEnabled(buttonsEnabled);
		b4.setEnabled(buttonsEnabled);
	}
	
	public void updateTurn() {
		int n = csc.receiveButtonNum();
		message.setText("Your enemy clicked button #" + n + ". Your turn.");
		enemyPoints += values[n - 1];
//		System.out.println("Your enemy has " + enemyPoints + " points.");
		if(playerID == 1 && turnsMade == maxTurns) {
			checkWinner();
		}else {
			buttonsEnabled = true;
		}
		toggleButtons();
	}
	
	private void checkWinner() {
		buttonsEnabled = false;
		if(myPoints > enemyPoints) {
			message.setText("You won!\n" + "You: " + myPoints + "\nEnemy: " + enemyPoints);
		}else if(myPoints < enemyPoints) {
			message.setText("You lost!\n" + "You: " + myPoints + "\nEnemy: " + enemyPoints);
		}else {
			message.setText("It's a tie! You both got " + myPoints + "points.");
		}
		csc.closeConnection();
	}
	
	// Client connection
	private class ClientSideConnection {
		private Socket socket;
		private DataInputStream dis;
		private DataOutputStream dos;
		
		public ClientSideConnection() {
			System.out.println("----Client----");
			try {
				socket = new Socket("localhost", 8765);
				dis = new DataInputStream(socket.getInputStream());
				dos = new DataOutputStream(socket.getOutputStream());
				playerID = dis.readInt();
				
				System.out.println("Connected to server as Player #" + playerID + ".");
				maxTurns = dis.readInt() / 2;
				for(int i = 0; i < 4; i++) {
					values[i] = dis.readInt();
				}
				System.out.println("Max turns: " + maxTurns);
				for(int i = 0; i < 4; i++) {
					System.out.println("Value #" + (i + 1) + " is " + values[i]);
				}
			}catch(IOException e) {
				e.printStackTrace();
			}
		}
		
		public void sendButtonNum(int n) {
			try {
				dos.writeInt(n);
				dos.flush();
			}catch(IOException e) {
				e.printStackTrace();
			}
		}
		
		public int receiveButtonNum() {
			int n = -1;
			try {
				n = dis.readInt();
				System.out.println("Player #" + otherPlayer + " clicked button #" + n);
			}catch(IOException e) {
				e.printStackTrace();
			}
			return n;
		}
		
		public void closeConnection() {
			try {
				socket.close();
				System.out.println("Connection closed.");
			}catch(IOException e) {
				e.printStackTrace();
			}
		}
	}
	
	public static void main(String[] args) {
		Player p = new Player(500, 100);
		p.connectToServer();
		p.setupGUI();
		p.setupButtons();
	}
}

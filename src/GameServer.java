import java.io.*;
import java.net.*;

public class GameServer {
	private ServerSocket ss;
	private int numPlayers;
	private ServerSideConnection player1, player2;
	private int turnsMade;
	private int maxTurns;
	private int[] values;
	private int playerOneButtonNum;
	private int playerTwoButtonNum;
	
	public GameServer() {
		System.out.println("----Game Server");
		numPlayers = 0;
		turnsMade = 0;
		maxTurns = 4;
		values = new int[4];
		
		for(int i = 0; i < values.length; i++) {
			values[i] = (int)Math.ceil(Math.random() * 100);
			System.out.println("value #" + (i + 1) + " is " + values[i]);
		}
		
		try {
			ss = new ServerSocket(8765);
		}catch(IOException e) {
			e.printStackTrace();
		}
	}
	
	public void acceptConnections() {
		try {
			System.out.println("Waiting for connections");
			while(numPlayers < 2) {
				Socket s = ss.accept();
				numPlayers++;
				System.out.println("Player #" + numPlayers + " has connected");
				ServerSideConnection ssc = new ServerSideConnection(s, numPlayers);
				if(numPlayers == 1) {
					player1 = ssc;
				}else {
					player2 = ssc;
				}
				Thread t = new Thread(ssc);
				t.start();
			}
			System.out.println("We now have 2 players. No longer accepting connections");
		}catch(IOException e) {
			e.printStackTrace();
		}
	}
	
	private class ServerSideConnection implements Runnable{
		private Socket socket;
		private DataInputStream dis;
		private DataOutputStream dos;
		private int playerID;
		
		public ServerSideConnection(Socket s, int id) {
			socket = s;
			playerID = id;
			
			try {
				dis = new DataInputStream(socket.getInputStream());
				dos = new DataOutputStream(socket.getOutputStream());
			}catch(IOException e) {
				e.printStackTrace();
			}
		}
		
		public void run() {
			try {
				dos.writeInt(playerID);
				dos.writeInt(maxTurns);
				dos.writeInt(values[0]);
				dos.writeInt(values[1]);
				dos.writeInt(values[2]);
				dos.writeInt(values[3]);
				dos.flush();
				
				while(true) {
					if(playerID == 1) {
						playerOneButtonNum = dis.readInt();
						System.out.println("Player 1 clicked button #" + playerOneButtonNum);
						player2.sendButtonNum(playerOneButtonNum);
					}else {
						playerTwoButtonNum = dis.readInt();
						System.out.println("Player 2 clicked button #" + playerTwoButtonNum);
						player1.sendButtonNum(playerTwoButtonNum);
					}
					turnsMade++;
					if(turnsMade == maxTurns) {
						System.out.println("Max turns have been reached.");
						break;
					}
				}
				player1.closeConnection();
				player2.closeConnection();
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
		GameServer gs = new GameServer();
		gs.acceptConnections();
	}
}

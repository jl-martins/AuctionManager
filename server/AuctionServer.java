import java.net.ServerSocket;
import java.net.Socket;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;

public class AuctionServer{

	public static void main(String[] args) {
		ServerSocket serverSocket;
		Socket socket = null;
		UsersMap users = new UsersMap();
		AuctionsMap auctions = new AuctionsMap();

		try{
			serverSocket = new ServerSocket(8080);
			while((socket = serverSocket.accept()) != null){
				/* This thread will read the command, act on it and send a reply
				   if necessary */
				(new Thread(new ClientThread(socket,users,auctions))).start();
				/* I guess we'll need another thread for notifications and shit */
			}	
		}catch(IOException e){
			/* Log something */
		}
	}
}

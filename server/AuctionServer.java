import java.net.ServerSocket;
import java.net.Socket;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.io.FileNotFoundException;

import java.util.logging.Level;
import java.util.logging.Logger;

import java.util.Timer;
import java.util.TimerTask;

/**
 * TODO: The logs are all logging to stdout apparently, got to change that
 *       Static variable dont get serialized, got to find a away to save the next auction ID variable.
 *              - We should move the assignemt of id's to AuctionsMap, everytime addAuction is called the id is size+1
 *		  that way we dont have static trouble
 */

/**
 * There are two ways of implementing a second thread that would save the state of the program every hour
 * 1º: Create a thread, have it sleep for an hour then save the state, sleep for an hour, save the state, and so on and so forward
 * 2º: Use Timer and TimerTask 
 */


public class AuctionServer{
	private static UsersMap users;
	private static AuctionsMap auctions;

	public static void main(String[] args) {
		ServerSocket serverSocket;
		Socket socket = null;
		
		try{	
			users = UsersMap.readObj("UsersMap.ser");
			auctions = AuctionsMap.readObj("AuctionsMap.ser");
		
		}catch(IOException | ClassNotFoundException e){
			
			users = new UsersMap();
			auctions = new AuctionsMap();
			Logger.getLogger(AuctionServer.class.getName()).log(Level.SEVERE, "Error on reading saved data");
		
		}
		
		/* A not very pretty way to not lose the next auction id */
		Auction.setNextId(auctions.size());	


		try{
			Timer scheduledWriter = new Timer();
			TimerTask hourlyTask = new TimerTask(){
				
				public void run(){
					writeData(users,auctions);
				}
			
			};

			scheduledWriter.scheduleAtFixedRate(hourlyTask, 0, 1000*60*5); /* 5 minutes for testing */
			
			serverSocket = new ServerSocket(8080);
			while((socket = serverSocket.accept()) != null){
				/* This thread will read the command, act on it and send a reply
				   if necessary */
				(new Thread(new ClientThread(socket,users,auctions))).start();
			}	
		
		}catch(IOException e){
			Logger.getLogger(AuctionServer.class.getName()).log(Level.SEVERE, "Server went down", e);
		}
	}
	
	public static void writeData(UsersMap users, AuctionsMap auctions){
		try{
			users.writeObj("UsersMap.ser");
			auctions.writeObj("AuctionsMap.ser");
		
		}catch(IOException e){
			Logger.getLogger(AuctionServer.class.getName()).log(Level.SEVERE, "Error on writing program's state", e);
		}
	}
}

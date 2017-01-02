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


public class AuctionServer {
    private static final Logger logger = Logger.getLogger(AuctionServer.class.getName());
    
	public static void main(String[] args) {
		ServerSocket serverSocket;
		Socket socket = null;
		Counter nextAuctionId;

		/* final variables due to java restriction to only use final variables in innerclasses */
		final UsersMap users;
		final AuctionsMap auctions;
		/* used to circumvent java's warning of possibly assigning two times */
		UsersMap usersTemp = null;
		AuctionsMap auctionsTemp = null; 

		try {
			usersTemp = UsersMap.readObj("UsersMap.ser");
			auctionsTemp = AuctionsMap.readObj("AuctionsMap.ser");
		} catch(IOException | ClassNotFoundException e) {
			usersTemp = new UsersMap();
			auctionsTemp = new AuctionsMap();
			logger.log(Level.SEVERE, "Error on reading saved data");
		}

		users = usersTemp;
		auctions = auctionsTemp;

		/* this will synchronize the auction id atribution */
		nextAuctionId = new Counter(auctions.size());
		Timer scheduledWriter = new Timer();
		TimerTask hourlyTask = new TimerTask() {
			public void run() {
				writeData(users, auctions);
			}
		};

		try {
			scheduledWriter.scheduleAtFixedRate(hourlyTask, 0, 1000*60*5); /* 5 minutes for testing */
			serverSocket = new ServerSocket(8080);
			while((socket = serverSocket.accept()) != null){
				/* This thread will read the command, act on it and send a reply
				   if necessary */
				Thread ct = new Thread(new ClientThread(socket,users,auctions, nextAuctionId));
				ct.start();
			}
		} catch(IOException e) {
			logger.log(Level.SEVERE, "Server went down", e);
		}
	}
	
	public static void writeData(UsersMap users, AuctionsMap auctions) {
		try {
			users.writeObj("UsersMap.ser");
			auctions.writeObj("AuctionsMap.ser");
            
		} catch(IOException e) {
			Logger.getLogger(AuctionServer.class.getName()).log(Level.SEVERE, "Error on writing program's state", e);
		}
	}
}


import java.util.HashSet;
import java.util.Set;
import java.util.Queue;
import java.util.LinkedList;
import java.util.concurrent.locks.ReentrantLock;
import java.util.concurrent.locks.Condition;

import java.io.Serializable;

public class Client implements Serializable {
	private String username;
	private String password;
	private Set<Integer> auctionsOwned;		/* Auctions in which the client is the auctioneer */
	private Set<Integer> auctionsIn;		/* Auctions in which the client is in as a bidder */
	private	Notifications notifications; 	/* FIFO queue of notifications */

	public Client(String username, String password) {
		this.username = username;
		this.password = password;
		this.auctionsOwned = new HashSet<>();
		this.auctionsIn = new HashSet<>();
		this.notifications = new Notifications();
	}

	public String getUsername() {
		return username;
	}

	public String getPassword() {
		return password;
	}

	public Notifications getNotifications() {
		return notifications;
	}

	public void addAuction(int auctionId, boolean asBidder) {
		if(!asBidder)
			auctionsOwned.add(auctionId);
		else
			auctionsIn.add(auctionId);
	}

	public boolean isAuctioneerOf(int auctionId) {
		return auctionsOwned.contains(auctionId);
	}

	public boolean isBidderIn(int auctionId) {
		return auctionsIn.contains(auctionId);
	}
	
	/*
	public void removeAuction(int auctionId) {
		auctionsOwned.remove(auctionId);
	}	
	*/

	public boolean checkPassword(String password) throws AuthorizationException {
		if(!this.password.equals(password))
			throw new AuthorizationException("Invalid credentials");
		
        return true;
	}

	public void add(String notification){
		notifications.add(notification);
	}

	/* equals e toString */
}

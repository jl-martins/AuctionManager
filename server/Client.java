
import java.util.HashSet;
import java.util.Set;
import java.util.Queue;
import java.util.LinkedList;
import java.util.concurrent.locks.ReentrantLock;
import java.util.concurrent.locks.Condition;

public class Client{
	private String username;
	private String password;
	private Set<Integer> auctionsOwned;		/* Auctions in which he's the auctioneer */
	private Set<Integer> auctionsIn;		/* Auctions he's in as a bidder */
	private	Notifications notifications; 	/* FIFO of notifications */

	public Client(String username, String password){
		this.username = username;
		this.password = password;
		this.auctionsOwned = new HashSet<>();
		this.auctionsIn = new HashSet<>();
		this.notifications = new Notifications();
	}

	public String getUsername(){
		return username;
	}

	public String getPassword(){
		return password;
	}

	public Notifications getNotifications(){
		return notifications;
	}

	public void addAuction(int auctionId, boolean asBidder){
		if(!asBidder)
			auctionsOwned.add(auctionId);
		else
			auctionsIn.add(auctionId);
	}

	public boolean isAuctioneerOf(int auctionId){
		return auctionsOwned.contains(auctionId);
	}

	public boolean isBidderIn(int auctionId){
		return auctionsIn.contains(auctionId);
	}
	
	/*
	public void removeAuction(int auctionId){
		auctionsOwned.remove(auctionId);
	}	
	*/

	public boolean checkPassword(String password){
		return this.password.equals(password);
	}

	public void add(String notification){
		notifications.add(notification);
	}

	/* equals e toString */
}

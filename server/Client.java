
import java.util.HashSet;
import java.util.Set;

public class Client{
	private String username;
	private String password;
	private Set<Integer> auctionsOwned;	/* Auctions of which he's the auctioneer */
	private Set<Integer> auctionsIn;	/*Auctions he's is as a bidder */

	public Client(String username, String password){
		this.username = username;
		this.password = password;
		this.auctionsOwned = new HashSet<>();
		this.auctionsIn = new HashSet<>();
	}

	public String getUsername(){
		return username;
	}

	public String getPassword(){
		return password;
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

	/* equals e toString */
}

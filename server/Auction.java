import java.util.Set;
import java.util.HashSet;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.locks.ReentrantLock;

import java.io.Serializable;

public class Auction implements Serializable{
	private final int auctionId;
	private final ReentrantLock auctionLock;
	private final String auctioneer;
	private String description;
	private List<String> licitationHistory;
	private String highestBidder;
	private double highestBid;
	private Set<String> bidders;
	private boolean terminated;

	public Auction(String auctioneer, String description, int id) {
		this.auctioneer = auctioneer;
		this.description = description;
		highestBidder = "";
		highestBid = 0.0;
		licitationHistory = new ArrayList<>();
		auctionLock = new ReentrantLock();
		bidders = new HashSet<>();
		terminated = false;
		auctionId = id; 
	}


	public void lock() {
		auctionLock.lock();
	}

	public void unlock() {
		auctionLock.unlock();
	}
	
	public String getAuctioneer() {
		lock();
		try {
			return auctioneer;
		} finally {
			unlock();
		}
	}
	
	public int getAuctionId() {
		lock();
		try {
			return auctionId;
		} finally {
			unlock();
		}	
	}

	public String getDescription() {
		lock();
		try {
			return description;
		} finally {
			unlock();
		}
	}

	public void bid(String bidder, double bid)
        throws AlreadyHighestBidderException, AuctionOwnerException, InvalidBidException
    {
		lock();
		
		bidders.add(bidder);
		try{
            if(bidder.equals(highestBidder))
                throw new AlreadyHighestBidderException("You cannot bid, because you already have the highest bid!");
            
            if(bidder.equals(auctioneer))
                throw new AuctionOwnerException("You cannot bid in an auction that you created!");
            
			if(bid <= highestBid)
				throw new InvalidBidException("Your bid should be higher than the current highest bid!");
            
            highestBid = bid;
            highestBidder = bidder;
			licitationHistory.add(bidder + ": " + bid);
		} finally {
			unlock();
		}
	}

	public String getHighestBidder() {
		lock();
		try {
			return highestBidder;
		} finally {
			unlock();
		}
	}

	public double getHighestBid() {
		lock();
		try {
			return highestBid;
		} finally {
			unlock();
		}
	}

	public boolean isTerminated() {
		lock();
		try {
			return terminated;
		} finally {
			unlock();
		}
	}

	public void terminate() {
		lock();
		try {
			terminated = true;
		} finally {
			unlock();
		}
	}

	public Set<String> getBidders() {
		lock();
		try {
			return bidders;
		} finally {
			unlock();
		}
	}

	public List<String> readHistory() {
		/*TODO: later */
		return null;
	}
}

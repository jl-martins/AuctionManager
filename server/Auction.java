
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.locks.ReentrantLock;

public class Auction{
	private static int nextId = 0;
	private final int auctionId = ++nextId;
	private ReentrantLock auctionLock;
	private String auctioner;
	private String description;
	private List<String> licitationHistory;
	private String highestBidder;
	private double highestBid;

	public Auction(String auctioner, String description){
		this.auctioner = auctioner;
		this.description = description;
		highestBid = 0.0;
		licitationHistory = new ArrayList<>();
		auctionLock = new ReentrantLock();
	}


	public void lock(){
		auctionLock.lock();
	}

	public void unlock(){
		auctionLock.unlock();
	}
	
	public int getAuctionId(){
		lock();
		try{
			return auctionId;
		}finally{
			unlock();
		}	
	}

	public String getDescription(){
		lock();
		try{
			return description;
		}finally{
			unlock();
		}
	}

	public void bid(String bidder, double bid){
		lock();
		try{
			if(bid > highestBid){
				highestBid = bid;
				highestBidder = bidder;
			}

			licitationHistory.add(bidder+": "+bid);
		}finally{
			unlock();
		}
	}

	public String getHighestBidder(){
		lock();
		try{
			return highestBidder;
		}finally{
			unlock();
		}
	}

	public double getHighestBid(){
		lock();
		try{
			return highestBid;
		}finally{
			unlock();
		}
	}

	public List<String> readHistory(){
		/*TODO: later */
		return null;
	}
	
}

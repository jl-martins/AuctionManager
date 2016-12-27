import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.locks.ReentrantLock;

public class AuctionsMap{
	private Map<Integer, Auction> auctions;
	private int closedAuctions;
	private ReentrantLock auctionsLock;
	
	public AuctionsMap(){
		auctions = new HashMap<>();
		auctionsLock = new ReentrantLock();
		closedAuctions = 0;
	}

	public void lock(){
		auctionsLock.lock();
	}

	public void unlock(){
		auctionsLock.unlock();
	}

	public void addAuction(Auction a){
		lock();
		try{
			auctions.put(a.getAuctionId(), a);
		}finally{
			unlock();
		}
	}

	public Auction get(int auctionId){
		lock();
		try{
			return auctions.get(auctionId);
		}finally{
			unlock();
		}
	}

	public boolean containsAuction(int id){
		lock();
		try{
			return auctions.containsKey(id);
		}finally{
			unlock();
		}
	}

	public int size(){
		lock();
		try{
			return auctions.size();
		}finally{
			unlock();
		}
	}
	
	public Collection<Auction> values() {
		return auctions.values();
	}

	public int getClosedAuctions(){
		lock();
		try{
			return closedAuctions;
		}finally{
			unlock();
		}
	}

	public void incClosedAuctions(){
		lock();
		try{
			closedAuctions += 1;
		}finally{
			unlock();
		}
	}

}
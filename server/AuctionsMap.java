import java.io.IOException;
import java.io.ObjectOutputStream;
import java.io.FileOutputStream;
import java.io.ObjectInputStream;
import java.io.FileInputStream;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.locks.ReentrantLock;

import java.io.Serializable;

public class AuctionsMap implements Serializable {
	private Map<Integer, Auction> auctions;
	private int closedAuctions;
	private ReentrantLock auctionsLock;
	
	public AuctionsMap() {
		auctions = new HashMap<>();
		auctionsLock = new ReentrantLock();
		closedAuctions = 0;
	}

	public void lock() {
		auctionsLock.lock();
	}

	public void unlock() {
		auctionsLock.unlock();
	}

	public void addAuction(Auction a) {
		lock();
		try {
			auctions.put(a.getAuctionId(), a);
		} finally {
			unlock();
		}
	}

	public Auction get(int auctionId) {
		lock();
		try {
			return auctions.get(auctionId);
		} finally {
			unlock();
		}
	}

	public boolean containsAuction(int id) {
		lock();
		try {
			return auctions.containsKey(id);
		} finally {
			unlock();
		}
	}

	public int size() {
		lock();
		try {
			return auctions.size();
		} finally {
			unlock();
		}
	}
	
	public Collection<Auction> values() {
		lock();
		try {
			return auctions.values();
		} finally {
			unlock();
		}
	}

	public Collection<Integer> keys() {
		lock();
		try {
			return auctions.keySet();
		} finally {
			unlock();
		}
	}

	public int getClosedAuctions() {
		lock();
		try {
			return closedAuctions;
		} finally {
			unlock();
		}
	}

	public void incClosedAuctions() {
		lock();
		try {
			closedAuctions += 1;
		} finally {
			unlock();
		}
	}

	public static AuctionsMap readObj(String file) throws IOException, ClassNotFoundException {
		ObjectInputStream ois = new ObjectInputStream(new FileInputStream(file));
		AuctionsMap auctions = (AuctionsMap) ois.readObject();
		ois.close();
		return auctions; 
	}

	public void writeObj(String file) throws IOException{
		ObjectOutputStream oos = new ObjectOutputStream(new FileOutputStream(file));
        lock();
		oos.writeObject(this);
        unlock();
		oos.flush();
		oos.close();
	}

}

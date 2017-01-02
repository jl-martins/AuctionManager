/* Just a String wrapper class really */
/* Immutable so i guess we dont really need this class */
/* Im keeping it for now */

import java.util.List;
import java.util.LinkedList;
import java.util.Queue;
import java.util.concurrent.locks.ReentrantLock;
import java.util.concurrent.locks.Condition;

import java.io.Serializable;

public class Notifications implements Serializable{
	private Queue<String> buffer;
	private ReentrantLock bufferLock;
	private Condition available;

	public Notifications(){
		buffer = new LinkedList<>();
		bufferLock = new ReentrantLock();
		available = bufferLock.newCondition();
	}
	
	public Condition getAvailableCondition(){
		return available;
	}

	public void lock(){
		bufferLock.lock();
	}

	public void unlock(){
		bufferLock.unlock();
	}

	public void add(String notification){
		lock();
        try {
            buffer.add(notification);
            available.signal();
        } finally {
            unlock();
        }
	}

	public int size(){
		lock();
		try{
			return buffer.size();
		}finally{
			unlock();
		}
	}

	public String get(){
		lock();
		try{
			return buffer.remove();
		}finally{
			unlock();
		}
	}
    
    public boolean isLocked() {
        return bufferLock.isLocked();
    }
}

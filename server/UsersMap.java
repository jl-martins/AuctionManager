
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.locks.ReentrantLock;

public class UsersMap{
	private Map<String, Client> users;
	private ReentrantLock usersLock;

	public UsersMap(){
		users = new HashMap<>();
		usersLock = new ReentrantLock();
	}

	public void lock(){
		usersLock.lock();
	}

	public void unlock(){
		usersLock.unlock();
	}
	
	public void addUser(Client c){
		lock();
		try{
			users.put(c.getUsername(), c);
		}finally{
			unlock();
		}
	}

	public void addUser(String username, String password){
		lock();
		try{
			Client c = new Client(username, password);
			users.put(username, c);
		}finally{
			unlock();
		}
	}

	public Client get(String username){
		lock();
		try{
			return users.get(username);
		}finally{
			unlock();
		}
	}

	public boolean containsUser(String username){
		lock();
		try{
			return users.containsKey(username);
		}finally{
			unlock();
		}
	}
}

import java.io.PrintWriter;
import java.util.concurrent.locks.Condition;
import java.util.Queue;
import java.util.LinkedList;

public class NotificationsThread extends Thread{
	private Notifications buffer;
	private PrintWriter socketIn;
	private Condition available;

	public NotificationsThread(Notifications buffer, PrintWriter socketIn){
		this.buffer = buffer;
		this.socketIn = socketIn;
		this.available = buffer.getAvailableCondition();
	}

	public void run(){
		try{
			while(!Thread.currentThread().isInterrupted()){
				buffer.lock();
				StringBuilder str = new StringBuilder();
				int size;

				while(!((size = buffer.size()) > 0))
						available.await();

				for(int i = 0; i < size; i++)
					str.append(buffer.get()).append("\n");

				socketIn.println(str.toString());
				buffer.unlock();
			}
		}catch(InterruptedException e){
		}
	}

	public void cancel(){
		interrupt();
	}


}

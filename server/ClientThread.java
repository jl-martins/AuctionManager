import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.Socket;
import java.util.Set;

public class ClientThread implements Runnable {
	private Socket s;
	private UsersMap users;			/* shared state */
	private AuctionsMap auctions;	/* shared state */
	private BufferedReader output;
	private PrintWriter input;
	private String loggedUser;
	private NotificationsThread nf;

	public ClientThread(Socket s, UsersMap users, AuctionsMap auctions){
		this.s = s;
		this.users = users;
		this.auctions = auctions;
		loggedUser = "";
	}
	
	public void run(){
		try{
			output = new BufferedReader(new InputStreamReader(s.getInputStream()));
			input = new PrintWriter(s.getOutputStream(),true);

			String m;
			while(((m = output.readLine()) != null)){
				String[] cmd = m.split(" ");
				loggedUser = cmd[0].equals("logout") ? "" : loggedUser;
				parse(cmd);
			}
			s.close();
		}catch(IOException e){
		}
		
	}

	/* TODO: validate input */
	public boolean parse(String[] cmd){
		boolean error = false;
		switch(cmd[0]){
			case "login": 
				error = (cmd.length != 3); 
				if(!error && login(cmd[1], cmd[2])){
					users.lock();
					(nf = new NotificationsThread(users.get(loggedUser).getNotifications(), input)).start();
					users.unlock();
				}
				break;
			case "reg": 
				error = (cmd.length != 3);
				if(!error){
					register(cmd[1],cmd[2]);
				}
				break;
			case "start":
				error = (cmd.length < 2);
				if(!error && !loggedUser.equals(""))
					startAuction(cmd);
				break;
			case "list":
				listAuctions();	
				break;
			case "bid":
				error = (cmd.length != 3);
				if(!error){
					try{
						int auctionId = Integer.parseInt(cmd[1]);
						double amount = Double.parseDouble(cmd[2]);
						bid(auctionId, amount);
					}catch(NumberFormatException e){
					}
				}
				break;
			case "close":
				error = (cmd.length != 2);
				if(!error)
					try{
						closeAuction(Integer.parseInt(cmd[1]));
					}catch(NumberFormatException e){
					}
				break;
			case "logout":
				nf.cancel();
				break;
			default:
				System.out.println(cmd[0]);
		}

		return error;
	}

	private String getDescription(String[] cmd){
		/* cmd[0] is the command "start" auction */
		/* Everything in front of that is the description of the item */
		StringBuilder str = new StringBuilder();
		for(int i = 1; i < cmd.length; i++)
			str.append(cmd[i]).append(" ");
		
		return str.toString().trim();
	} 
	
	public void startAuction(String[] cmd){
		int auctionId;
		auctions.lock();
		
		Auction a = new Auction(loggedUser, getDescription(cmd));
		auctionId = a.getAuctionId();
		
		auctions.addAuction(a);
		
		
		auctions.unlock();
	
		users.lock();
		users.get(loggedUser).addAuction(auctionId, false);
		users.unlock();


		input.println("Auction started with id: "+auctionId);
	}

	public void listAuctions(){
		Client c = null;
		StringBuilder str = new StringBuilder();
		int lines = 10;
		int i = 0;

		users.lock();
		c = users.get(loggedUser);
		users.unlock();
	
		auctions.lock();
		try{
			for(Auction a: auctions.values()){
				if(a.isTerminated()) continue;
				++i;
				a.lock();
				int auctionId = a.getAuctionId();

				str.append("[").append(auctionId).append("] ");
				str.append(a.getDescription());
			
				str.append(" Highest Bid: ")	
				if(a.getHighestBid() == 0) str.append("n/a");
				else str.append(a.getHighestBid());

				if(c.isAuctioneerOf(auctionId)){
					str.append("* ");
				}
				else if (a.getHighestBidder().equals(loggedUser)){
					str.append("+ ");	
				}

				str.append("\n");
				a.unlock();
				if(i == lines || (i == auctions.size()-auctions.getClosedAuctions())){
					String s = str.toString();
					input.println(s);
					str.delete(0, s.length()-1);
					i = 0;
				}
			}
		}finally{
			auctions.unlock();
		}
	}

	public void bid(int auctionId, double amount){
		auctions.lock();
		
		/*TODO: case auctionId doesnt exist */
		Auction a = auctions.get(auctionId);
		
		a.lock();
		auctions.unlock();
		
		if(!a.isTerminated())
			a.bid(loggedUser, amount);

		a.unlock();
	}

	public void closeAuction(int auctionId){
		String notification = null;
		auctions.lock();
		Auction a = auctions.get(auctionId);

		a.lock();
		auctions.unlock();
		Set<String> bidders;
		if(!a.isTerminated()){
			StringBuilder str = new StringBuilder();
		
			str.append("Auction #").append(auctionId);
			str.append(" closed with value ").append(a.getHighestBid());
			str.append(" from user ").append(a.getHighestBidder());
		
			notification = str.toString();
			bidders = a.getBidders();
			for(String bidder: bidders){
				users.get(bidder).add(notification);
			}
			a.terminate();
			auctions.incClosedAuctions();
		}
	
		a.unlock();
	}

	public void register(String username, String password){
		users.lock();
		try{
			if(!users.containsUser(username)){
				users.addUser(new Client(username, password));
				input.println("Registration Successful!");
			}
			else{
				input.println("Username already in use!");
			}
		}finally{
			users.unlock();
		}
	}

	public boolean login(String username, String password){
		Client c = null;
		users.lock();
		try{
			if(users.containsUser(username))
				c = users.get(username);
		}finally{
			users.unlock();
		}
		
		boolean isValid = c.checkPassword(password);

		if(isValid) 
			this.loggedUser = username;

		return isValid;
	}

	
	public void logout(){
	}
}



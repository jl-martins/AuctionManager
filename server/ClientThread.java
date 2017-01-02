import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.Socket;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;

public class ClientThread implements Runnable {
	private Socket s;
	private UsersMap users;			/* shared state */
	private AuctionsMap auctions;		/* shared state */
	private BufferedReader output;
	private PrintWriter input;
	private String loggedUser;
	private NotificationsThread nf;
	private Counter nextAuctionId;

	public ClientThread(Socket s, UsersMap users, AuctionsMap auctions, Counter nextAuctionId){
		this.s = s;
		this.users = users;
		this.auctions = auctions;
		loggedUser = "";
		this.nextAuctionId = nextAuctionId;
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
		String descr = getDescription(cmd);
		int auctionId = nextAuctionId.getCurrentValueAndIncrement();
		Auction a = new Auction(loggedUser, descr, auctionId);

		auctions.lock();
		auctions.addAuction(a);
		auctions.unlock();
	
		users.lock();
		users.get(loggedUser).addAuction(auctionId, false); /* we're adding this auction not as a bidder but as a auctioneer, hence asBidder=false */
		users.unlock();


		input.println("Auction started with id: "+auctionId);
		Logger.getLogger(ClientThread.class.getName())
			.log(Level.INFO, "["+loggedUser+"]: Auction started with id: "+auctionId+". Description: "+descr);
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
		if(auctions.size() == auctions.getClosedAuctions()){
			input.println("There are no open auctions.");
		}
		try{
			for(Auction a: auctions.values()){
				if(a.isTerminated()) continue;
				++i;
				a.lock();
				int auctionId = a.getAuctionId();

				str.append("[").append(auctionId).append("] ");
				str.append(a.getDescription());
			
				str.append(" Highest Bid: ");
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
		Auction a;
		auctions.lock();
		try{
			/*TODO: This is ugly..very ugly */
			if(!auctions.containsAuction(auctionId)){
				input.println("There are no in course auctions with that id!");
				return;
			}
			a = auctions.get(auctionId);
			a.lock();
		}finally{
			auctions.unlock();
		}
		
		try{	
			StringBuilder str = new StringBuilder();
			if(!a.isTerminated()){
				String s = a.getHighestBidder();
				a.bid(loggedUser, amount);
				if(!s.equals("")){
					str.append("Your bid in the auction with id: ").append(auctionId);
					str.append(" was passed by ").append(loggedUser);
					str.append("'s bid of ").append(amount);
					users.get(s).add(str.toString());
				}
			}

			Logger.getLogger(ClientThread.class.getName()).log(Level.INFO, "["+loggedUser+"]: bid "+amount+"on "+a.getDescription());
		}catch(InvalidBidException e){
			input.println(e.getMessage());
			Logger.getLogger(ClientThread.class.getName()).log(Level.INFO, "["+loggedUser+"]: tried to bid less than the current highest bid", e);
		}finally{
			a.unlock();
		}
	}

	public void closeAuction(int auctionId){
		String notification = null;
		Auction a;
		auctions.lock();
		try{
			if(!auctions.containsAuction(auctionId)){
				input.println("There are no in course auctions with that id");
				return;
			}
			
			a = auctions.get(auctionId);
			a.lock();
		}finally{
			auctions.unlock();
		}

		try{
			/*TODO: again very ugly */
			if(!a.getAuctioneer().equals(loggedUser)){
				input.println("You're not authorized to close this auction");
				return;
			}
			
			Set<String> bidders;
			if(!a.isTerminated()){
				StringBuilder str = new StringBuilder();
		
				str.append("Auction #").append(auctionId);
				str.append(" closed with value ").append(a.getHighestBid());
				str.append(" from user ").append(a.getHighestBidder());

				notification = str.toString();
				bidders = a.getBidders();
				bidders.add(loggedUser);
				for(String bidder: bidders){
					users.get(bidder).add(notification);
				}
				a.terminate();
				auctions.incClosedAuctions();
			}	
			
			Logger.getLogger(ClientThread.class.getName()).log(Level.INFO, "["+loggedUser+"]: "+notification);
		}finally{
			a.unlock();
		}
	}

	public void register(String username, String password){
		users.lock();
		try{
			if(!users.containsUser(username)){
				users.addUser(new Client(username, password));
				input.println("Registration Successful!");
				Logger.getLogger(ClientThread.class.getName()).log(Level.INFO, "User added with username: "+username);
				
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
		boolean isValid = false;
		users.lock();
		try{
			if(users.containsUser(username))
				c = users.get(username);
			else	
				throw new UserNonexistentException("xl"); /* login error code */
		}catch(UserNonexistentException e){
			input.println(e.getMessage()); 
			Logger.getLogger(ClientThread.class.getName()).log(Level.INFO, "invalid login", e);
			isValid = false;
			return isValid;
		}finally{
			users.unlock();
		}
	
		try{	
			isValid = c.checkPassword(password);

			if(isValid) 
				this.loggedUser = username;
		}catch(AuthorizationException e){
			input.println(e.getMessage());
			Logger.getLogger(ClientThread.class.getName()).log(Level.INFO, "invalid login", e);
		}
		return isValid;
	}
}



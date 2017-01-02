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
	private BufferedReader fromClient;
	private PrintWriter toClient;
	private String loggedUser;
	private NotificationsThread nf;
	private Counter nextAuctionId;
    private static final Logger logger = Logger.getLogger(ClientThread.class.getName());

	public ClientThread(Socket s, UsersMap users, AuctionsMap auctions, Counter nextAuctionId) {
		this.s = s;
		this.users = users;
		this.auctions = auctions;
		loggedUser = "";
		this.nextAuctionId = nextAuctionId;
	}
	
	public void run() {
		try {
			fromClient = new BufferedReader(new InputStreamReader(s.getInputStream()));
			toClient = new PrintWriter(s.getOutputStream(),true);

			String m;
			while(((m = fromClient.readLine()) != null)){
				String[] args = m.split(" ");
				loggedUser = args[0].equals("logout") ? "" : loggedUser;
				parse(args);
			}
			s.close();
		} catch(IOException e) {
            logger.log(Level.SEVERE, e.getMessage(), e);
		}
		
	}

	public void parse(String[] args) {
        // Note: command syntax is validated by the client
		switch(args[0]){
			case "login": 
				users.lock();
                		if(login(args[1], args[2]))
					(nf = new NotificationsThread(users.get(loggedUser).getNotifications(), toClient)).start();
				users.unlock();
				break;
			case "reg": 
				register(args[1],args[2]);
				break;
			case "start":
				if(!loggedUser.equals(""))
					startAuction(args);
				break;
			case "list":
				listAuctions();	
				break;
			case "bid":
				try{
					int auctionId = Integer.parseInt(args[1]);
					double ammount = Double.parseDouble(args[2]);
					bid(auctionId, ammount);
				} catch(NumberFormatException e) {
                    			toClient.println("Auction id must be an integer and the bidded ammount must be decimal");
				}			
				break;
			case "close":
				try {
					closeAuction(Integer.parseInt(args[1]));
				} catch(NumberFormatException e){
                    			toClient.println("Auction id must be an integer");
                		}
				break;
			case "logout":
                nf.stop();
				toClient.println("Logout successful");
				break;
			default: // command syntax is validated by the client, so this default is never reached
		}
	}
    
    public boolean login(String username, String password) {
        Client c = null;
        boolean isValid = false;
        
        users.lock();
        try {
            if(users.containsUser(username))
                c = users.get(username);
            else
                throw new UserNonExistentException("Invalid credentials"); /* login error code */
        } catch(UserNonExistentException e){
            toClient.println(e.getMessage());
            logger.log(Level.INFO, "Invalid credentials: Username = " + username + " Password = " + password, e);
            isValid = false;
            return isValid;
        } finally {
            users.unlock();
        }
        
        try {
            isValid = c.checkPassword(password);
            
            if(isValid) {
                this.loggedUser = username;
                toClient.println("Successful login");
            }
        } catch(AuthorizationException e) {
            toClient.println(e.getMessage());
            logger.log(Level.INFO, "Invalid credentials: Username = " + username + ", Password = " + password, e);
        }
        return isValid;
    }
    
    public void register(String username, String password) {
        users.lock();
        try {
            if(!users.containsUser(username)) {
                users.addUser(new Client(username, password));
                toClient.println("Registration successful!");
                logger.log(Level.INFO, "User added with username: " + username);
                
            } else {
                toClient.println("Username already in use!");
            }
        } finally {
            users.unlock();
        }
    }
    
    private String getDescription(String[] args) {
		/* args[0] is the command "start" auction */
		/* Everything in front of that is the description of the item */
		StringBuilder sb = new StringBuilder();
		for(int i = 1; i < args.length; i++)
			sb.append(args[i]).append(" ");
		
		return sb.toString().trim();
	} 
	
	public void startAuction(String[] args) {
		String descr = getDescription(args);
		int auctionId = nextAuctionId.getCurrentValueAndIncrement();
		Auction a = new Auction(loggedUser, descr, auctionId);

		auctions.lock();
		auctions.addAuction(a);
		auctions.unlock();
	
		users.lock();
		users.get(loggedUser).addAuction(auctionId, false); /* we're adding this auction not as a bidder but as a auctioneer, hence asBidder=false */
		users.unlock();


		toClient.println("Auction started with id: " + auctionId);
		logger.log(Level.INFO, "[" + loggedUser + "]: Auction started with id: " + auctionId + ". Description: " + descr);
	}

	public void listAuctions() {
		Client c = null;
		StringBuilder sb = new StringBuilder();

		users.lock();
		c = users.get(loggedUser);
		users.unlock();
	
		AuctionsMap auctionsAux = auctions.clone();

		if(auctionsAux.size() == auctionsAux.getClosedAuctions())
			toClient.println("There are no open auctions.");

		for(Auction a: auctionsAux.values()){
			if(a.isTerminated())
                continue;

			a.lock();
			int auctionId = a.getAuctionId();
			sb.append("[").append(auctionId).append("] ");
			sb.append("Description: ").append(a.getDescription());
			sb.append("; Highest Bid: ");
			if(a.getHighestBid() == 0.0)
                sb.append("n/a");
			else
                sb.append(a.getHighestBid());
			if(c.isAuctioneerOf(auctionId)){
				sb.append("* ");
			} else if(a.getHighestBidder().equals(loggedUser)) {
				sb.append("+ ");	
			}
			a.unlock();
            String s = sb.toString();
            toClient.println(s);
            sb.delete(0, s.length());
		}
	}

	public void bid(int auctionId, double amount) {
		Auction a;
		auctions.lock();
		try {
			if(!auctions.containsAuction(auctionId)){
				toClient.println("There are no in course auctions with that id!");
				return;
			}
			a = auctions.get(auctionId);
			a.lock();
		} finally {
			auctions.unlock();
		}
		
		try {
			StringBuilder sb = new StringBuilder();
			if(!a.isTerminated()){
				String highestBidder = a.getHighestBidder();
				a.bid(loggedUser, amount);
				if(!highestBidder.equals("")){
					sb.append("Your bid in the auction with id ").append(auctionId);
					sb.append(" was passed by ").append(loggedUser);
					sb.append("'s bid of ").append(amount);
					users.get(highestBidder).add(sb.toString());
				}
			}

			logger.log(Level.INFO, "[" + loggedUser + "]: bid " + amount + " on " + a.getDescription());
		} catch(InvalidBidException e) {
			toClient.println(e.getMessage());
			logger.log(Level.INFO, "[" + loggedUser + "]: tried to bid less than the current highest bid.", e);
        } catch(AlreadyHighestBidderException e) {
            toClient.println(e.getMessage());
            logger.log(Level.INFO, "[" + loggedUser + "]: tried to bid while already having the highest bid.", e);
        } catch(AuctionOwnerException e) {
            toClient.println(e.getMessage());
            logger.log(Level.INFO, "[" + loggedUser + "]: tried to bid in an auction he/she owns.", e);
        }finally {
			a.unlock();
		}
	}

	public void closeAuction(int auctionId) {
		String notification = null;
		Auction a;
        
		auctions.lock();
		try {
			if(!auctions.containsAuction(auctionId)) {
				toClient.println("There are no in course auctions with that id");
				return;
			}
			
			a = auctions.get(auctionId);
			a.lock();
		} finally {
			auctions.unlock();
		}

		try {
			if(!a.getAuctioneer().equals(loggedUser)) {
				toClient.println("You're not authorized to close this auction");
				return;
			}
			
			Set<String> bidders;
			if(!a.isTerminated()) {
				StringBuilder sb = new StringBuilder();
                double highestBid = a.getHighestBid();
                String highestBidder = a.getHighestBidder();
                
                sb.append("Auction #").append(auctionId);
                if(!highestBidder.equals("")) {
                    sb.append(" closed with value ").append(highestBid);
                    sb.append(" from user ").append(highestBidder);
                    users.get(highestBidder).add("You won auction #" + auctionId + " with a bid of " + highestBid);
                } else {
                    sb.append(" closed. No bids were made!");
                }
				notification = sb.toString();
				bidders = a.getBidders();
				bidders.add(loggedUser);
                bidders.remove(highestBidder);
				for(String bidder: bidders) {
					users.get(bidder).add(notification);
				}
				a.terminate();
				auctions.incClosedAuctions();
			}
			logger.log(Level.INFO, "[" + loggedUser + "]: " + notification);
		} finally {
			a.unlock();
		}
	}
}



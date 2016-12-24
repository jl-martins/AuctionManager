import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.Socket;

public class ClientThread implements Runnable {
	private Socket s;
	private UsersMap users;			/* shared state */
	private AuctionsMap auctions;	/* shared state */
	private BufferedReader output;
	private PrintWriter input;
	private String loggedUser;

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
				parse(m.split(" "));
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
				if(!error)
					login(cmd[1], cmd[2]);
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
					}catch(Exception e){
					}
				}
				break;
			case "end":
				input.println("Not yet implemented");
				break;
			case "logout":
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
				++i;
				a.lock();
				int auctionId = a.getAuctionId();

				if(c.isAuctioneerOf(auctionId)){
					str.append("* ");
				}
				else if (a.getHighestBidder().equals(loggedUser)){
					str.append("+ ");
					
				}
				
				str.append("[").append(auctionId).append("] ");
				str.append(a.getDescription());
				str.append("\n");
				a.unlock();
				if(i == lines || (i == auctions.size())){
					input.println(str.toString());
					str = new StringBuilder();
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
		
		a.bid(loggedUser, amount);

		a.unlock();
	}

	public void register(String username, String password){
		users.lock();
		try{
			if(!users.containsUser(username))
				users.addUser(new Client(username, password));
		}finally{
			users.unlock();
		}
		input.println("Registration Successful!");
	}

	public void login(String username, String password){
		Client c = null;
		users.lock();
		try{
			if(users.containsUser(username))
				c = users.get(username);
		}finally{users.unlock();}


		if(c.checkPassword(password)) 
			this.loggedUser = username;
	}
	
	public void logout(){
	}
}



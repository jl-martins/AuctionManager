import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.Socket;

import java.util.Arrays;
import java.util.Set;
import java.util.HashSet;
import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.ReentrantLock;
import java.util.logging.Logger;
import java.util.logging.Level;

public class AuctionClient extends Thread {
	
	private static final String[] loginRegisterOptions = {"Login", "Register", "Exit"};
	private static final String[] helpLines = {
		"Start Auction: start <description>", "List Auctions: list",
		"Bid: bid <auctionId> <amount>", "Close Auction: close <auctionId>",
		"Logout: logout", "Exit: exit"
	};
	private static final Set<String> commands =
		new HashSet<>(Arrays.asList(new String[] {"start", "bid", "list", "close", "logout", "--help", "exit"}));
	/* AuctionClient's logger */
	private static final Logger logger = Logger.getLogger(AuctionClient.class.getName());

	/* Instance variables used in reader thread */
	private final Socket s;
	private final BufferedReader fromServer;
	private final RunFlag runFlag; // indicates if an AuctionClient thread should be running
	
	public AuctionClient(Socket s, BufferedReader fromServer, RunFlag runFlag) {
		this.s = s;
		this.fromServer = fromServer;
		this.runFlag = runFlag;
	}

	/* Run method of the client thread that reads from the socket and writes to stdout. */
	public void run() {
		String serverMessage;
		boolean exitFlag = false;

		try {
			do {
				while(runFlag.getValue() == false) {
					synchronized(runFlag) {
						runFlag.wait(); // wait until master thread tells this thread to run
					}
				}
				serverMessage = fromServer.readLine();
				exitFlag = (serverMessage == null);
				if(!exitFlag)
					System.out.println(serverMessage);
			} while(!exitFlag);

			s.shutdownOutput();
		} catch(IOException e) {
			// HANDLE EXCEPTION
		} catch(InterruptedException e) {
			Thread.currentThread().interrupt();
		}
	}
	
	public static void main(String[] args) throws IOException, InterruptedException {
		Socket s = null; // avoids "variable s might not have been initialized" error
		
		if(args.length == 0) {
			s = new Socket("localhost", 8080);
		} else if(args.length == 1) {
			s = new Socket(args[0], 8080);
		} else if(args.length == 2) {
			try {
				s = new Socket(args[0], Integer.valueOf(args[1]));
			} catch(IllegalArgumentException e) {
				System.err.println("Invalid port number");
				System.exit(1);
			}
		} else {
			System.err.println("Usage: AuctionClient [host [port]]");
		}
		work(s);
	}


	private static void work(Socket s) throws IOException {
		PrintWriter toServer = new PrintWriter(s.getOutputStream(), true);
		BufferedReader stdin = new BufferedReader(new InputStreamReader(System.in));
		BufferedReader fromServer = new BufferedReader(new InputStreamReader(s.getInputStream()));
		RunFlag runFlag = new RunFlag(false);
		AuctionClient readerThread = new AuctionClient(s, fromServer, runFlag);
		boolean exitFlag = false;

		readerThread.start(); // readerThread starts with runFlag set to false. It won't run until after login	
		do {
			exitFlag = !loginAndRegister(stdin, toServer, fromServer);

			if(!exitFlag) {
				/* At this point client is logged in */	
				printHelp();
				runFlag.setValue(true);
				synchronized(runFlag) {
					runFlag.notify(); // tell readerThread to run
				}
			}
			String message;
			String[] cmd;
			boolean logout = false;

			while(!exitFlag && !logout && ((message = stdin.readLine()) != null)) {
				message = message.trim();
				cmd = message.split(" ");
				
				if(!existsCmd(cmd[0])){
					System.out.println("Incorrect syntax. Use --help for instructions.");
				} else {
					logout = cmd[0].equalsIgnoreCase("logout");
					if(logout || (exitFlag = cmd[0].equalsIgnoreCase("exit")))
						runFlag.setValue(false);
					
					sendMessageToServer(message, toServer);
				}
			}
		} while(!exitFlag);

		System.exit(0);
	}

	private static boolean loginAndRegister(BufferedReader stdin, PrintWriter toServer, BufferedReader fromServer)
		throws IOException
	{
		int option = -1;
		boolean exitFlag = false, validLogin = false;

		do {
			try {
				printMenu(loginRegisterOptions);
				option = Integer.valueOf(stdin.readLine());
				if(option == 1)
					validLogin = login(stdin, toServer, fromServer);
				else if(option == 2)
					register(stdin, toServer);
				else if(option == 3)
					exitFlag = true;
				else
					System.err.println("Invalid option!");
			} catch(NumberFormatException e) {
				System.err.println("Invalid option!");
			}
		} while(!validLogin && !exitFlag);

		return exitFlag;
	}

	private static boolean existsCmd(String cmd) {
		return commands.contains(cmd.toLowerCase());
	}

	private static String[] getUsernamePassword(BufferedReader stdin) throws IOException {		
		String[] credentials = new String[2];
		
		System.out.print("Username: ");
		credentials[0] = stdin.readLine();
		System.out.print("Password: ");
		credentials[1] = stdin.readLine();
		
		return credentials;
	}

	private static void register(BufferedReader stdin, PrintWriter toServer){
		try{
			String[] credentials = getUsernamePassword(stdin);
			toServer.println("reg " + credentials[0] + " " + credentials[1]);
		} catch(IOException e) {
			// HANDLE EXCEPTION
		}
	}

	private static boolean login(BufferedReader stdin, PrintWriter toServer, BufferedReader fromServer) {
		boolean validLogin = false;

		try {
			String[] credentials = getUsernamePassword(stdin);
			toServer.println("login " + credentials[0] + " " + credentials[1]);
			validLogin = !fromServer.readLine().equals("xl"); // "xl" means invalid login
			if(!validLogin)
				System.err.println("Invalid login!");
			else
				System.out.println(fromServer.readLine());
		} catch(IOException e){
			// HANDLE EXCEPTION!
		}
		return validLogin;
	}

	private static void printMenu(String[] options) {
		for(int i = 1; i <= options.length; ++i)
			System.out.printf("%d. %s%n", i, options[i-1]);
	}

	private static void printHelp() {
		for(String str : helpLines)
			System.out.println(str);
	}

	private static void sendMessageToServer(String cmd, PrintWriter toServer){
		switch(cmd.split(" ")[0]){
			case "start": 
			case "list":
			case "bid":
			case "close":
			case "logout":
				toServer.println(cmd);
				break;
			case "exit":
				toServer.println("logout");
				break;
			case "--help":
				printHelp();
				break;
			default:
		}
	}
}

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
						System.out.println("WAITING");
						runFlag.wait(); // wait until master thread tells this thread to run
					}
				}
				System.out.println("RUNNING");
				serverMessage = fromServer.readLine();
				exitFlag = (serverMessage == null);
				if(!exitFlag)
					System.out.println(serverMessage);
			} while(!exitFlag);

			s.shutdownOutput();
		} catch(IOException e) {
			logger.log(Level.SEVERE, e.getMessage(), e);
		} catch(InterruptedException e) {
			Thread.currentThread().interrupt();
		}
		System.out.println("EXITING");
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
			System.out.println("Exit flag " + exitFlag);
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
			
				logout = cmd[0].equalsIgnoreCase("logout");
				if(logout || (exitFlag = cmd[0].equalsIgnoreCase("exit")))
					runFlag.setValue(false);
					
				sendMessageToServer(message, toServer);
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
					register(stdin, toServer, fromServer);
				else if(option == 3)
					exitFlag = true;
				else
					System.err.println("Invalid option!");
			} catch(NumberFormatException e) {
				System.err.println("Invalid option!");
			}
		} while(!validLogin && !exitFlag);

		return validLogin;
	}

	private static String[] getUsernamePassword(BufferedReader stdin) throws IOException {		
		String[] credentials = new String[2];
		
		System.out.print("Username: ");
		credentials[0] = stdin.readLine();
		System.out.print("Password: ");
		credentials[1] = stdin.readLine();
		
		return credentials;
	}

	private static boolean login(BufferedReader stdin, PrintWriter toServer, BufferedReader fromServer)
		throws IOException
	{
        boolean validLogin = false;
        String serverReply = null;
        String[] credentials = getUsernamePassword(stdin);
            
        if(credentials[0].trim().isEmpty() || credentials[1].trim().isEmpty())
            System.out.println("Invalid credentials.");
        else {
            toServer.println("login " + credentials[0] + " " + credentials[1]);
            serverReply = fromServer.readLine();
            System.out.println(serverReply);
            validLogin = !serverReply.equals("Invalid credentials");
        }
		return validLogin;
	}

	private static void register(BufferedReader stdin, PrintWriter toServer, BufferedReader fromServer)
		throws IOException
	{
		String[] credentials = getUsernamePassword(stdin);
            
        if(credentials[0].trim().isEmpty() || credentials[1].trim().isEmpty())
            System.err.println("Username and password must not be empty.");
        else {
            toServer.println("reg " + credentials[0] + " " + credentials[1]);
            System.out.println(fromServer.readLine());
        }
	}

	private static void printMenu(String[] options) {
		for(int i = 1; i <= options.length; ++i)
			System.out.printf("%d. %s%n", i, options[i-1]);
	}

	private static void printHelp() {
		for(String str : helpLines)
			System.out.println(str);
	}

	private static void sendMessageToServer(String cmd, PrintWriter toServer) {
        String[] args = cmd.split(" ");
        boolean incorrectSyntax = false;
        // Command syntax is validated here, to spare the server from syntax validations
        switch(args[0]) {
			case "start":
                if(args.length < 2)
                	incorrectSyntax = true;
                break;
			case "list":
				if(args.length != 1)
					incorrectSyntax = true;
				break;
			case "bid":
				if(args.length != 3)
					incorrectSyntax = true;
				else try {
					Integer.parseInt(args[1]);
					Double.parseDouble(args[2]);
				} catch(NumberFormatException e) {
					System.err.println("Auction id must be an integer and the bidded ammount must be decimal");
					incorrectSyntax = true;
				}
				break;
			case "close":
				if(args.length != 2)
					incorrectSyntax = true;
				else try {
					Integer.parseInt(args[1]);
				} catch(NumberFormatException e) {
					System.err.println("Auction id must be an integer");
					incorrectSyntax = true;
				}
				break;
			case "logout":
				break;
			case "exit":
				cmd = "logout";
				break;
			case "help":
				printHelp();
				break;
			default: // invalid command
                incorrectSyntax = true;
                break;
		}
		if(incorrectSyntax)
			System.err.println("Incorrect syntax. Use help for instructions.");
		else
			toServer.println(cmd);
	}
}

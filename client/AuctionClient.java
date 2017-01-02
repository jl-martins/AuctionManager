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
	private static final String PROMPT = ">>> ";
    private static final int EXIT = 0, LOGIN = 1, LOGOUT = 2, CONTINUE = 3; // constants used for regulating login and main menu loops
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
		} catch(IOException e) {
			logger.log(Level.SEVERE, e.getMessage(), e);
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
		int r = CONTINUE;

		readerThread.start(); // readerThread starts with runFlag set to false. It won't run until after login	
		do {
			r = loginAndRegister(stdin, toServer, fromServer); // run the login and register loop and store its return value in r

			if(r == LOGIN)
				r = mainMenu(stdin, toServer, runFlag, readerThread); // during the main menu loop, server messages are read by readerThread

		} while(r != EXIT);

		runFlag.setValue(true);
		synchronized(runFlag) { // tells readerThread to run if it's waiting on runFlag, so it can exit
			runFlag.notify();
		}
		s.shutdownOutput();
	}

	private static int loginAndRegister(BufferedReader stdin, PrintWriter toServer, BufferedReader fromServer)
		throws IOException
	{
		int r = CONTINUE, option = 0;

		do {
			try {
				printMenu(loginRegisterOptions);
				option = Integer.valueOf(stdin.readLine());
				if(option == 1 && login(stdin, toServer, fromServer))
					r = LOGIN; // indicates a successful login
				else if(option == 2)
					register(stdin, toServer, fromServer);
				else if(option == 3)
					r = EXIT;
				else
					System.err.println("Invalid option!");
			} catch(NumberFormatException e) {
				System.err.println("Invalid option!");
			}
		} while(r != LOGIN && r != EXIT);

		return r;
	}

	private static int mainMenu(BufferedReader stdin, PrintWriter toServer, RunFlag runFlag, AuctionClient readerThread)
		throws IOException
	{
		/* At this point client is logged in */
		printHelp();
		runFlag.setValue(true);
		synchronized(runFlag) {
			runFlag.notify();
		}

		String message;
		String[] args;
		int r = CONTINUE;
		do {
			System.out.println(PROMPT);
			message = stdin.readLine();
			if(message == null) // EOF
				r = EXIT;
			else {
				message = message.trim();
				args = message.split(" ");

				if(args[0].equalsIgnoreCase("logout")) {
					runFlag.setValue(false);
					r = LOGOUT;
				} else if(args[0].equalsIgnoreCase("exit")) {
					runFlag.setValue(false);
					r = EXIT;
				}
				sendMessageToServer(message, toServer);
			}
		} while(r != EXIT && r != LOGOUT);

		return r;
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

	private static void printMenu(String[] options) { // %n is the platform-specific line separator
		int i;

		for(i = 1; i <= options.length; ++i)
			System.out.printf("%d. %s%n", i, options[i-1]);

		System.out.println(PROMPT);
	}

	private static void printHelp() {
		final String separator = "-------------------------------------";

		System.out.printf("%s%n", separator);
		for(String str : helpLines)
			System.out.println(str);

		System.out.printf("%s%n", separator);
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

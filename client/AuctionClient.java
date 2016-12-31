import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.Socket;
import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.ReentrantLock;
import java.io.Console;

public class AuctionClient extends Thread{

	private static ReentrantLock validLoginLock = new ReentrantLock();	
	private static boolean validLogin;
	private static boolean invocationConcluded;
	private static Condition invocationConcludedCondition = validLoginLock.newCondition();
	private Socket s;
	private BufferedReader socketOut;
	
	public AuctionClient(Socket s, BufferedReader socketOut){
		this.s = s;
		this.socketOut = socketOut;
	}

	/* This thread reads from socket!! */
	public void run(){
		/* do work */
		String serverMessage;
		try{
			while((serverMessage = socketOut.readLine()) != null){
				if(serverMessage.equals("xl")){
					validLoginLock.lock();
					validLogin = false;
					validLoginLock.unlock();
					System.out.println("Invalid Login");
				}
				else {
					System.out.println(serverMessage);
				}
				invocationConcludedCondition.signal();
			}
			s.shutdownOutput();
		}catch(IOException e){
		}
	}
	
	public static void main(String[] args) throws IOException, InterruptedException{
		Socket s = new Socket("localhost", 8080);
		BufferedReader stdin = new BufferedReader(new InputStreamReader(System.in));
		BufferedReader socketOut = new BufferedReader(new InputStreamReader(s.getInputStream()));
		PrintWriter socketIn = new PrintWriter(s.getOutputStream(), true);
		AuctionClient readerThread = new AuctionClient(s, socketOut);
		
		/* Start Thread that reads from socket */
		readerThread.start();
		
		boolean exitFlag = false;
		do{
			int option;
			do{
				/*maybe move this to a method */
				/* validLoginLock.lock();
				validLogin = true;
				validLoginLock.unlock();*/
				
				System.out.print(firstMenu());
				try{
					option = Integer.parseInt(stdin.readLine());
					if(option == 2) login(stdin, socketIn);
					else if(option == 1) register(stdin, socketIn);
				}catch(NumberFormatException e){
					validLoginLock.lock();
					validLogin = false;
					validLoginLock.unlock();
					option = 1;
					System.out.println("Please choose one of the above options");
				}
				
				validLoginLock.lock();
				while(!invocationConcluded){
					invocationConcludedCondition.await();
				}
				validLoginLock.unlock();

				/*Thread.sleep(300);*/
			}while(option==1 || !validLogin);
			
			/* At this point client is logged in */	

			String message;
			String[] cmd;
			boolean logout = false;
			while(!logout && ((message = stdin.readLine()) != null)){
				cmd = message.trim().split(" ");
				
				if(! existsCmd(cmd[0])){
					System.out.println("Incorrect Syntax. Use --help for instructions.");
					continue;
				}

				logout = cmd[0].equals("logout");
				sendMessageToServer(message.trim(), socketIn);
			}
		}while( !exitFlag );
	}

	public static void sendMessageToServer(String cmd, PrintWriter socketOut){
		switch(cmd.split(" ")[0]){
			case "start": 
			case "list":
			case "bid":
			case "close":
			case "logout": socketOut.println(cmd); break;
			case "--help": showHelp(); break;
			default:
		}
	}

	public static void showHelp(){
		StringBuilder str = new StringBuilder();
		str.append("Start Auction: start <description>\n");
		str.append("List Auctions: list\n");
		str.append("Bid: bid <auctionId> <amount>\n");
		str.append("Close Auction: close <auctionId>\n");
		str.append("Logout: logout");
		System.out.println(str.toString());
	}
	
	public static void login(BufferedReader stdin, PrintWriter socketIn){
		try{
		String[] data = askForUsernamePassword(stdin);
		socketIn.println("login "+data[0]+" "+data[1]);
		}catch(IOException e){
		}
	}

	public static String firstMenu(){
		StringBuilder str = new StringBuilder();
		str.append("1 - Register\n");
		str.append("2 - Login\n");
		
		return str.toString();
	}

	public static String mainMenu(){
		StringBuilder str = new StringBuilder();
		str.append("Start Auction\n");
		str.append("List running auctions\n");
		str.append("Bid item\n");
		str.append("Close Auction\n");
		str.append("Logout and exit\n");
		
		return str.toString();
	}

	public static boolean existsCmd(String cmd){
		/* Commands need to be stored somewhere */
		return (cmd.equals("start") || cmd.equals("list") || cmd.equals("bid") 
		|| cmd.equals("close") || cmd.equals("logout") || cmd.equals("--help"));
	}
	
	public static String[] askForUsernamePassword(BufferedReader stdin) throws IOException{		
		String[] data = new String[2];
		System.out.print("Username: ");
		data[0] = stdin.readLine();
		System.out.print("Password: ");
		if(System.console() == null) {
		    data[1] = stdin.readLine();
		} else{
			data[1] = new String(System.console().readPassword("Password: "))
		}

		return data;
	}

	public static void register(BufferedReader stdin, PrintWriter socketIn){
		try{
		String[] data = askForUsernamePassword(stdin);
		socketIn.println("reg "+data[0]+" "+data[1]);
		}catch(IOException e){
		}
	}
}

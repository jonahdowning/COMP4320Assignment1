import java.io.*;   // for Input/OutputStream
import java.net.*;  // for Socket
import java.util.Scanner;  // for Scanner

public class myFirstTCPClient {

  public static void main(String args[]) throws Exception {

    // SETUP SERVER CONNECTION LOGIC
    
    if (args.length != 2)  // Test for correct # of args
      throw new IllegalArgumentException("Parameter(s): <Destination> <Port>");

    InetAddress destAddr = InetAddress.getByName(args[0]);  // Destination address
    int destPort = Integer.parseInt(args[1]);               // Destination port

    Socket sock = new Socket(destAddr, destPort);

    // TAKE INPUTS
    Scanner sc = new Scanner(System.in);
    System.out.println("Enter a item code: ");
    String itemCode = sc.nextLine();
    System.out.println("Enter a quantity: ");
    int quantity = sc.nextInt();
    
    
    System.out.println("Displaying order: " + itemCode + " " + quantity); // Display order just to check what we send



    System.out.println("Sending Friend (Binary)");
    OutputStream out = sock.getOutputStream(); // Get a handle onto Output Stream
    out.write(itemCode.getBytes()); // Send item code as bytes
    out.write(quantity); // Send quantity as a byte (note: this will only work for
    sock.close();

  }
}

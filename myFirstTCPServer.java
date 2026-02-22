import java.io.*;   // for Input/OutputStream
import java.net.*;  // for Socket and ServerSocket
import java.util.ArrayList;
import java.util.List;
public class myFirstTCPServer {

  public static void main(String args[]) throws Exception {

    if (args.length != 1)  // Test for correct # of args
      throw new IllegalArgumentException("Parameter(s): <Port>");

    int port = Integer.parseInt(args[0]);   // Receiving Port
	
    ServerSocket servSock = new ServerSocket(port);
    Socket clntSock = servSock.accept();


    // Use DataInputStream to read the shorts correctly
    DataInputStream din = new DataInputStream(clntSock.getInputStream());
    
    //define the array of received items
    List<Short> receivedItems = new ArrayList<>();

    // 1. Read the Request # and TML first
    short reqID = din.readShort(); //
    short tml = din.readShort();   //
    System.out.println("Processing Request #" + reqID + " (TML: " + tml + ")");

    // 2. Read the pairs until the -1 terminator is found
    while (true) {
        short quantity = din.readShort(); //
        if (quantity == -1) { //
            break; 
        }
        
        short code = din.readShort(); //
        receivedItems.add(code);
        receivedItems.add(quantity);
        
        System.out.println("Order received: Q=" + quantity + ", C=" + code);
    }

    

    // STEP 3: Calculate Costs (using a dummy price for now)
    
    short numItems = (short) (receivedItems.size() / 2); // Assuming you stored them in a list
    int totalAmount = 0;
    List<Integer> itemCosts = new ArrayList<>();

    // Calculate how many bytes we SHOULD have received
    // 4 (Header) + (numItems * 4) + 2 (for the -1 terminator)
    int actualBytesRead = 4 + (numItems * 4) + 2;

    short responseTML;
    if (tml != actualBytesRead) {
        System.out.println("Error: TML Mismatch! Expected " + tml + " but got " + actualBytesRead);
        responseTML = -1; // This sends 0xFFFF to the client
    } else {
        // Normal calculation: 10 base bytes + 4 per item cost
        responseTML = (short) (10 + (numItems * 4));
    }

    // Logic to calculate each item's cost
    for (int i = 0; i < receivedItems.size(); i += 2) {
        // Based on your loop, index i is Code and i+1 is Quantity
        short code = receivedItems.get(i);
        short quantity = receivedItems.get(i + 1);
        
        // Dummy math: 100 cents per unit
        int cost = quantity * 100; 
        itemCosts.add(cost);
        totalAmount += cost;
    }

    // STEP 4: Send Response
    OutputStream out = clntSock.getOutputStream();
    DataOutputStream dout = new DataOutputStream(out);

    // Calculate Dynamic TML: 6 (Header) + (4 * numItems) + 4 (Total)
    

    dout.writeShort(reqID);   
    dout.writeShort(responseTML); // Use the variable, not a hardcoded number
    dout.writeShort(numItems);

    // If there was a mismatch, you might want to skip sending costs or send 0s
    if (responseTML != -1) {
        for (int cost : itemCosts) {
            dout.writeInt(cost);
        }
        dout.writeInt(totalAmount);
    } else {
        // Optional: Send a 0 total if there was an error
        dout.writeInt(0); 
    }
    dout.flush();

    clntSock.close();
    servSock.close();
  }
}



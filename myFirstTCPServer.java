import java.io.*;   // for Input/OutputStream
import java.net.*;  // for Socket and ServerSocket
import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.List;
import java.util.HashMap;
import java.util.Map;

public class myFirstTCPServer{

  public static void main(String args[]) throws Exception {

    if (args.length != 1)  // Test for correct # of args
      throw new IllegalArgumentException("Parameter(s): <Port>");

    int port = Integer.parseInt(args[0]);   // Receiving Port
	

    //reading CSV file
    Map<Short, Object[]> itemDataMap = new HashMap<>();

    try (BufferedReader br = new BufferedReader(new FileReader("data.csv"))) {
        String line;
        while ((line = br.readLine()) != null) {
            String[] parts = line.split(",");
            String cleanID = parts[0].trim().replace("\uFEFF", "").replaceAll("[^0-9]", ""); 
            short code = Short.parseShort(cleanID);
            
            // SWAPPED HERE: Column 1 is Snickers, Column 2 is the Price
            String description = parts[1].trim(); 
            int price = Integer.parseInt(parts[2].trim());
            
            itemDataMap.put(code, new Object[]{price, description});
        }
    }

    //Open Socket
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

    // 1. Create a "draft" area to build the byte array
    ByteArrayOutputStream baos = new ByteArrayOutputStream();
    DataOutputStream dout = new DataOutputStream(baos);

    // Step 5a: Fill Request #
    dout.writeShort(reqID); 

    // Step 5f: Placeholder for TML (We will fill this properly later)
    dout.writeShort((short)0);

   // Initialize the running total before the loop starts
    int totalAmount = 0; 

    // Logic to calculate each item's cost
    for (int i = 0; i < receivedItems.size(); i += 2) {
        // Your logic stored them as [code, quantity]
        short code = receivedItems.get(i);
        short originalQuantity = receivedItems.get(i + 1);

        // 1. Get the real data from the CSV map
        Object[] data = itemDataMap.get(code);
        String description = (data != null) ? (String)data[1] : "Unknown Item";
        int unitPrice = (data != null) ? (int)data[0] * 100: 0; // Price from CSV

        // 2. Calculate the actual cost for this item
        int cost = originalQuantity * unitPrice;

        // 3. Add to the grand total
        totalAmount += cost; 

        // 5c: Length L of the string
        dout.writeByte((byte) description.length()); 
        // 5c: The String D itself
        dout.writeBytes(description); 
        // 5b: The Total Cost TC
        dout.writeInt(cost); 
        // 5d: The Quantity Qi
        dout.writeShort(originalQuantity);
    }

    // Write the Final Total Amount before the trailer
    dout.writeInt(totalAmount); 

    // Step 5e: Fill trailer with -1
    dout.writeShort((short)-1);
    dout.flush();

    // Step 5f: Finalize TML and overwrite the placeholder
    byte[] finalResponseArray = baos.toByteArray();
    short finalTML = (short) finalResponseArray.length;

    // Inject the real TML into the header
    ByteBuffer bb = ByteBuffer.wrap(finalResponseArray);
    bb.putShort(2, finalTML); 

    // Step 5g: Display byte per byte in hexadecimal
    System.out.print("Server Response (Hex): ");
    for (byte b : finalResponseArray) {
        System.out.printf("0x%02X ", b);
    }
    System.out.println();

    // Final Step: Send the completed array to the client
    OutputStream out = clntSock.getOutputStream();
    out.write(finalResponseArray);
    out.flush();
    

    clntSock.close();
    servSock.close();
  }
}



import java.io.*;   // for Input/OutputStream
import java.net.*;  // for Socket
import java.util.ArrayList;
import java.util.List;
import java.util.Scanner;  // for Scanner
import java.nio.ByteBuffer; //ByteBuffer for primitive types

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
    List<Short> items = new ArrayList<>(); 
    short requestID = 101; 

    while (true) {
        System.out.print("Enter a quantity number between 0-32767 (or -1 to finish): ");
        short q = sc.nextShort(); 
        
        if (q == -1) { 
            break;
        }
        
        System.out.print("Enter a description code between 0-32767: ");
        short c = sc.nextShort(); 
        
        items.add(q);
        items.add(c);
    }

    items.add((short)-1);
    
    // 2 bytes for RequestID + 2 bytes for TML + (number of elements * 2 bytes)
    //tml = Total Message Length
    short tml = (short) (4 + (items.size() * 2));
    
    ByteBuffer buffer = ByteBuffer.allocate(tml);

    // Field 1: Request # [cite: 33]
    buffer.putShort(requestID);

    // Field 2: TML 
    buffer.putShort(tml);

    // Field 3: All (Q, C) pairs and the -1 terminator 
    for (short value : items) {
        buffer.putShort(value);
    }

    // Convert the buffer to a standard byte array
    byte[] arrayA = buffer.array();

    //print in hexidecimal format
    System.out.print("Array A (Hex): ");
    for (byte b : arrayA) {
        System.out.printf("0x%02X ", b);
    }
    System.out.println();

    // Send to server
    OutputStream out = sock.getOutputStream();
    out.write(arrayA);
    out.flush();


    // STEP 6: Wait for a response from the server
    InputStream in = sock.getInputStream();
    DataInputStream din = new DataInputStream(in); // Helps read shorts and ints easily

    // Read the Header first (4 bytes total)
    //res = response
    short resRequestID = din.readShort(); // 2 bytes
    short resTML = din.readShort();       // 2 bytes

    // 2. CHECK FOR TML MISMATCH ERROR (-1)
    if (resTML == -1 || resTML == (short)0xFFFF) {
        System.out.println("Error: Server reported a TML Mismatch in the request.");
        sock.close();
        return; 
    }

    // 3. Continue reading normally if TML is valid
    short resNumItems = din.readShort();
    // Print the Header
    System.out.println("------------------------------------------------------------------");
    System.out.printf("%-10s %-20s %-12s %-10s %-15s\n", "Item #", "Description", "Unit Cost", "Quantity", "Cost Per Item");
    System.out.println("------------------------------------------------------------------");

    // Loop through the received item costs
    for (int i = 0; i < resNumItems; i++) {
        int itemCost = din.readInt();
        
        // We need the original quantity for the table. 
        // It's stored in your 'items' list from earlier:
        short originalQuantity = items.get(i * 2); 
        short originalCode = items.get(i * 2 + 1);
        
        // Calculate unit cost for the table
        int unitCost = (originalQuantity > 0) ? (itemCost / originalQuantity) : 0;

        // Print the row (converting cents to dollars)
        System.out.printf("%-10d %-20s $%-11.2f %-10d $%-14.2f\n", 
            (i + 1), 
            "Code " + originalCode, // Description
            unitCost / 100.0, 
            (int)originalQuantity, 
            itemCost / 100.0);
    }

    // Print the Final Total
    System.out.println("------------------------------------------------------------------");
    int totalAmount = din.readInt();
    System.out.printf("%45s %-10s $%.2f\n", "", "Total", totalAmount / 100.0);

    sock.close();
  }
}

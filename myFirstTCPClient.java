import java.io.*;   // for Input/OutputStream
import java.net.*;  // for Socket
import java.util.ArrayList;
import java.util.List;
import java.util.Scanner;  // for Scanner
import java.nio.ByteBuffer; //ByteBuffer for primitive types

public class myFirstTCPClient {

  public static void main(String args[]) throws Exception {

    // SETUP SERVER CONNECTION LOGIC
    if (args.length != 2) throw new IllegalArgumentException("Parameter(s): <Destination> <Port>");
    InetAddress destAddr = InetAddress.getByName(args[0]);  // Destination address
    int destPort = Integer.parseInt(args[1]);               // Destination port
    Socket sock = new Socket(destAddr, destPort);

    // TAKE INPUTS
    short requestID = 101; 
    List<Short> items = collectItems();
    
    //PRINT HEX ARRAY
    byte[] arrayA = getArrayA(items, requestID);
    System.out.print("Array A (Hex): ");
    for (byte b : arrayA) {
        System.out.printf("0x%02X ", b);
    }
    System.out.println();

    // SEND REQUEST TO SERVER
    OutputStream out = sock.getOutputStream();
    out.write(arrayA);
    out.flush();


    // READ RESPONSE FROM SERVER
    DataInputStream din = new DataInputStream(sock.getInputStream()); // Helps read shorts and ints easily

    // Read the Header first (4 bytes total)
    short respRequestID = din.readShort(); // 2 bytes; todo: does this need to be checked against the original requestID? what if it doesn't match?
    short respTML = din.readShort();       // 2 bytes

    //Data Validation: Check if the TML is valid
    if (respTML == -1 || respTML == (short)0xFFFF) {
        System.out.println("Error: Server reported a TML Mismatch in the request.");
        sock.close();
        return; 
    }
    // READ AND PRINT THE RESPONSE
    int expectedItemsSent = items.size() / 2;
    readAndPrintResponse(expectedItemsSent, din);

    sock.close();
  }

  private static void readAndPrintResponse(int expectedItemsSent, DataInputStream din) throws IOException {
    System.out.println("\n------------------------------------------------------------------");
    System.out.printf("%-10s %-20s %-12s %-10s %-15s\n", "Item #", "Description", "Unit Cost", "Quantity", "Cost Per Item");
    System.out.println("------------------------------------------------------------------");
    
    // Calculate exactly how many items we are expecting back
    int respItemCount = 0;

    for (int i = 0; i < expectedItemsSent; i++) {
        //Read length L
        byte len = din.readByte();

        // Read Description D
        byte[] stringBytes = new byte[len];
        din.readFully(stringBytes);
        String itemName = new String(stringBytes);

        // Read Total Cost TC
        int itemCost = din.readInt();

        // Read Quantity Qi
        short quantityReceived = din.readShort();

        respItemCount++;
        double unitCost = (quantityReceived > 0) ? (itemCost / (double)quantityReceived) : 0;

        System.out.printf("%-10d %-20s $%-11.2f %-10d $%-14.2f\n", respItemCount, itemName, unitCost / 100.0, (int)quantityReceived, itemCost / 100.0);
    }

    // Print the Final Total
    System.out.println("------------------------------------------------------------------");
    int totalAmount = din.readInt();
    short trailer = din.readShort();  // The -1 trailer (0xFFFF)
    System.out.printf("%45s %-10s $%.2f\n", "", "Total", totalAmount / 100.0);
  }

  private static byte[] getArrayA(List<Short> items, short requestID) {
    // (2 bytes for RequestID) + (2 bytes for TML) + (number of elements * 2 bytes)
    //tml = Total Message Length
    short tml = (short) (4 + (items.size() * 2));
    
    ByteBuffer buffer = ByteBuffer.allocate(tml);

    // Field 1: Request #
    buffer.putShort(requestID);

    // Field 2: TML 
    buffer.putShort(tml);

    // Field 3: All (Q, C) pairs and the -1 terminator 
    for (short value : items) {
        buffer.putShort(value);
    }

    // Convert the buffer to a standard byte array
    byte[] arrayA = buffer.array();
    return arrayA;
  }

  private static List<Short> collectItems() {
    Scanner sc = new Scanner(System.in);
    List<Short> items = new ArrayList<>();
    while (true) {
        System.out.print("Enter a quantity number between 0-32767 (or -1 to finish): ");
        short quantity = sc.nextShort(); 
        
        if (quantity == -1) { 
            break;
        }
        
        System.out.print("Enter a description code between 0-32767: ");
        short code = sc.nextShort(); 
        
        items.add(quantity);
        items.add(code);
    }
    sc.close();

    // Add the -1 terminator for quantity
    items.add((short)-1);
    return items;
  }
  
}

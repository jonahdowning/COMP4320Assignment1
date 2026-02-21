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

    // Wait for a response (Requirement Step 6)
    // For now, we just close to see if the client runs without errors
    sock.close();


    //System.out.println("Displaying order: " + q + " " + c); // Display order just to check what we send
    // System.out.println("Sending Friend (Binary)");
    // OutputStream out = sock.getOutputStream(); // Get a handle onto Output Stream
    // out.write(itemCode.getBytes()); // Send item code as bytes
    // out.write(quantity); // Send quantity as a byte (note: this will only work for
    // sock.close();

  }
}

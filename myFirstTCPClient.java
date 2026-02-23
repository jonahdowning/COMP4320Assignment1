import java.io.*;
import java.net.*;
import java.util.ArrayList;
import java.util.List;
import java.util.Scanner;
import java.nio.ByteBuffer;

public class myFirstTCPClient {

  public static void main(String args[]) throws Exception {

    // SETUP SERVER CONNECTION LOGIC
    if (args.length != 2)
      throw new IllegalArgumentException("Parameter(s): <Destination> <Port>");
    InetAddress destAddr = InetAddress.getByName(args[0]); // Destination address
    int destPort = Integer.parseInt(args[1]); // Destination port
    Socket sock = new Socket(destAddr, destPort);

    // TAKE INPUTS
    short requestID = 100;
    List<Short> items = collectItems();

    // PRINT HEX ARRAY
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
    InputStream in = sock.getInputStream();
    byte[] header = new byte[4];
    int bytesRead = 0;
    while (bytesRead < 4) {
      int res = in.read(header, bytesRead, 4 - bytesRead);
      if (res == -1)
        break;
      bytesRead += res;
    }

    int tml = Short.toUnsignedInt(ByteBuffer.wrap(header).getShort(2));
    byte[] responseArray = new byte[tml];
    System.arraycopy(header, 0, responseArray, 0, 4);

    int current = 4;
    while (current < tml) {
      int res = in.read(responseArray, current, tml - current);
      if (res == -1)
        break;
      current += res;
    }

    System.out.print("Server Response (Hex): ");
    for (byte b : responseArray) {
      System.out.printf("0x%02X ", b);
    }
    System.out.println();

    DataInputStream din = new DataInputStream(new ByteArrayInputStream(responseArray)); // Helps read shorts and intseasily

    // Read the Header first (4 bytes total)
    short respRequestID = din.readShort(); // 2 bytes; todo: does this need to be checked against the original requestID? what if it doesn't match?
    short respTML = din.readShort(); // 2 bytes

    // Data Validation: Check if the TML is valid
    if (respTML == -1 || respTML == (short) 0xFFFF) {
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
    // Read Total Cost TC (4 bytes) - Located in Header per PDF
    int serverTotalCost = din.readInt();

    System.out.println("\n------------------------------------------------------------------");
    System.out.printf("%-10s %-20s %-12s %-10s %-15s\n", "Item #", "Description", "Unit Cost", "Quantity",
        "Cost Per Item");
    System.out.println("------------------------------------------------------------------");

    // Calculate exactly how many items we are expecting back
    int respItemCount = 0;
    int calculatedTotalCost = 0;

    for (int i = 0; i < expectedItemsSent; i++) {
      // Read length L
      byte len = din.readByte();

      // Read Description D
      byte[] stringBytes = new byte[len];
      din.readFully(stringBytes);
      String itemName = new String(stringBytes);

      // Read Unit Cost CS (2 bytes)
      short unitCostShort = din.readShort();

      // Read Quantity Qi (2 bytes)
      short quantityReceived = din.readShort();

      // Calculate Item Cost
      int itemCost = unitCostShort * quantityReceived;
      calculatedTotalCost += itemCost;

      respItemCount++;
      double unitCostDisplay = unitCostShort / 100.0;
      double itemCostDisplay = itemCost / 100.0;

      System.out.printf("%-10d %-20s $%-11.2f %-10d $%-14.2f\n", respItemCount, itemName, unitCostDisplay,
          (int) quantityReceived, itemCostDisplay);
    }

    // Print the Final Total
    System.out.println("------------------------------------------------------------------");

    short trailer = din.readShort(); // The -1 trailer (0xFFFF)
    System.out.printf("%45s %-10s $%.2f\n", "", "Total", serverTotalCost / 100.0);

    // Step 6 Validation: Check if TC equals sum of (CS * Q)
    if (serverTotalCost != calculatedTotalCost) {
      System.out.println("\nError: the total cost in the response does not match the total computed by the client.");
    }
  }

  private static byte[] getArrayA(List<Short> items, short requestID) {
    // (2 bytes for RequestID) + (2 bytes for TML) + (number of elements * 2 bytes)
    // tml = Total Message Length
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
    items.add((short) -1);
    return items;
  }

}

import java.io.*;
import java.net.*;
import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.List;
import java.util.HashMap;
import java.util.Map;

public class myFirstTCPServer {

    public static void main(String args[]) throws Exception {
        if (args.length != 1) {
            throw new IllegalArgumentException("Parameter(s): <Port>");
        } else {System.out.println("Server is running. Waiting for client request...");}

        int port = Integer.parseInt(args[0]);
        // load item data from CSV into a Map for quick lookup
        Map<Short, Object[]> itemDataMap = getItemDataMap();

        try (ServerSocket servSock = new ServerSocket(port);
                Socket clntSock = servSock.accept();
                DataInputStream din = new DataInputStream(clntSock.getInputStream());
                OutputStream out = clntSock.getOutputStream()) {

            while (true) {
                // READ REQUEST FROM CLIENT
                ClientRequest request;
                try {
                    request = parseRequest(din, clntSock);
                } catch (EOFException e) {
                    System.out.println("\nClient disconnected.");
                    break;
                }

                // TML Validation
                if (!validateTML(request)) {
                    sendErrorResponse(out, request.requestID);
                    break; // Stop processing on error
                }

                // BUILD RESPONSE
                byte[] finalResponseArray = buildResponse(request, itemDataMap);

                // PRINT AND SEND
                printHexDump("\nServer Response (Hex): ", finalResponseArray);
                out.write(finalResponseArray);
                out.flush();
            }
        }
    }

    // --- Extracted Methods ---

    /**
     * Parses the client's request from the DataInputStream and constructs a
     * ClientRequest object.
     * 
     * @param din DataInputStream to read the client's request
     * @return ClientRequest object containing the request ID, TML, and list of
     *         ordered items
     * @throws IOException
     */
    private static ClientRequest parseRequest(DataInputStream din, Socket clntSock) throws IOException {
        byte[] header = new byte[4];
        din.readFully(header);

        ByteBuffer bb = ByteBuffer.wrap(header);
        short requestID = bb.getShort();
        short tml = bb.getShort();

        System.out.println("\n--- Starting Request #" + requestID + " (TML: " + tml + ")---");
        System.out.println("Client IP: " + clntSock.getInetAddress().getHostAddress() + ", Port: " + clntSock.getPort());

        int bodyLen = Short.toUnsignedInt(tml) - 4;
        byte[] body = new byte[bodyLen];
        din.readFully(body);

        byte[] fullRequest = new byte[Short.toUnsignedInt(tml)];
        System.arraycopy(header, 0, fullRequest, 0, 4);
        System.arraycopy(body, 0, fullRequest, 4, bodyLen);
        printHexDump("Array A (Hex): ", fullRequest);

        DataInputStream bodyDin = new DataInputStream(new ByteArrayInputStream(body));
        List<OrderItem> items = new ArrayList<>();
        System.out.println("\nItems in Request:");
        while (true) {
            short quantity = bodyDin.readShort();
            if (quantity == -1) {
                break;
            }
            short code = bodyDin.readShort();
            items.add(new OrderItem(code, quantity));

            System.out.println("Order received: Q=" + quantity + ", C=" + code);
        }

        return new ClientRequest(requestID, tml, items);
    }

    private static boolean validateTML(ClientRequest request) {
        int expectedItemCt = request.items.size();
        // 4 (Header) + (numItems * 4) + 2 (for the -1 terminator)
        int expectedTML = 4 + (expectedItemCt * 4) + 2;

        if (Short.toUnsignedInt(request.tml) != expectedTML) {
            System.out.println(
                    "Error: TML Mismatch! Expected " + Short.toUnsignedInt(request.tml) + " but got " + expectedTML);
            return false;
        }
        return true;
    }

    private static void sendErrorResponse(OutputStream out, short requestID) throws IOException {
        ByteBuffer bb = ByteBuffer.allocate(4);
        bb.putShort(requestID);
        bb.putShort((short) -1);
        out.write(bb.array());
        out.flush();
        System.out.println("Sent Error Response: RequestID " + requestID + ", -1");
    }

    private static byte[] buildResponse(ClientRequest request, Map<Short, Object[]> itemDataMap) throws IOException {
        // Use a temporary buffer for items to calculate Total Cost first
        ByteArrayOutputStream itemsBaos = new ByteArrayOutputStream();
        DataOutputStream itemsDos = new DataOutputStream(itemsBaos);

        int totalCost = 0;

        // Step 1: Process Items
        for (OrderItem item : request.items) {
            Object[] data = itemDataMap.get(item.code);
            String description = (data != null) ? (String) data[1] : "Article Not Available";
            short unitPrice = (short) ((data != null) ? (int) data[0] : 0);

            int cost = item.quantity * unitPrice;
            totalCost += cost;

            itemsDos.writeByte((byte) description.length());
            itemsDos.writeBytes(description);
            itemsDos.writeShort(unitPrice);
            itemsDos.writeShort(item.quantity);
        }

        // Step 2: Build Final Message
        ByteArrayOutputStream finalBaos = new ByteArrayOutputStream();
        DataOutputStream dout = new DataOutputStream(finalBaos);

        dout.writeShort(request.requestID);
        dout.writeShort((short) 0); // Placeholder for TML
        dout.writeInt(totalCost); // TC (Total Cost) goes in Header

        dout.write(itemsBaos.toByteArray()); // Write the items

        dout.writeShort((short) -1);
        dout.flush();

        // Step 3: Finalize TML
        byte[] responseBytes = finalBaos.toByteArray();
        short finalTML = (short) responseBytes.length;

        ByteBuffer.wrap(responseBytes).putShort(2, finalTML);

        return responseBytes;
    }

    private static void printHexDump(String label, byte[] data) {
        System.out.print(label);
        for (byte b : data) {
            System.out.printf("0x%02X ", b);
        }
        System.out.println();
    }

    /**
     * Reads the data.csv file and constructs a Map of Item Code to an Object array
     * containing Price and Description.
     * 
     * @return Map of Item Code to [Price, Description]
     * @throws IOException
     */
    private static Map<Short, Object[]> getItemDataMap() throws IOException {
        Map<Short, Object[]> itemDataMap = new HashMap<>();

        try (BufferedReader br = new BufferedReader(new FileReader("data.csv"))) {
            String line;
            while ((line = br.readLine()) != null) {
                String[] parts = line.split(",");
                String cleanID = parts[0].trim().replace("\uFEFF", "").replaceAll("[^0-9]", "");
                short code = Short.parseShort(cleanID);

                // column 1 is description, column 2 is the Price
                String description = parts[1].trim();
                int price = Integer.parseInt(parts[2].trim());

                itemDataMap.put(code, new Object[] { price, description });
            }
        }
        return itemDataMap;
    }

    // --- Helper Classes ---

    private static class ClientRequest {
        final short requestID;
        final short tml;
        final List<OrderItem> items;

        ClientRequest(short requestID, short tml, List<OrderItem> items) {
            this.requestID = requestID;
            this.tml = tml;
            this.items = items;
        }
    }

    private static class OrderItem {
        final short code;
        final short quantity;

        OrderItem(short code, short quantity) {
            this.code = code;
            this.quantity = quantity;
        }
    }
}
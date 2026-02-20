import java.io.*;   // for Input/OutputStream
import java.net.*;  // for Socket and ServerSocket
public class myFirstTCPServer {

  public static void main(String args[]) throws Exception {

    if (args.length != 1)  // Test for correct # of args
      throw new IllegalArgumentException("Parameter(s): <Port>");

    int port = Integer.parseInt(args[0]);   // Receiving Port
	
    ServerSocket servSock = new ServerSocket(port);
    Socket clntSock = servSock.accept();


    System.out.println("Received Binary-Encoded Friend");
    

    clntSock.close();
    servSock.close();
  }
}



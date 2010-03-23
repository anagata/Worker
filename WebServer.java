/*****************************************************************
 * Multi Threaded WebServer                                      *
 * Implements GET, HEAD, and POST requests                       *
 * Handles the following errors: 404, 501, and 505               *
 * http://flame.cs.dal.ca/~nagata/CS3171.htm                     *
 * PORT# 4016                                                    *
 * Why java? because it has extensive libraries that we can use  *
 *           and it handles string quite nicely                  *
 *****************************************************************/
import java.io.*;
import java.net.*;
import java.util.*;

public final class WebServer {
    public static void main(String args[]) throws Exception {
        // Set up the listen socket
        int PORT = 4016;
        ServerSocket listenSocket = new ServerSocket(PORT);
        // Process HTTP service requests in an infinite loop
        while(true) {
          // Construct an object to process the HTTP request message
          HttpRequest request = new HttpRequest(listenSocket.accept());
          // Create a new thread to process the request
          Thread thread = new Thread(request);
          // Start the thread
          thread.start();
        }
    }
}

final class HttpRequest implements Runnable {
    // Set up Carriage Return and Line Feed variable
    final static String CRLF = "\r\n";
    Socket socket;

    public HttpRequest(Socket socket) throws Exception {
        this.socket = socket;
    }
    // Implement the run() method of the Runnable interface
    public void run() {
        try {
          processRequest();
        } catch (Exception e) {
          System.out.println(e);
        }
    }
    // Get the file type for Http response
    private String fileType(String fileName) {
        // Check requested file type
        if(fileName.endsWith(".htm") || fileName.endsWith(".html"))
          return "text/html";
        else if(fileName.endsWith(".jpg") || fileName.endsWith(".jpeg"))
          return "image/jpeg";
        else if(fileName.endsWith(".gif"))
          return "image/gif";
        else if(fileName.endsWith(".txt"))
          return "text/plain";
        else
          return "application/octet-stream";
    }
    private static void sendFiles(FileInputStream fiSt, OutputStream oStr) throws Exception {
        // Construct a 1K buffer to hold the requested data
        byte[] buffer = new byte[1024];
        int bytes = 0;
        // Copy requested file into the socket's output stream
        while((bytes = fiSt.read(buffer)) != -1 )
          oStr.write(buffer, 0, bytes);
    }
    private void processRequest() throws Exception {
        // Get references to sockets input & output streams
        InputStream iStr = this.socket.getInputStream();
        DataOutputStream oStr = new DataOutputStream(this.socket.getOutputStream());
        // Set up input stream filter
        BufferedReader br = new BufferedReader(new InputStreamReader(iStr));
        // Get the request line of HTTP message
        String requestLine = br.readLine();

        // Extract request, filename, and httpVersion from the request line
        StringTokenizer tokens = new StringTokenizer(requestLine);
        String request = tokens.nextToken();
        String fileName = tokens.nextToken();
        String httpVersion = tokens.nextToken();

        // Remove slash at the beginning of token
        if(fileName.startsWith("/"))
          fileName = fileName.substring(1,fileName.length());
        // Open the requested file
        FileInputStream fiSt = null;
        boolean fileExists = true;
        try {
          fiSt = new FileInputStream(fileName);
        } catch (FileNotFoundException e) {
          fileExists = false;
        }
        // Construct the response message
        String statusLine = null;
        String contentTypeLine = null;
        String entityBody = null;
        // Check request for error
        if (!httpVersion.equals("HTTP/1.1")) {
          statusLine = "HTTP/1.1 505 HTTP Version Not Supported" + CRLF;
        } else if (!request.equals("GET") && !request.equals("HEAD")
                                          && !request.equals("POST")) {
          statusLine = "HTTP/1.1 501 Not Implemented" + CRLF;
        } else {
          if (fileExists) {
            statusLine = "HTTP/1.1 200 OK" + CRLF;
            contentTypeLine = "Content-type: " + fileType(fileName) + CRLF;
          } else {
            statusLine = "HTTP/1.1 404 Not Found" + CRLF;
            if (request.equals("GET") || request.equals("HEAD")){
              contentTypeLine = "NONE";
              entityBody = "\n\n Not Found";
            } else if (request.equals("POST")){
              entityBody = "\n\n HTTP POST failed";
            }
          }
        }
        // Send the status line
        oStr.writeBytes(statusLine);
        // Send the content type line
        oStr.writeBytes(contentTypeLine);
        // Send a blank line to indicate the end of the header lines
        oStr.writeBytes(CRLF);
        // Send the entity body
        if (fileExists && request.equals("GET")) {
          sendFiles(fiSt, oStr);
          fiSt.close();
        } else {
          oStr.writeBytes(entityBody);
        }
        // Close the streams
        oStr.close();
        br.close();
        socket.close();
    }
}

package route;

import java.io.BufferedWriter;
import java.io.OutputStreamWriter;
import java.net.Socket;
import java.util.Map;

import javax.json.JsonObject;

public class postRoute {

    Socket requestSocket;
    
    public postRoute(Socket reqSock){
        this.requestSocket = reqSock;
    }

    public int route(String path, Map<String, String> params, Map<String, String> headers, String buffer, JsonObject jsonObj)
    {
        switch (path)
        {
        
            default : 
                return notFound();
        }
    }

    private int notFound()
    {
        try
        {
            BufferedWriter respBuffer = new BufferedWriter(new OutputStreamWriter(requestSocket.getOutputStream()));
            
            respBuffer.write("HTTP/1.1 404 NotFound\r\nContent-Type: text/html\r\n\r\n<h1>404 Not Found</h1>");
            respBuffer.flush();


            respBuffer.close();
        }
        catch(Exception e)
        {
            System.out.println(e);
        }
        
        return 0;
    }
    
}

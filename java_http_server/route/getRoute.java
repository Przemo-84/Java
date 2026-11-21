package route;
import java.io.*;
import java.net.*;
import java.util.*;
import javax.json.*;
import httpServer.httpServer;

public class getRoute {

    Socket requestSocket;
    httpServer  httpServ;
    
    public getRoute(Socket reqSock, httpServer httpServ){
        this.requestSocket = reqSock;
        this.httpServ = httpServ;
    }

    public int route(String path, Map<String, String> params, Map<String, String> headers, String buffer, JsonObject jsonObj)
    {
        switch (path)
        {
            case "/" :
               return getMain(headers, buffer);

            default : 
                return notFound();
        }
    }

    private int getMain(Map<String, String> headers, String buffer)
    {
        try
        {
            BufferedWriter respBuffer = new BufferedWriter(new OutputStreamWriter(this.requestSocket.getOutputStream()));

            String content = "<HTML>\r\n<HEAD>\r\n<TITLE>\r\nJava Http Server\r\n</TITLE><meta name=\"viewport\" content=\"width=device-width, height=device-height, initial-scale=1.0\">\r\n</HEAD>\r\n<BODY>\r\n<br/><span style=\"position : relative; display: block; left: 20%; width: 70%; text-align:left; font-family: \'Tahoma\'; font-weight: bold; font-size:24px; letter-spacing:1px; word-spacing: 2px\">\r\nWelcome to Java Http Server!\r\n</span><br/><br/><span style=\" position: relative; display: block; left: 20%; width: 70%; text-align:left; font-family: 'Tahoma'; font-size: 18px; letter-spacing:1px; word-spacing: 3px\">If you see this page, the Java web server is successfully installed and working. Further configuration is required.<br/><br/>\r\nIf you want to know more about me, you can visit my web site at <a href =\""+httpServ.dataConfig.get("homePage_url")+"\">"+httpServ.dataConfig.get("homePage_url")+"</a></br></br></span></BODY>\r\n</HTML>\r\n";
            
            respBuffer.write("HTTP/1.1 200 OK\r\nContent-Type: text\\html\r\nContent-Length: " + content.length()+ "\r\n\r\n" + content);
            respBuffer.flush();


            respBuffer.close();
        }
        catch(Exception e)
        {
            System.out.println(e);
        }

        return 0;
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

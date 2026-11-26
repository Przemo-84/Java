package httpServer;
import java.io.*;
import java.net.*;
import java.nio.charset.StandardCharsets;
import route.*;





public class requestExecute extends Thread{

    Socket clientSock;
    httpServer httpServ;

    private final int MAX_BUFFER_SIZE = 15000;
    private final int MAX_BUFFER_READ_COUNT = 5000;

    public requestExecute(Socket cs, httpServer httpServ)
    {
        this.clientSock = cs;
        this.httpServ = httpServ;
    }

    public void run()
    {
        try
        {
          
            InputStreamReader ir = new InputStreamReader(clientSock.getInputStream(), StandardCharsets.UTF_8);
  
            char [] buff = new char[MAX_BUFFER_SIZE];
            char [] buffRead = new char[MAX_BUFFER_READ_COUNT];

            int buffLen = 0;
            int readCount = 0;
            int retCode = -1;

            requestParser reqParser = new requestParser();

            clientSock.setSoTimeout(200);

            try
            {
             
                while((readCount = ir.read(buffRead, 0, MAX_BUFFER_READ_COUNT)) > 0) 
                {
                    if(readCount + buffLen > MAX_BUFFER_SIZE)
                    {
                        buffLen = 0;
                        retCode = -1;
                        break;
                    }
                    
                    System.arraycopy(buffRead, 0, buff, buffLen, readCount);
                    buffLen += readCount;

                    retCode = reqParser.parse(buff, buffLen);

                    if (retCode >= 0)
                        break;
                }
            }
            catch(Exception e)
            {
                System.out.println(e);
            }


            if(retCode == 0)
            {
                switch(reqParser.method)
                {
                    case "GET" : 
                    {
                        getRoute getR = new getRoute(clientSock, httpServ);
                        getR.route(reqParser);
                        break;
                    }
                    case "POST":
                    {
                        postRoute postR = new postRoute(clientSock, httpServ);
                        postR.route(reqParser);
                    
                        break;
                    }
                    default :
                    {
                        try
                        {
                            BufferedWriter respBuffer = new BufferedWriter(new OutputStreamWriter(clientSock.getOutputStream()));
                            
                            respBuffer.write("HTTP/1.1 404 NotFound\r\nContent-Type: text/html\r\n\r\n<h1>404 Not Found</h1>");
                            respBuffer.flush();

                            respBuffer.close();
                        }
                        catch(Exception e)
                        {
                            System.out.println(e);
                        }

                        break;
                    }
                }
        
            }
            else
            {
                try
                {
                    BufferedWriter respBuffer = new BufferedWriter(new OutputStreamWriter(clientSock.getOutputStream()));
                
                    respBuffer.write("HTTP/1.1 400 Bad Request\r\nContent-Type: text/html\r\n\r\n<h1>404 Bad Request</h1>");
                    respBuffer.flush();

                    respBuffer.close();
                }
                catch(Exception e)
                {
                    System.out.println(e);
                }
            }

            clientSock.close();
            ir.close();
        }
        catch(Exception e)
        {
            System.out.println(e);
        }

        
        this.httpServ.lock.lock();
        
        this.httpServ.threadCount --;

        this.httpServ.lock.unlock();
        
    }
}
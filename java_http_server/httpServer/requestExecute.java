package httpServer;
import java.lang.Thread;
import java.util.*;
import java.net.*;
import java.io.*;

import route.*;


public class requestExecute extends Thread{

    Socket clientSock;
    httpServer httpServ;

    public requestExecute(Socket cs, httpServer httpServ)
    {
        this.clientSock = cs;
        this.httpServ = httpServ;
    }

    public void run()
    {
        try
        {
            // System.out.println(clientSock.getInetAddress());

            Scanner sc = new Scanner(clientSock.getInputStream());

            clientSock.setSoTimeout(50);
        
            String s="";
            ArrayList <String>requestBuffer = new ArrayList<String>();

            try
            {

                while (sc.hasNextLine()) {
                    s= sc.nextLine();
                    requestBuffer.add(s);
                }


            }
            catch(Exception e)
            {
                //System.out.println(e);
            }

            if(requestBuffer.size() > 0)
            {

                requestParser reqParser = new requestParser();

                if(reqParser.parse(requestBuffer) == 0)
                {
                    switch(reqParser.method)
                    {
                        case "GET" : 
                        {
                            getRoute getR = new getRoute(clientSock, httpServ);
                            getR.route(reqParser.path, reqParser.urlParams, reqParser.headers, reqParser.buffer, reqParser.jsonObj);
                            break;
                        }
                        case "POST":
                        {
                            postRoute postR = new postRoute(clientSock);
                            postR.route(reqParser.path, reqParser.urlParams, reqParser.headers,
                                        reqParser.buffer, reqParser.jsonObj);
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
                    
                        respBuffer.write("HTTP/1.1 400 Bad Request\r\nContent-Type: text/html\r\n\r\n<h1>400 Bad Request</h1>");
                        respBuffer.flush();

                        respBuffer.close();
                    }
                    catch(Exception e)
                    {
                        System.out.println(e);
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
            sc.close();
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
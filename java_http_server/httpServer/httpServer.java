package httpServer;
import java.net.InetSocketAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.*;
import java.util.concurrent.locks.ReentrantLock;
import sqlModule.sqlModule;

public class httpServer implements Runnable {

    long thredId;

    public boolean is_connected = true;

    public ServerSocket servSock ;

    public InetSocketAddress sockAddr ;

    public Map<String,String> dataConfig = new HashMap<>();

    public sqlModule sql_module;

    static Boolean is_exit = false;

    public ReentrantLock lock = null; 
   
    public long threadCount;

    public void close()
    {
        System.out.println("Http Server is destroyed...");
    };

    public httpServer(long thredId, Map<String,String> dataConf) 
    {

            this.thredId = thredId;

            for(Map.Entry<String,String> entry : dataConf.entrySet())
            {
                this.dataConfig.put(entry.getKey(), entry.getValue());
            }

            // sql_module = new sqlModule(dataConf);

            this.threadCount = 0;

            lock = new ReentrantLock();

            try
            {
                int port = Integer.parseInt(this.dataConfig.get("listenPort"));
                this.servSock = new ServerSocket();
                sockAddr = new InetSocketAddress("127.0.0.1", port);
                servSock.bind(sockAddr);  
                System.out.println("Http Server crated and listening on port : " + port);

            }
            catch(Exception e)
            {
                System.out.println(e);
                this.is_connected = false;
            }
    }
     public void run()
     {
        do
        {
            try{

                Socket ss = servSock.accept();

                requestExecute requestExec = new requestExecute(ss, this);
                
                Thread requesrtThread = new Thread(requestExec);

                this.lock.lock();
        
                this.threadCount ++;

                this.lock.unlock();
           
                requesrtThread.start();

            }
            catch(Exception e){
                // System.out.println(e);
            }

        }while(!is_exit);


        try
        {
            if(!servSock.isClosed())
                servSock.close();
        }
        catch(Exception e)
        {
            System.out.println(e);
        }

        while(this.threadCount > 0)
        {
            try{
                Thread.sleep(3000);
            }
            catch(Exception e)
            {
                System.out.println(e);
            }
        }
    }


    public void exit()
    {     
        is_exit = true;

        try
        {
            if(!servSock.isClosed())
                servSock.close();

            // sql_module.close();
        }
        catch(Exception e)
        {
            System.out.println(e);
        }
    }
    
}

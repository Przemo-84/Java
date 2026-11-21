import httpServer.httpServer;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.InputStreamReader;
import java.util.*;

public class mainApp{

    static Boolean is_exit = false;

    static String [] keys = {"listenPort", "host", "base_name", "user", "password", "base_port", "homePage_url"};

    static Map<String,String> dataConfig = new HashMap<>();

    public static void main(String[]argv)
    {
        if(argv.length < 1)
        {
            System.out.println("Wymagany jest argument z nazwą pliku zawierającego dane konfiguracyjne");
            System.exit(1);
        }

        File envFile = new File(argv[0]);

        if(!envFile.exists())
        {
            System.out.println("Podany plik nie istnieje lub zawiera błędne dane");
            System.exit(2);
        }
        try
        {
            BufferedReader envBuff = new BufferedReader(new InputStreamReader(new FileInputStream(envFile)));

            int i = 0;
            String value;

            while((value = envBuff.readLine()) != null && i < 14)
            {

                String key = keys[i];
                dataConfig.put(key, value);
                i++;
            }

            envBuff.close();
        }

        catch(Exception e)
        {
            System.out.println(e);
            System.exit(3);
        }

        try
        {
            httpServer http_serv = new httpServer(140, dataConfig);

            if(!http_serv.is_connected)
            {
                System.exit(4);
            }

            Thread listenThread = new Thread(http_serv);
            listenThread.start();

            BufferedReader systemIn = new BufferedReader(new InputStreamReader(System.in));

            while(!is_exit)
            {
                String command = systemIn.readLine();

                if(command.compareTo("exit")==0)
                {
                    is_exit = true;
                    http_serv.exit();
                    
                }
                else
                {
                    System.out.println("Nierozpoznane polecenie.\nDostępne polecenia to :\n exit - kończy działanie programu");
                }
            }
        }
        catch(Exception e)
        {
            System.out.println(e);
        }        

    }

}

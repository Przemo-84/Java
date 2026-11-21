package httpServer;
import java.io.StringReader;
import java.lang.String;
import java.util.*;
import javax.json.*;

public class requestParser {

    String method = "";
    String path = "";
    String protocol = "";
    Map<String,String> urlParams = new HashMap<>();
    Map<String,String> headers = new HashMap<>();
    int content_length = 0;
    String buffer = "";
    JsonObject jsonObj;

    String methods [] = {"GET", "POST", "PUT", "OPTIONS", "HEAD", "TRACE", "DELETE", "PATH", "CONNECT"};
    String protocols [] = {"HTTP/1.0", "HTTP/1.1", "HTTP/2.0", "HTTP/3.0"}; 

    String textContentType = "text/plain";
    String jsonContentType = "application/json";

    String queryRegExp = "^\\?([\\w-]+(=[\\w-]*)?(&[\\w-]+(=[\\w-]*)?)*)?$";
    String jsonValueRegExp = "^\"([a-zA-Z])+(([-])?([a-zA-Z]))*\": ((true)|(false)|([0-9])+|(\"([a-z A-Z'!@#$%^&*()+=-_,.?ęóśłżźćń<>/0-9](\\\")*)+\"))$";
    String headerRegExp = "^([a-zA-Z])+(([-])?([a-zA-Z]))*: ([a-z A-Z':!-;@#$%^&*()+=-_,.?ęóśłżźćń<>\\/0-9])+$";

    public  requestParser()
    {
       
    }

    public int parse(ArrayList<String> requestBuffer)
    {
        int retVal = 0;
        int index = 0;

        while(index < requestBuffer.size() && requestBuffer.get(index).length() > 0)
        {
            if(index == 0)
            {

                String [] urlStr = requestBuffer.get(index).split(" ");

                if(urlStr.length != 3)
                {
                    retVal = 1;
                    break;
                }

                retVal = 2;

                for(int i = 0; i < methods.length; i++)
                {
                    if(urlStr[0].compareTo(methods[i]) == 0)
                    {
                        this.method = methods[i];
                        retVal = 0;
                        break;
                    }
                }

                if(retVal !=0 || urlStr[1].charAt(0) != '/')
                    return retVal;

                int j = 0 ;
                while(j < urlStr[1].length() && urlStr[1].charAt(j) != '?')
                {
                    this.path  += urlStr[1].charAt(j);
                    j++;
                }

                if(j < urlStr[1].length())
                {   
                    if(urlStr[1].charAt(j) !='?' || j + 3 >= urlStr[1].length())
                    {
                        retVal = 3;
                        break;
                    }

                    String queryS = urlStr[1].substring(j);

                    if(!queryS.matches(this.queryRegExp))
                    {
                        retVal =4;
                        break;
                    }

                    String params[] = (queryS.substring(1)).split("&");

                    if(params.length <= 0)
                    {
                        retVal = 6;
                        break;
                    }

                    for(j = 0; j < params.length; j++)      
                    {
                       String paramItem[] = params[j].split("=");
                       this.urlParams.put(paramItem[0], paramItem[1]);
                    }
                }

                retVal = 7;

                for(int i = 0; i < protocols.length; i++)
                {
                    if(urlStr[2].compareTo(protocols[i]) == 0)
                    {
                        this.protocol = protocols[i];
                        retVal = 0;
                        break;
                    }
                }

                if(retVal != 0)
                    return retVal;
            }
            else
            {
                if(!requestBuffer.get(index).matches(this.headerRegExp))
                {
                    retVal = 8;
                    break;
                }

                String header_key_value[] = requestBuffer.get(index).split(": ");
                this.headers.put(header_key_value[0], header_key_value[1]);
            }

            index ++;
        }

        if(retVal != 0)
            return retVal;
        
        if(requestBuffer.get(index).compareTo("") == 0 && index + 1 <= requestBuffer.size())
        {
            do
            {
                index++;
                if(index >= requestBuffer.size())
                    break;

            }while(requestBuffer.get(index).compareTo("") == 0);

            if(index < requestBuffer.size())
            {
                String contentType = headers.get("Content-Type");
                String contentLength = headers.get("Content-Length");

                try
                {
                   content_length = Integer.parseInt(contentLength, 10);
                    
                } 
                catch (NumberFormatException ex)
                {
                    retVal = 9;
                }
                if(contentType.length() > 0 && retVal == 0)
                {
                   
                    for(int i = index; i < requestBuffer.size(); i++)
                    {
                        buffer = buffer.concat(requestBuffer.get(i));
                        if(i + 1 < requestBuffer.size())
                            buffer = buffer.concat("\r\n");
                    }

                    if(content_length <= buffer.length())
                        retVal = 10;
                    else
                    {
                        if(contentType.compareTo(jsonContentType)==0)
                        {
                            try
                            {
                                JsonReader jsonReader = Json.createReader(new StringReader(buffer));
                                jsonObj = jsonReader.readObject();
                                jsonReader.close();
                            }
                            catch(JsonException jsonException)
                            {
                                System.out.println(jsonException);
                            }
                        }
                    }
                }
                else retVal = 9; 
            }
        }
        return retVal;
    }
    
}

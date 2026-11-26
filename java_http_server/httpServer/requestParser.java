package httpServer;
import java.io.StringReader;
import java.util.*;
import java.lang.String;

import java.nio.charset.StandardCharsets;

import javax.json.*;

public class requestParser {

    public String method = "";
    public String path = "";
    public String protocol = "";
    public Map<String,String> urlParams = new HashMap<>();
    public Map<String,String> headers = new HashMap<>();
    public char [] requestBuffer_char = new char[15000];
    public int requestBuffer_char_size = 0;
    public char [] bodyChar;
    public JsonObject jsonObj;

    int retVal = 0;

    int content_length = 0;

    ArrayList <String>requestBuffer_headersList = null;
    String requestBuffer_str = null ;

    boolean getAllHeaders = false;


    String methods [] = {"GET", "POST", "PUT", "OPTIONS", "HEAD", "TRACE", "DELETE", "PATH", "CONNECT"};
    String protocols [] = {"HTTP/1.0", "HTTP/1.1", "HTTP/2.0", "HTTP/3.0"}; 

  //  String textContentType = "text/plain";
    String jsonContentType = "application/json";

    String queryRegExp = "^\\?([\\w-]+(=[\\w-]*)?(&[\\w-]+(=[\\w-]*)?)*)?$";
   // String jsonValueRegExp = "^\"([a-zA-Z])+(([-])?([a-zA-Z]))*\": ((true)|(false)|([0-9])+|(\"([a-z A-Z'!@#$%^&*()+=-_,.?ęóśłżźćń<>/0-9](\\\")*)+\"))$";
    String headerRegExp = "^([a-zA-Z])+(([-])?([a-zA-Z]))*: ([a-z A-Z':!-;@#$%^&*()+=-_,.?ęóśłżźćń<>\\/0-9])+$";

    public  requestParser()
    {
       
    }

    public int parse(char [] buffer, int buffer_size)
    {
        int index = 0;

        int bodyByte_index = 2;

        if(buffer_size > 15000)
            return retVal = 1;
        
        if(buffer_size > requestBuffer_char_size)
        {
            System.arraycopy(buffer, requestBuffer_char_size, requestBuffer_char,requestBuffer_char_size, buffer_size - requestBuffer_char_size);
            requestBuffer_char_size = buffer_size;
        }
        else 
            return retVal;
        
        requestBuffer_str = new String(requestBuffer_char, 0, buffer_size);

        if(requestBuffer_str.indexOf("\r\n\r\n")==-1)
            return retVal = -1;
     
        Scanner sc = new Scanner(requestBuffer_str);

        requestBuffer_headersList = new ArrayList<String>();

        getAllHeaders = false;

        while(sc.hasNextLine())
        {
            String headerItem = sc.nextLine();

            if(headerItem.compareTo("") == 0)
            {
                getAllHeaders = true;
                break;
            }

            requestBuffer_headersList.add(headerItem);
        }

        sc.close();

        if(!getAllHeaders)
            return retVal = -2;

        method = "";
        path = "";
        protocol = "";
        urlParams.clear();
        headers.clear();

        while(requestBuffer_headersList.size() > index)
        {
            if(index == 0)
            {

                bodyByte_index += requestBuffer_headersList.get(index).length() + 2;

                String [] urlStr = requestBuffer_headersList.get(index).split(" ");

                if(urlStr.length != 3)
                {
                    retVal = 2;
                    break;
                }

                retVal = 3;

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
                        retVal = 4;
                        break;
                    }

                    String queryS = urlStr[1].substring(j);

                    if(!queryS.matches(this.queryRegExp))
                    {
                        retVal =5;
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
                String headerItem = requestBuffer_headersList.get(index);

                if(!headerItem.matches(this.headerRegExp))
                {
                    retVal = 8;
                    break;
                }

                bodyByte_index += headerItem.length() + 2;

                String header_key_value[] = headerItem.split(": ");
                this.headers.put(header_key_value[0], header_key_value[1]);
            }

            index ++;
           
        }

        if(retVal != 0)
            return retVal;
  
        if( method.compareTo("POST") == 0)
        {
            String contentType = headers.get("Content-Type");
            String contentLength = headers.get("Content-Length");

            if(contentType == null || contentLength == null)
                return retVal = 10;

            try
            {
                content_length = Integer.parseInt(contentLength, 10);
                
            } 
            catch (NumberFormatException ex)
            {
                retVal = 11;
            }
            
            if(content_length == 0)
            {
                if(bodyByte_index < buffer_size)
                    return retVal =  -3;
                else if(bodyByte_index == buffer_size)
                    return retVal = 0;
                else return retVal = 12;
            }

            if(bodyByte_index + 1 <= buffer_size)
            {


                if(content_length >= 0 && retVal == 0)
                {
         
                    bodyChar = Arrays.copyOfRange(requestBuffer_char, bodyByte_index, requestBuffer_char_size);
                    
                    byte [] bodyByte = new String(bodyChar).getBytes(StandardCharsets.UTF_8);

                    int bodyChar_size = bodyByte.length;

                    if(content_length != bodyChar_size)
                        return retVal= -4;

                    if(contentType.compareTo(jsonContentType)==0)
                    {
                        try
                        {
                            String bodyStr = new String(bodyByte,0, bodyByte.length, StandardCharsets.UTF_8);

                            JsonReader jsonReader = Json.createReader(new StringReader(bodyStr));
                            jsonObj = jsonReader.readObject();
                            jsonReader.close();
                        }
                        catch(JsonException jsonException)
                        {
                            System.out.println(jsonException);
                            retVal = 13;
                        }
                    }
                    
                }
                else retVal = 14; 
            }
            else retVal = -5;
        }
        return retVal;
    }
    
}

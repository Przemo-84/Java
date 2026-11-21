package sqlModule;

import java.util.*;
import java.util.Date;
import java.sql.*;

import com.zaxxer.hikari.*;


public class sqlModule{
    
    private static Map<String,String> dataConfig =  new HashMap<>();

    private static HikariConfig config;
    private static HikariDataSource hikari_ds;

    private static String postgres_url = "";


    public sqlModule(Map<String,String> dataConf)
    {
        for(Map.Entry<String,String> entry : dataConf.entrySet())
        {
            dataConfig.put(entry.getKey(), entry.getValue());
        }

        postgres_url = "jdbc:postgresql://" + dataConfig.get("host")+":"+dataConfig.get("base_port")+"/"+dataConfig.get("base_name");

        Properties hikariProps = new Properties();

        hikariProps.setProperty("jdbcUrl", postgres_url);
        hikariProps.setProperty("username", dataConfig.get("user"));
        hikariProps.setProperty("password", dataConfig.get("password"));
        hikariProps.setProperty("dataSource.cachePrepStmts", "true");
        hikariProps.setProperty("dataSource.prepStmtCacheSize", "250");
        hikariProps.setProperty("dataSource.prepStmtCacheSqlLimit", "2048");
        hikariProps.setProperty("dataSource.connectionTimeout", "5000");
        hikariProps.setProperty("dataSource.maximumPoolSize", "5");

        config = new HikariConfig(hikariProps);

        // config.setJdbcUrl(dataConfig.get(postgres_url));
        // config.setUsername(dataConfig.get("user"));
        // config.setPassword(dataConfig.get("password"));
        // config.addDataSourceProperty( "cachePrepStmts" , "true" );
        // config.addDataSourceProperty( "prepStmtCacheSize" , "250" );
        // config.addDataSourceProperty( "prepStmtCacheSqlLimit" , "2048" );
        // config.addDataSourceProperty( "connectionTimeout" , "30000 " );
        // config.addDataSourceProperty( "maximumPoolSize" , "10" );

        hikari_ds = new HikariDataSource(config);
    }

    public void close()
    {
        if(hikari_ds != null)
            hikari_ds.close();
    }

    public int getHotAuction(Map<String,String> auctionObj)
    {
        Date time = new Date();
        long currentTime = time.getTime();

        int retVal = 0;

        String query1 = "SELECT MIN(auction_time) FROM auctions WHERE auction_time > " + currentTime;

        try
        {
            Connection con = hikari_ds.getConnection();
            PreparedStatement  statement = con.prepareStatement(query1);

            ResultSet rs = statement.executeQuery();

            if(rs.next())
            { 
                long min_auction_time = rs.getLong(1);

                if(min_auction_time > 0)
                {

                    String query2 = "SELECT auction_id, job_title, auction_time FROM auctions WHERE auction_time=" + min_auction_time;

                    statement = con.prepareStatement(query2);

                    rs = statement.executeQuery();

                    if(rs.next())
                    {
                        auctionObj.put("auction_id", rs.getString(1));
                        auctionObj.put("job_title", rs.getString(2));
                        auctionObj.put("auction_time", rs.getString(3));
                    }
                    else 
                        retVal = 3;
                }
                else
                    retVal = 2;
            }
            else
               retVal = 2;

            con.close();
        }
        catch(SQLException sql_e)
        {
            System.out.println(sql_e);
            return 1;
        }

        if(retVal == 2 || retVal == 3)
        {
            auctionObj.put("auction_id", "");
            auctionObj.put("job_title", "");
            auctionObj.put("auction_time", "");

            retVal = 0;
        }

        return retVal;
    }
  
}

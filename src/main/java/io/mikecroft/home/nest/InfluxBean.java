package io.mikecroft.home.nest;

import org.influxdb.InfluxDB;
import org.influxdb.InfluxDBFactory;
import org.influxdb.dto.Point;
import org.influxdb.dto.Query;
import org.influxdb.dto.QueryResult;

import javax.annotation.PostConstruct;
import javax.ejb.Singleton;
import java.util.concurrent.TimeUnit;

/**
 * Created by mike on 30/11/16.
 */
@Singleton
public class InfluxBean {

    InfluxDB influxDB;
    String dbName = "tempTest1";

    @PostConstruct
    private void setUp(){
        influxDB = InfluxDBFactory.connect("http://localhost:8086", "root", "root");
        influxDB.createDatabase(dbName);

        // Flush every 2 points, at least every 5 seconds
        influxDB.enableBatch(2, 5, TimeUnit.SECONDS);
    }


    public void write(Point p){
        influxDB.write(dbName, "autogen", p);
    }

    public QueryResult query(String q){
        return influxDB.query(new Query(q, dbName));
    }

}

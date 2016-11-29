package io.mikecroft.home.nest;

import com.bwssystems.nest.controller.Nest;
import com.bwssystems.nest.controller.NestSession;
import com.bwssystems.nest.controller.Thermostat;
import com.bwssystems.nest.protocol.error.LoginException;
import org.influxdb.InfluxDB;
import org.influxdb.InfluxDBFactory;
import org.influxdb.dto.Point;
import org.influxdb.dto.Query;
import org.influxdb.dto.QueryResult;

import javax.annotation.PostConstruct;
import javax.ejb.Schedule;
import javax.ejb.Singleton;
import javax.ejb.Startup;
import java.util.Set;
import java.util.concurrent.TimeUnit;

/**
 * Created by mike on 24/11/16.
 */
@Singleton
@Startup
public class TemperatureMonitor {

    private NestSession theSession = null;
    private String username = System.getProperty("un");
    private String password = System.getProperty("pw");
    private String homeNameFromSet = System.getProperty("home");
    private String thermoNameFromSet = System.getProperty("therm");

    @PostConstruct
    private void init(){


        try {
            theSession = new NestSession(username, password);
        } catch (LoginException e) {
            System.out.println("Caught Login Exception, exiting....");
            System.exit(1);
        }

        Nest theNest = new Nest(theSession);

        Set<String> thermoNames = theNest.getThermostatNames(); /* list of thermostats in all structure */
        Thermostat thermo1 = theNest.getThermostat(thermoNameFromSet);

        System.out.println("\n");
        System.out.println("Humid: -- " + thermo1.getDeviceDetail().getCurrentHumidity());
        System.out.println("Temp:  -- " + thermo1.getSharedDetail().getCurrentTemperature());
        System.out.println("\n");

    }


    @Schedule(hour = "*", minute = "*", second = "*/3", info = "Every 3 second timer", timezone = "UTC")
    private void logData(){
        Nest theNest = new Nest(theSession);

        Set<String> thermoNames = theNest.getThermostatNames(); /* list of thermostats in all structure */
        Thermostat thermo1 = theNest.getThermostat(thermoNameFromSet);

        Double temp = thermo1.getSharedDetail().getCurrentTemperature();
        Long humid = thermo1.getDeviceDetail().getCurrentHumidity();
        System.out.println("\n");
        System.out.println("Humid: -- " + humid);
        System.out.println("Temp:  -- " + temp);
        System.out.println("\n");


        InfluxDB influxDB = InfluxDBFactory.connect("http://localhost:8086", "root", "root");
        String dbName = "tempTest1";


        influxDB.createDatabase(dbName);

        // Flush every 2 points, at least every 5 seconds
        influxDB.enableBatch(2, 5, TimeUnit.SECONDS);
        Point point1 = Point.measurement("hallway")
                .time(System.currentTimeMillis(), TimeUnit.MILLISECONDS)
                .addField("temperature", temp)
                .addField("humidity", humid)
                .build();

        influxDB.write(dbName, "autogen", point1);

        /*
        TODO: Clean up InfluxDB stuff so it's not terrible
        https://github.com/influxdata/influxdb-java
        Should also create scripts to provision InfluxDB (create tables, CQs etc)

        TODO: Notify via PushBullet/IFTTT
        https://github.com/silk8192/jpushbullet
        */

    }

    @Schedule(hour = "*", minute = "*", second = "*/3", info = "Every 3 second timer", timezone = "UTC")
    private void queryTable(){
        InfluxDB influxDB = InfluxDBFactory.connect("http://localhost:8086", "root", "root");
        String dbName = "tempTest1";


        influxDB.createDatabase(dbName);
        Query query = new Query("SELECT * FROM hallway", dbName);
        QueryResult query1 = influxDB.query(query);
        for (QueryResult.Result r : query1.getResults()){
            System.out.println(r.toString());
        }
    }


}

package io.mikecroft.home.nest;

import com.bwssystems.nest.controller.Nest;
import com.bwssystems.nest.controller.NestSession;
import com.bwssystems.nest.controller.Thermostat;
import com.bwssystems.nest.protocol.error.LoginException;
import org.influxdb.dto.Point;
import org.influxdb.dto.QueryResult;

import javax.annotation.PostConstruct;
import javax.ejb.Schedule;
import javax.ejb.Startup;
import javax.inject.Inject;
import java.util.concurrent.TimeUnit;

/**
 * Created by mike on 24/11/16.
 */
@Startup
public class TemperatureMonitor {

    private NestSession theSession = null;
    private String username = System.getProperty("un");
    private String password = System.getProperty("pw");
    private String homeNameFromSet = System.getProperty("home");
    private String thermoNameFromSet = System.getProperty("therm");

    @Inject
    private InfluxBean influxBean;

    private Nest theNest;
    private Thermostat thermo1;

    @PostConstruct
    private void init(){
        try {
            theSession = new NestSession(username, password);
        } catch (LoginException e) {
            System.out.println("Caught Login Exception, exiting....");
            System.exit(1);
        }

        theNest = new Nest(theSession);
        thermo1 = theNest.getThermostat(thermoNameFromSet);

        logData();
    }


    @Schedule(hour = "*", minute = "*", second = "*/3", info = "Every 3 second timer", timezone = "UTC")
    private void logData(){

        Double temp = thermo1.getSharedDetail().getCurrentTemperature();
        Long humid = thermo1.getDeviceDetail().getCurrentHumidity();
        System.out.println("\n");
        System.out.println("Humid: -- " + humid);
        System.out.println("Temp:  -- " + temp);
        System.out.println("\n");

        Point pt = Point.measurement("hallway")
                .time(System.currentTimeMillis(), TimeUnit.MILLISECONDS)
                .addField("temperature", temp)
                .addField("humidity", humid)
                .build();

        influxBean.write(pt);

        /*
        TODO: Clean up InfluxDB stuff so it's not terrible
        https://github.com/influxdata/influxdb-java
        Should also create scripts to provision InfluxDB (create tables, CQs etc)
        */
    }

    @Schedule(hour = "*", minute = "*", second = "*/3", info = "Every 3 second timer", timezone = "UTC")
    private void queryTable(){
        QueryResult query1 = influxBean.query("SELECT * FROM hallway");
        for (QueryResult.Result r : query1.getResults()){
            System.out.println(r.toString());
        }
    }
}

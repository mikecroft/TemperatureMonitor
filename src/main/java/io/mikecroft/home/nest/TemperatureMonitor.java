package io.mikecroft.home.nest;

import com.bwssystems.nest.controller.Nest;
import com.bwssystems.nest.controller.NestSession;
import com.bwssystems.nest.controller.Thermostat;
import com.bwssystems.nest.protocol.error.LoginException;

import javax.annotation.PostConstruct;
import javax.ejb.Schedule;
import javax.ejb.Singleton;
import javax.ejb.Startup;
import java.util.Set;

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

        System.out.println("\n");
        System.out.println("Humid: -- " + thermo1.getDeviceDetail().getCurrentHumidity());
        System.out.println("Temp:  -- " + thermo1.getSharedDetail().getCurrentTemperature());
        System.out.println("\n");

        /*
        TODO: Persist to InfluxDB
        https://github.com/influxdata/influxdb-java
        Should also create scripts to provision InfluxDB (create tables, CQs etc)

        TODO: Notify via PushBullet/IFTTT
        https://github.com/silk8192/jpushbullet
        */

    }

}

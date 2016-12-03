package io.mikecroft.home.nest;

import com.bwssystems.nest.controller.Home;
import com.bwssystems.nest.controller.Nest;
import com.bwssystems.nest.controller.NestSession;
import com.bwssystems.nest.controller.Thermostat;
import com.bwssystems.nest.protocol.error.LoginException;
import com.github.fedy2.weather.YahooWeatherService;
import com.github.fedy2.weather.data.Channel;
import com.github.fedy2.weather.data.unit.DegreeUnit;
import org.influxdb.dto.Point;
import org.influxdb.dto.QueryResult;

import javax.annotation.PostConstruct;
import javax.ejb.Schedule;
import javax.ejb.Singleton;
import javax.ejb.Startup;
import javax.inject.Inject;
import java.util.concurrent.TimeUnit;

/**
 * Created by mike on 24/11/16.
 */
@Startup
@Singleton
public class TemperatureMonitor {

    private NestSession theSession = null;
    private String username = System.getProperty("un");
    private String password = System.getProperty("pw");
    private String homeNameFromSet = System.getProperty("home");
    private String thermoNameFromSet = System.getProperty("therm");
    private String woeid = System.getProperty("woeid");

    @Inject
    private InfluxBean influxBean;

    private Nest theNest;
    private Thermostat thermo1;
    private Home home;

    @PostConstruct
    private void init() {
        try {
            System.out.println("Logging in...");
            theSession = new NestSession(username, password);
        } catch (LoginException e) {
            System.out.println("Caught Login Exception, exiting....");
            System.exit(1);
        }

        theNest = new Nest(theSession);
        thermo1 = theNest.getThermostat(thermoNameFromSet);
        home = theNest.getHome(homeNameFromSet);

        logData();
    }


    @Schedule(hour = "*", minute = "*", second = "1", info = "Every minute timer", timezone = "UTC")
    private void logData() {
        influxBean.write(getThermState());
        influxBean.write(getOutsideTemperature(this.woeid));
    }

    private Point getThermState() {

        Double temp = thermo1.getSharedDetail().getCurrentTemperature();
        Long humid = thermo1.getDeviceDetail().getCurrentHumidity();
//        Boolean canHeat = thermo1.getSharedDetail().getCanHeat();
//        thermo1.getSharedDetail().getTargetTemperature();
        return Point.measurement("hallway")
                .time(System.currentTimeMillis(), TimeUnit.MILLISECONDS)
                .addField("temperature", temp)
                .addField("humidity", humid)
//                .addField("canHeat", canHeat)
                .build();

    }

    private Point getOutsideTemperature(String woeid) {
        int temp = 0;
        YahooWeatherService service;
        Channel channel;

        try {
            service = new YahooWeatherService();
            channel = service.getForecast(woeid, DegreeUnit.CELSIUS);
            System.out.println(channel.getDescription());
            temp = channel.getItem().getCondition().getTemp();

        } catch (Exception e) {
            System.out.println("Exception: " + e.getMessage());
        }
        return Point.measurement("weather")
                .time(System.currentTimeMillis(), TimeUnit.MILLISECONDS)
                .addField("temperature", temp)
                .build();
    }

    @Schedule(hour = "*", minute = "1", second = "1", info = "Every hour timer", timezone = "UTC")
    private void queryTable() {
        QueryResult query1 = influxBean.query("SELECT * FROM hallway");
        for (QueryResult.Result r : query1.getResults()) {
            System.out.println(r.toString());
        }
    }
}

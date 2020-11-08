package com.soen343.smarthomesimulator.controllers;

import com.soen343.smarthomesimulator.models.Home;
import com.soen343.smarthomesimulator.models.User;
import com.soen343.smarthomesimulator.services.HomeService;
import com.soen343.smarthomesimulator.services.UserService;
import net.minidev.json.JSONObject;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.sql.Timestamp;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Class HomeController
 * <p>
 * Controller for the Home component. Contains the endpoints that perform logic on the home entity.
 */
@RestController
public class HomeController {

    @Autowired
    HomeService homeService;

    @Autowired
    UserService userService;

    private Map<String, String> response;

    public HomeController() {
        this.response = new HashMap<String, String>();
        this.response.put("success", "true");
    }

    /**
     * POST endpoint to <code>/home/store</code>
     * <p>
     * This endpoint creates a new Home entity and saves it to the database.
     *
     * @param name Defines the name of the home being saved.
     * @return Response containing the operation's status.
     */
    @PostMapping("/home/store")
    public JSONObject store(@RequestParam(value = "name") String name) {
        homeService.save(new Home(name));
        return new JSONObject(this.response);
    }

    @PostMapping("/home/update")
    public JSONObject update(@RequestParam(value = "id") Long id,
                             @RequestParam(value = "temperature", required = false) Integer temperature,
                             @RequestParam(value = "date", required = false) String date,
                             @RequestParam(value = "security_level", required = false) String securityLevel) {
        Home home = homeService.findById(id);

        if (temperature != null) {
            home.setOutside_temp(temperature);
        }
        System.out.println(date);
        if (date != null) {
            home.setDate(Timestamp.valueOf(date));
        }

        if (securityLevel != null) {
            boolean armed = this.armAlarm(home, securityLevel);
            if (!armed) {
                this.response.put("message", "Alarm cannot be engaged because there are still users present in the home.");
            }
        }

        homeService.save(home);
        return new JSONObject(this.response);
    }

    private boolean armAlarm(Home home, String securityLevel) {
        List<User> users = userService.findAll();
        for (User user : users) {
            if (user.getHome().getId().equals(home.getId()) && user.getZone() != null) {
                return false;
            }
        }
        home.setSecurityLevel(securityLevel);
        homeService.save(home);
        return true;
    }

}

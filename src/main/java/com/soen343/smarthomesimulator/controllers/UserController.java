package com.soen343.smarthomesimulator.controllers;

import com.soen343.smarthomesimulator.exceptions.UnauthorizedActionException;
import com.soen343.smarthomesimulator.models.Appliance;
import com.soen343.smarthomesimulator.models.Home;
import com.soen343.smarthomesimulator.models.User;
import com.soen343.smarthomesimulator.models.Zone;
import com.soen343.smarthomesimulator.services.ApplianceService;
import com.soen343.smarthomesimulator.services.HomeService;
import com.soen343.smarthomesimulator.services.UserService;
import com.soen343.smarthomesimulator.services.ZoneService;
import net.minidev.json.JSONObject;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.web.bind.annotation.*;

import javax.servlet.http.HttpServletResponse;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
public class UserController {

    @Autowired
    UserService userService;

    @Autowired
    HomeService homeService;

    @Autowired
    ZoneService zoneService;

    @Autowired
    ApplianceService applianceService;

    private Map<String, String> response;

    public UserController() {
        this.response = new HashMap<String, String>();
        this.response.put("success", "true");
    }

    /**
     * GET endpoint to <code>/users</code>
     * 
     * Sets the state of a particular opening during the simulation.
     * 
     * @param id The id of the opening.
     * @param state The state to set the opening to.
     * @return The response status of the operation.
     */
    @GetMapping("/users")
    public List<User> index() {
        return userService.findAll();
    }

    /**
     * POST endpoint to <code>/user/update</code>
     * 
     * Updates user information.
     * 
     * @param id User Entity ID
     * @param homeId Home Entity ID
     * @param zoneId Zone Entity ID
     * @param name User's name to be updated to
     * @param email User's email to be updated to
     * @param password User's password to be updated to
     * @return Returns a JSON status object based on the operation status.
     */
    @PostMapping("/user/update")
    public JSONObject update(@RequestParam(value = "id") Long id,
                             @RequestParam(value = "home_id", required = false) Long homeId,
                             @RequestParam(value = "zone_id", required = false) Long zoneId,
                             @RequestParam(value = "name", required = false) String name,
                             @RequestParam(value = "email", required = false) String email,
                             @RequestParam(value = "password", required = false) String password) {

        User user = userService.findById(id);
        setUserHome(homeId, user);
        setUserZone(zoneId, user);
        getAppliancesFromZone(user);

        if (user.getHome().getSecurityLevel() != null && user.getHome().getSecurityLevel().equals(Home.SECURITY_ARMED) && user.getZone().getId() != 0) {
            this.response.put("success", "false");
            this.response.put("message", "Alarm has been triggered. Please leave the home and disable the alarm.");
            return new JSONObject(this.response);
        }

        if (name != null) {
            user.setName(name);
        }

        if (email != null) {
            user.setEmail(email);
        }

        if (password != null) {
            user.setPassword(passwordEncoder().encode(password));
        }

        userService.save(user);

        return new JSONObject(this.response);
    }

    /**
     * Find Home object using the homeId. If found, save the user's home as the found home.
     * 
     * @param user Current logged in user
     * @param homeId ID of home used to find Home object to be updated to
     */
    public void setUserHome(Long homeId, User user){
        if (homeId != null && homeService.exists(homeId)) {
            Home home = homeService.findById(homeId);
            user.setHome(home);
        } else if (homeId != null) {
            this.response.put("home", "Home supplied does not exist");
        }
    }
    
    /**
     * Find Zone object using the zoneId. If found, save the user's zone as the found zone.
     * 
     * @param user Current logged in user
     * @param zoneId ID of zone used to find Zone object to be updated to
     */
    public void setUserZone(Long zoneId, User user){
        if (zoneId != null && zoneService.exists(zoneId)) {
            Zone zone = zoneService.findById(zoneId);
            user.setZone(zone);
        } else if (zoneId != null && zoneId == 0) {
            user.setZone(null);
        } else if (zoneId != null) {
            this.response.put("zone", "Zone supplied does not exist");
        }
    }

    /**
    * Fetches appliances from user's current zone. Saves appliances to applianceService.
    * 
    * @param user Current logged in user
    */
    public void getAppliancesFromZone(User user){
        if (user.getHome().getAutoMode() == 1) {
            for (Appliance appliance: user.getZone().getAppliances()) {
                if (appliance != null && appliance.getType().equals("light") && appliance.getState() != 1) {
                    appliance.setState(1);
                    applianceService.save(appliance);
                }
            }
        }
    }
    
    /**
     * GET endpoint to <code>/user</code>
     * 
     * Finds user and retrieves user data based on ID.
     * 
     * @param id User ID
     * @return Returns the User data
     */
    @GetMapping("/user")
    public User show(@RequestParam(value = "id") Long id) {
        return userService.findById(id);
    }

    /**
     * GET endpoint to <code>/user/login</code>
     * 
     * Gets user by email and password
     * 
     * @param email User's email
     * @param password User's password
     * @return Returns the User data
     */
    @GetMapping("/user/login")
    public User getUser(@RequestParam(value = "email") String email,
                        @RequestParam(value = "password") String password) {

        return userService.findUserByCredentials(email, password);
    }

    /**
     * GET endpoint to <code>/user/current</code>
     * 
     * Retrieves the current authenticated user's information.
     * 
     * @return Current User object
     */
    @GetMapping("/user/current")
    public User currentUser() {
        /**
         * case when no user is logged in, the principal is set to string 'anonymousUser' by default;
         * if a user is logged in, cast the principal to User
         */ 
        if (SecurityContextHolder.getContext().getAuthentication().getPrincipal().getClass() == String.class)
            return null;
        else {
            return (User) SecurityContextHolder.getContext().getAuthentication().getPrincipal();
        }
    }

    /**
     * POST endpoint to <code>/user/store</code>
     * 
     * Signs a User entity up.
     * 
     * @param name User's name
     * @param email User's email
     * @param password User's password
     * @param isParent Boolean if the User's type is parent
     * @param isChild Boolean if the User's type is child
     * @param isGuest Boolean if the User's type is guest
     * @return Returns a JSON status object based on the operation status.
     */
    @PostMapping(value = "/user/store")
    public JSONObject store(@RequestParam(value = "name") String name,

                        @RequestParam(value = "email") String email,
                        @RequestParam(value = "password") String password,
                        @RequestParam(value = "isParent") Boolean isParent,
                        @RequestParam(value = "isChild") Boolean isChild,
                        @RequestParam(value = "isGuest") Boolean isGuest) {

        password = this.passwordEncoder().encode(password);
        String role;

        if (isParent) {
            role = User.ROLE_PARENT;
        } else if (isChild) {
            role = User.ROLE_CHILD;
        } else {
            role = User.ROLE_USER;
        }

        if (userService.save(new User(name, email, password, role)) != null) {
            return new JSONObject(this.response);
        }

        this.response.put("success", "false");
        return new JSONObject(this.response);
    }

    /**
     * Allows for deleting of a user's account
     * @param id The Id of a user to be destroyed
     * @return JSONObject
     */
    @PostMapping("/users/destroy")
    public JSONObject destroy(@RequestParam(value = "id") Long id) {
        User current = currentUser();
        if (current != null && (current.getId().equals(id) || current.getRole().equals(User.ROLE_ADMIN))) {
            userService.deleteById(id);
        } else {
            // TODO: Add exception that sets status code to 403 here
            this.response.put("success", "false");
            throw new UnauthorizedActionException();
        }
        return new JSONObject(this.response);
    }

    public BCryptPasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }
}
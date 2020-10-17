package com.soen343.smarthomesimulator.controllers;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.soen343.smarthomesimulator.models.Opening;
import com.soen343.smarthomesimulator.models.Zone;
import com.soen343.smarthomesimulator.services.OpeningService;
import com.soen343.smarthomesimulator.services.UserService;
import com.soen343.smarthomesimulator.services.ZoneService;
import net.minidev.json.JSONArray;
import net.minidev.json.JSONObject;
import net.minidev.json.parser.JSONParser;
import net.minidev.json.parser.ParseException;
import org.apache.logging.log4j.util.StringMap;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;


import java.io.IOException;
import java.io.InputStream;
import java.io.UnsupportedEncodingException;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;


@RestController
public class ZoneController {


    @Autowired
    ZoneService zoneService;

    @Autowired
    OpeningService openingService;

    private Map<String, String> response;

    public ZoneController() {
        this.response = new HashMap<String, String>();
        this.response.put("success", "true");
    }

    @PostMapping("/zones/load")
    public JSONObject load(@RequestParam(value = "layout") MultipartFile layout) {
        if (!layout.getContentType().equals("application/json")) {
            this.response.put("success", "false");
            this.response.put("message", "File Supplied is not a JSON text file. File type supplied: ".concat(layout.getContentType()));
            return new JSONObject(this.response);
        }

        InputStream encoded;
        try {
            encoded = layout.getInputStream();
        } catch (IOException e) {
            this.response.put("success", "false");
            this.response.put("message", "File Store failure.");
            this.response.put("exception", e.toString());
            return new JSONObject(this.response);
        }

//        ObjectMapper?
        JSONParser loaded = new JSONParser(JSONParser.DEFAULT_PERMISSIVE_MODE);
        JSONArray parsed = new JSONArray();
        try {
            parsed = (JSONArray) loaded.parse(encoded);
        } catch (ParseException e) {
            this.response.put("success", "false");
            this.response.put("message", "Parsing Failure");
            this.response.put("exception", e.toString());
        } catch (UnsupportedEncodingException e) {
            this.response.put("success", "false");
            this.response.put("message", "Unsupported Encoding");
            this.response.put("exception", e.toString());
        }

        // Build zones + openings
        for (Object z : parsed) {
            Zone zone = new Zone(handleGet(z, "name"));
            zoneService.save(zone);

            int wCount = Integer.parseInt(handleGet(z, "windows"));
            for (int i = 0; i < wCount; i++) {
                openingService.save(new Opening("window", zone));
            }

            int dCount = Integer.parseInt(handleGet(z, "doors"));
            for (int j = 0; j < dCount; j++) {
                openingService.save(new Opening("door", zone));
            }
        }


        this.response.put("layout", parsed.toString());
        return new JSONObject(this.response);
    }


    private String handleGet(Object obj, String field) {
        return new JSONObject((HashMap<String, String>) obj).get(field).toString();
    }
}

package com.toy.weather.component;

import java.util.HashMap;
import java.util.Map;

/**
 * Created by abhijitdc on 1/4/19.
 *
 * Enumeration for three applicable weather conditions.
 */
public enum WeatherCondition {

    SUNNY("Sunny", 0),
    RAIN("Rain", 1),
    SNOW("Snow", 2);

    private String condName;
    private int index;

    public String getCondName() {
        return condName;
    }

    public int getIndex() {
        return index;
    }


    //utility data structure to lookup enum by index, since libsvm deals with numeric values
    public static final Map<Integer, WeatherCondition> LOOKUP = new HashMap<>();

    static {
        for (WeatherCondition wc : values())
            LOOKUP.put(wc.index, wc);
    }

    WeatherCondition(String condName, int index) {
        this.condName = condName;
        this.index = index;
    }
}

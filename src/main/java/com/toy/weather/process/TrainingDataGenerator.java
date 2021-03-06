package com.toy.weather.process;

import com.toy.weather.component.GeoLocation;
import com.toy.weather.component.Sensor;
import com.toy.weather.component.SensorType;
import com.toy.weather.component.WeatherCondition;
import com.toy.weather.util.LocationSampleGenerator;
import org.apache.commons.math3.distribution.NormalDistribution;
import org.apache.commons.math3.distribution.UniformRealDistribution;

import java.io.*;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;

/**
 * Created by abhijitdc on 1/4/19.
 * <p>
 * This class generates the simulated training data. First Markov chain is used to predict the next weather
 * condition. Then utilizing that condition a set of predefined functions will produce a sensor data
 * within a given range with a normal distribution.
 * <p>
 * Locations to generate the training data is chosen at random from elevation_DE.BMP file to utilize
 * the real world topography.
 * Generated training data will be stored * in target/tmp/" + RUNID + "/training.dat
 * RUNID helps keep all training/predicted data and model organized in one directory.
 */
public class TrainingDataGenerator {

    private LocalDateTime startDate;
    private int noOfDays;
    private NormalDistribution nd = new NormalDistribution();
    private UniformRealDistribution ud = new UniformRealDistribution();
    private List<GeoLocation> sampleLocations;
    private int noOfGeoLocations;
    private Map<SensorType, Sensor> sensorCollection;
    private long RUNID;

    /**
     * @param startDate        - start date of the training data.
     * @param noOfGeoLocations - number of locations for which training data will be generated.
     * @param noOfDays         - number of individual days for which each location will give one set of sensor data.
     * @param RUNID            - RUNID to keep te generated data organized into a single directory.
     * @throws InstantiationException
     */
    public TrainingDataGenerator(LocalDateTime startDate, int noOfGeoLocations, int noOfDays, long RUNID) throws InstantiationException {
        this.startDate = startDate;
        this.noOfDays = noOfDays;
        this.noOfGeoLocations = noOfGeoLocations;
        this.RUNID = RUNID;
        init();
    }

    /**
     * This method defines the sensor specific functions to select normally distributed
     * value from a given range based on the weather condition.
     *
     * @throws InstantiationException
     */
    private void init() throws InstantiationException {
        try {
            this.sampleLocations = new LocationSampleGenerator().samples(noOfGeoLocations);
            sensorCollection = new HashMap<>();

            Sensor tempSensor = new Sensor("TEMPERATURE",
                    () -> 3.0 + 5.0 * nd.sample(),
                    () -> 20.5 + 5.0 * nd.sample(),
                    () -> -10.0 + 0.5 * nd.sample(), SensorType.TEMPSENSOR);

            Sensor humiditySensor = new Sensor("HUMIDITY",
                    () -> (70.0 - 30.0) * ud.sample() + 30.0,
                    () -> (95.0 - 70.0) * ud.sample() + 70.0,
                    () -> (70.0 - 40.0) * ud.sample() + 40.0, SensorType.HUMIDSENSOR);

            Sensor pressureSensor = new Sensor("PRESSURE",
                    () -> 700.0 + 0.2 * nd.sample(),
                    () -> 900.0 + 1.5 * nd.sample(),
                    () -> 800.0 + 2.0 * nd.sample(), SensorType.PRESSURESENSOR);

            sensorCollection.put(SensorType.TEMPSENSOR, tempSensor);
            sensorCollection.put(SensorType.HUMIDSENSOR, humiditySensor);
            sensorCollection.put(SensorType.PRESSURESENSOR, pressureSensor);

        } catch (IOException e) {
            throw new InstantiationException("Failed to get GeoLocation samples");
        }
    }


    /**
     * This method first selects the appropriate probability vector for Markov chain. Then starts with a
     * random weather condition and then transition to select the next weather condition.
     * For each selected weather condition appropriate function is executed to generate a
     * simulated continuous value for temperature, humidity and pressure for the location and the day the year.
     * <p>
     * Generate all the sensor data and weather condition for a given location for each day.
     * Output file is in LIBSVM format
     * @throws Exception
     */
    public void generateTraingData() throws Exception {

        String datapath = "target/tmp/" + RUNID + "/training.dat";
        File fs = new File("target/tmp/" + RUNID);
        if (!fs.exists()) fs.mkdirs();


        try (BufferedWriter bw = new BufferedWriter(new FileWriter(datapath, false))) {
            for (GeoLocation gcl : sampleLocations) {
                //select a random starting weather condition for the geo location
                WeatherCondition wCond = WeatherCondition.LOOKUP.get(new Random().nextInt(3));
                LocalDateTime sampleDate = LocalDateTime.of(startDate.getYear(), startDate.getMonth(), startDate.getDayOfMonth(), startDate.getHour(), startDate.getMinute(), startDate.getSecond());

                for (int i = 0; i < noOfDays; i++) {

                    wCond = gcl.getMarkovProbVector().getNextWeatherCond(wCond);

                    {
                        String dataSample = String.format("%d 1:%.2f 2:%.2f 3:%d 4:%d 5:%s", wCond.getIndex(), gcl.getLati(), gcl.getLongi(), gcl.getElv(), sampleDate.getDayOfYear(), 0);
                        bw.write(dataSample);
                        bw.newLine();
                    }

                    for (SensorType st : SensorType.values()) {
                        Sensor sensor = sensorCollection.get(st);
                        Double temperature = sensor.getSensorData(wCond);
                        String dataSample = String.format("%.2f 1:%.2f 2:%.2f 3:%d 4:%d 5:%d", temperature, gcl.getLati(), gcl.getLongi(), gcl.getElv(), sampleDate.getDayOfYear(), st.getSensorId());
                        bw.write(dataSample);
                        bw.newLine();
                    }
                    sampleDate = sampleDate.plusDays(1);
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
            throw new Exception("Failed to generate training data");
        }

    }

    /**
     * Generate simulation data just using Markov chain and probability distribution
     *
     * @throws Exception
     */
    public void generateSimulationData() throws Exception {
        String datapath = "target/tmp/" + RUNID + "/simulation.dat";
        File fs = new File("target/tmp/" + RUNID);
        if (!fs.exists()) fs.mkdirs();

        int locationNo = 0;
        try (BufferedWriter bw = new BufferedWriter(new FileWriter(datapath, false))) {
            for (GeoLocation gcl : sampleLocations) {
                //select a random starting weather condition for the geo location
                WeatherCondition wCond = WeatherCondition.LOOKUP.get(new Random().nextInt(3));
                LocalDateTime sampleDate = LocalDateTime.of(startDate.getYear(), startDate.getMonth(), startDate.getDayOfMonth(), startDate.getHour(), startDate.getMinute(), startDate.getSecond());

                Map<SensorType, Double> sensorObservation = new HashMap<>();
                for (int i = 0; i < noOfDays; i++) {

                    wCond = gcl.getMarkovProbVector().getNextWeatherCond(wCond);
                    for (SensorType st : SensorType.values()) {
                        Sensor sensor = sensorCollection.get(st);
                        Double observation = sensor.getSensorData(wCond);
                        sensorObservation.put(st, observation);
                    }
                    String strSampleDate = sampleDate.format(DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss'Z'"));
                    WeatherCondition wc = WeatherCondition.LOOKUP.get((int) Math.round(wCond.getIndex()));
                    String sampleData = String.format("%s|%.2f,%.2f,%d|%s|%s|%.1f|%.1f|%d", "LOCATION-" + locationNo, gcl.getLati(), gcl.getLongi(), gcl.getElv(), strSampleDate, wc.getCondName(),
                            sensorObservation.get(SensorType.TEMPSENSOR),
                            sensorObservation.get(SensorType.PRESSURESENSOR),
                            (int) Math.round(sensorObservation.get(SensorType.HUMIDSENSOR)));

                    bw.write(sampleData);
                    bw.newLine();

                    sampleDate = sampleDate.plusDays(1);
                }
                locationNo++;
            }
        } catch (Exception e) {
            e.printStackTrace();
            throw new Exception("Failed to generate simulation data");
        }
    }


}

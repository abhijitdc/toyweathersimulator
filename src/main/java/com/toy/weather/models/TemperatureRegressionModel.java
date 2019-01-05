package com.toy.weather.models;

import org.apache.spark.SparkConf;
import org.apache.spark.api.java.JavaPairRDD;
import org.apache.spark.api.java.JavaRDD;
import org.apache.spark.api.java.JavaSparkContext;
import org.apache.spark.mllib.linalg.Vector;
import org.apache.spark.mllib.linalg.Vectors;
import org.apache.spark.mllib.regression.LabeledPoint;
import org.apache.spark.mllib.tree.RandomForest;
import org.apache.spark.mllib.tree.model.RandomForestModel;
import org.apache.spark.mllib.util.MLUtils;
import scala.Tuple2;

import java.util.HashMap;
import java.util.Map;

/**
 * Created by abhijitdc on 1/5/19.
 */
public class TemperatureRegressionModel {
    public static void main(String[] args) {
        // $example on$
        SparkConf sparkConf = new SparkConf().setMaster("local[*]").setAppName("JavaRandomForestRegressionExample");
        JavaSparkContext jsc = new JavaSparkContext(sparkConf);
        // Load and parse the data file.
        String datapath = "src/main/resources/training.dat";

//        JavaRDD<String> allData = jsc.textFile(datapath);
//        JavaRDD<String> filterData = allData.filter(line -> line.endsWith("TEMPERATURE"));


        JavaRDD<LabeledPoint> allData = MLUtils.loadLibSVMFile(jsc.sc(), datapath).toJavaRDD();
        JavaRDD<LabeledPoint> data = allData.filter(l->l.features().toArray()[4]==1.0);
        data.take(10).forEach(l -> System.out.println(l.features().toArray()[3]));

        // Split the data into training and test sets (30% held out for testing)
        JavaRDD<LabeledPoint>[] splits = data.randomSplit(new double[]{0.7, 0.3});
        JavaRDD<LabeledPoint> trainingData = splits[0];
        JavaRDD<LabeledPoint> testData = splits[1];

        // Set parameters.
        // Empty categoricalFeaturesInfo indicates all features are continuous.
        Map<Integer, Integer> categoricalFeaturesInfo = new HashMap<>();
        int numTrees = 5; // Use more in practice.
        String featureSubsetStrategy = "auto"; // Let the algorithm choose.
        String impurity = "variance";
        int maxDepth = 4;
        int maxBins = 32;
        int seed = 12345;
        // Train a RandomForest model.
        RandomForestModel model = RandomForest.trainRegressor(trainingData,
                categoricalFeaturesInfo, numTrees, featureSubsetStrategy, impurity, maxDepth, maxBins, seed);

        // Evaluate model on test instances and compute test error
        JavaPairRDD<Double, Double> predictionAndLabel =
                testData.mapToPair(p -> new Tuple2<>(model.predict(p.features()), p.label()));
        double testMSE = predictionAndLabel.mapToDouble(pl -> {
            double diff = pl._1() - pl._2();
            return diff * diff;
        }).mean();
        System.out.println("Test Mean Squared Error: " + testMSE);
        System.out.println("Learned regression forest model:\n" + model.toDebugString());

        // Save and load model
        model.save(jsc.sc(), "target/tmp/myRandomForestRegressionModel");
        RandomForestModel sameModel = RandomForestModel.load(jsc.sc(),
                "target/tmp/myRandomForestRegressionModel");
        // $example off$
        double val = sameModel.predict( Vectors.dense(34.21,12.31,54.0,65.0,1.0));
        System.out.println("Predic >> " + val);
        jsc.stop();
    }


}

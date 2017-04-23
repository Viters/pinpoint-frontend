package com.pinpoint.beacons;

import android.content.Context;
import android.os.Looper;
import android.widget.Toast;

import com.facebook.react.bridge.Arguments;
import com.facebook.react.bridge.Callback;
import com.facebook.react.bridge.ReactApplicationContext;
import com.facebook.react.bridge.ReactContextBaseJavaModule;
import com.facebook.react.bridge.ReactMethod;
import com.facebook.react.bridge.WritableArray;
import com.facebook.react.bridge.WritableMap;
import com.kontakt.sdk.android.ble.configuration.ActivityCheckConfiguration;
import com.kontakt.sdk.android.ble.configuration.ScanMode;
import com.kontakt.sdk.android.ble.configuration.ScanPeriod;
import com.kontakt.sdk.android.ble.connection.OnServiceReadyListener;
import com.kontakt.sdk.android.ble.filter.ibeacon.IBeaconFilter;
import com.kontakt.sdk.android.ble.manager.ProximityManager;
import com.kontakt.sdk.android.ble.manager.ProximityManagerFactory;
import com.kontakt.sdk.android.ble.manager.listeners.IBeaconListener;
import com.kontakt.sdk.android.ble.manager.listeners.simple.SimpleIBeaconListener;
import com.kontakt.sdk.android.common.KontaktSDK;
import com.kontakt.sdk.android.common.profile.IBeaconDevice;
import com.kontakt.sdk.android.common.profile.IBeaconRegion;

import walker.blue.tri.lib.Trilateration;

import org.apache.commons.math3.fitting.leastsquares.LeastSquaresOptimizer;
import org.apache.commons.math3.fitting.leastsquares.LevenbergMarquardtOptimizer;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.PriorityQueue;
import java.util.Queue;
import java.util.concurrent.TimeUnit;

public class BeaconModule extends ReactContextBaseJavaModule {
    private KontaktSDK kontaktSDK;
    private ProximityManager proximityManager;
    private LinkedHashMap<String, Double> dataFromBeacons = new LinkedHashMap<>();

    private final static ArrayList<String> ALL_ID = new ArrayList<>(Arrays.asList(new String[]{
            "wsuT", "hLg1", "V2eV", "WUNK", "Q91y", "SOT9", "zwur", "XxjO"
    }));

    private static double[] position = new double[] {};

    private final static HashMap<String, Beacon> BEACONS = new HashMap<String, Beacon>() {{
        put("XxjO", new Beacon("XxjO", 0, 4.5));
        put("Q91y", new Beacon("Q91y", 2.5, 2.5));
        put("SOT9", new Beacon("SOT9", 0, 0));
    }};

    public BeaconModule(ReactApplicationContext reactContext) {
        super(reactContext);

        kontaktSDK = KontaktSDK.initialize("LTpGvyLTihfFVZObPIEmbQciVQgLXabr");
        Looper.prepare();
        proximityManager = ProximityManagerFactory.create(reactContext);
        proximityManager.configuration()
                .activityCheckConfiguration(ActivityCheckConfiguration.create(10000, 3000))
                .deviceUpdateCallbackInterval(1000)
                .scanMode(ScanMode.BALANCED)
                .scanPeriod(ScanPeriod.RANGING);
        proximityManager.setIBeaconListener(createIBeaconListener());

        final ArrayList<String> TEST_ID = new ArrayList<>(Arrays.asList(new String[]{
                "Q91y", "SOT9", "XxjO"
        }));

        IBeaconFilter customIBeaconFilter = new IBeaconFilter() {
            @Override
            public boolean apply(IBeaconDevice iBeaconDevice) {
                // So here we set the max distance from a beacon to 1m
                return TEST_ID.contains(iBeaconDevice.getUniqueId());
            }
        };

        proximityManager.filters().iBeaconFilter(customIBeaconFilter);
    }

    @Override
    public String getName() {
        return "BeaconModule";
    }

    @ReactMethod
    public void start() {
        proximityManager.connect(new OnServiceReadyListener() {
            @Override
            public void onServiceReady() {
                proximityManager.startScanning();
            }
        });
    }

    @ReactMethod
    public void stop() {
        proximityManager.stopScanning();
    }

    @ReactMethod
    public void getPosition(Callback get) {
        WritableMap data = Arguments.createMap();
        if (position != null && position.length >= 2) {
            data.putDouble("x", position[0]);
            data.putDouble("y", position[1]);
            data.putString("error", "false");
        }
        else {
            data.putDouble("x", 0);
            data.putDouble("y", 0);
            data.putString("error", "true");
        }
        get.invoke(data);
    }

    @ReactMethod
    public void updatePosition() {
        checkIfShouldCalculate();
    }

    private IBeaconListener createIBeaconListener() {

        return new SimpleIBeaconListener() {
            @Override
            public void onIBeaconDiscovered(IBeaconDevice ibeacon, IBeaconRegion region) {
                BEACONS.get(ibeacon.getUniqueId()).measurements.add(ibeacon.getDistance());
            }

            @Override
            public void onIBeaconsUpdated(List<IBeaconDevice> ibeacons, IBeaconRegion region) {
                for (IBeaconDevice ibeacon : ibeacons) {
                    BEACONS.get(ibeacon.getUniqueId()).measurements.add(ibeacon.getDistance());
                }
            }

            @Override
            public void onIBeaconLost(IBeaconDevice ibeacon, IBeaconRegion region) {
                BEACONS.get(ibeacon.getUniqueId()).measurements.add(ibeacon.getDistance());
            }
        };
    }

    static void checkIfShouldCalculate() {
        int i = 0;
        for (String s : BEACONS.keySet()) {
            if (BEACONS.get(s).measurements.size() >= 10) {
                i++;
            }
            if (i >= 3) {
                startCalculations();
                break;
            }
        }
    }

    private static void startCalculations() {
        ArrayList<double[]> positions = new ArrayList<>();
        ArrayList<Double> distances = new ArrayList<>();
        for (String s : BEACONS.keySet()) {
            if (BEACONS.get(s).measurements.size() < 1) {
                BEACONS.get(s).measurements.clear();
                continue;
            }
            positions.add(new double[]{BEACONS.get(s).x, BEACONS.get(s).y});
            Collections.sort(BEACONS.get(s).measurements);
            distances.add(median(BEACONS.get(s).measurements));
            BEACONS.get(s).measurements.clear();
        }

        double[][] positionsArray = new double[positions.size()][];
        for (int i = 0; i < positionsArray.length; i++) {
            positionsArray[i] = positions.get(i);
        }

        double[] distancesArray = new double[distances.size()];
        for (int i = 0; i < distancesArray.length; i++) {
            distancesArray[i] = distances.get(i);
        }

        NonLinearLeastSquaresSolver solver = new NonLinearLeastSquaresSolver(new TrilaterationFunction(positionsArray, distancesArray), new LevenbergMarquardtOptimizer());
        LeastSquaresOptimizer.Optimum optimum = solver.solve();

        position = optimum.getPoint().toArray();
    }

    private static double median(LinkedList<Double> measurements) {
        int middle = measurements.size() / 2;
        if (measurements.size() % 2 == 1) {
            return measurements.get(middle);
        } else {
            return (measurements.get(middle - 1) + measurements.get(middle)) / 2.0;
        }
    }

    private static class Beacon {
        final String id;
        final double x;
        final double y;
        final LinkedList<Double> measurements = new LinkedList<>();

        Beacon(String id, double x, double y) {
            this.id = id;
            this.x = x;
            this.y = y;
        }
    }
}

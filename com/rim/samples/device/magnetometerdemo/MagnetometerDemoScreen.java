/*
 * MagnetometerDemoScreen.java
 *
 * Copyright � 1998-2011 Research In Motion Limited
 * 
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *
 * Note: For the sake of simplicity, this sample application may not leverage
 * resource bundles and resource strings.  However, it is STRONGLY recommended
 * that application developers make use of the localization features available
 * within the BlackBerry development platform to ensure a seamless application
 * experience across a variety of languages and geographies.  For more information
 * on localizing your application, please refer to the BlackBerry Java Development
 * Environment Development Guide associated with this release.
 */

package com.rim.samples.device.magnetometerdemo;

import javax.microedition.location.Location;
import javax.microedition.location.LocationException;
import javax.microedition.location.LocationProvider;
import javax.microedition.location.QualifiedCoordinates;

import net.rim.device.api.gps.BlackBerryCriteria;
import net.rim.device.api.gps.BlackBerryLocationProvider;
import net.rim.device.api.location.GeomagneticField;
import net.rim.device.api.system.Application;
import net.rim.device.api.system.DeviceInfo;
import net.rim.device.api.system.MagnetometerChannelConfig;
import net.rim.device.api.system.MagnetometerData;
import net.rim.device.api.system.MagnetometerListener;
import net.rim.device.api.system.MagnetometerSensor;
import net.rim.device.api.system.MagnetometerSensor.Channel;
import net.rim.device.api.ui.Field;
import net.rim.device.api.ui.FieldChangeListener;
import net.rim.device.api.ui.component.ButtonField;
import net.rim.device.api.ui.component.LabelField;
import net.rim.device.api.ui.component.SeparatorField;
import net.rim.device.api.ui.component.TextField;
import net.rim.device.api.ui.container.HorizontalFieldManager;
import net.rim.device.api.ui.container.MainScreen;
import net.rim.device.api.ui.container.VerticalFieldManager;

/**
 * GUI screen for the Magnetometer Demo application
 */
public final class MagnetometerDemoScreen extends MainScreen implements
        FieldChangeListener, MagnetometerListener {
    private final TextField _headingField;
    private final TextField _angleField;
    private final TextField _strengthField;
    private final TextField _calibrationQualityField;
    private TextField _declinationField;
    private TextField _latitude;
    private TextField _longitude;
    private TextField _altitude;
    private TextField _locationInfo;

    private final TextField _snapshotAngleField;
    private final TextField _snapshotStrengthField;
    private final TextField _snapshotQualityField;
    private final TextField _snapshotHeadingField;
    private TextField _snapshotDeclinationField;

    private final ButtonField _snapshotButton;
    private final ButtonField _calibrateButton;

    private HorizontalFieldManager _locationManager;
    private final VerticalFieldManager _streamingManager;
    private final VerticalFieldManager _snapshotManager;

    private final Channel _magnetometerChannel;

    private GeomagneticField _geoField;
    private final Application _app;

    /**
     * Creates a new MagetometerDemoScreen object
     * 
     * @param app
     *            The application associated with this screen
     */
    public MagnetometerDemoScreen(final Application app) {
        // Initialize UI

        setTitle("Magnetometer Demo");

        // Cache if the device is a simulator or not
        final boolean isSim = DeviceInfo.isSimulator();

        // Add fields for displaying real time data
        _streamingManager = new VerticalFieldManager();
        _headingField = new TextField("Heading: ", "");
        _strengthField = new TextField("Field strength: ", "");
        _angleField = new TextField("Angle: ", "");
        _calibrationQualityField = new TextField("Calibration quality: ", "");
        _streamingManager.add(_headingField);
        _streamingManager.add(_angleField);
        _streamingManager.add(_strengthField);
        _streamingManager.add(_calibrationQualityField);
        add(_streamingManager);

        // Add HorizontalFieldManager for buttons
        final HorizontalFieldManager buttonManager =
                new HorizontalFieldManager(FIELD_HCENTER);
        _snapshotButton = new ButtonField("Snapshot");
        _calibrateButton = new ButtonField("Calibrate");
        _snapshotButton.setChangeListener(this);
        _calibrateButton.setChangeListener(this);
        buttonManager.add(_snapshotButton);
        buttonManager.add(_calibrateButton);
        add(buttonManager);

        add(new SeparatorField());

        // Add fields for displaying snapshot data
        _snapshotManager = new VerticalFieldManager();
        _snapshotHeadingField = new TextField("Snapshot heading: ", "");
        _snapshotAngleField = new TextField("Snapshot angle: ", "");
        _snapshotStrengthField = new TextField("Snapshot field strength: ", "");
        _snapshotQualityField =
                new TextField("Snapshot calibration quality: ", "");
        _snapshotManager.add(_snapshotHeadingField);
        _snapshotManager.add(_snapshotAngleField);
        _snapshotManager.add(_snapshotStrengthField);
        _snapshotManager.add(_snapshotQualityField);
        add(_snapshotManager);

        add(new SeparatorField());

        // The magnetometer channel will be opened with a sampling
        // frequency of 10 hertz and will be active only when the app
        // is in the foreground. This is the default configuration
        // that would be set automatically if the no arg constructor
        // for MagnetometerChannelConfig was used.
        final MagnetometerChannelConfig mcc =
                new MagnetometerChannelConfig(10, true, false);

        // Open up the magnetometer channel for reading data and
        // set this class as a MagnetometerListener.
        _magnetometerChannel = MagnetometerSensor.openChannel(app, mcc);
        _magnetometerChannel.addMagnetometerListener(this);

        // Cache the application for use later
        _app = app;

        // Start looking for the device's location and initialize
        // the GeomagneticField if on a real device.
        if (!isSim) {
            // Add the declination fields only if on a real device
            _declinationField = new TextField("Declination: ", "");
            _streamingManager.add(_declinationField);

            _snapshotDeclinationField =
                    new TextField("Snapshot declination: ", "");
            _snapshotManager.add(_snapshotDeclinationField);

            // Add field for displaying location status
            _locationInfo = new TextField("Location status: ", "");
            add(_locationInfo);

            // Add HorizontalFieldManager for the location information
            _locationManager = new HorizontalFieldManager(FIELD_HCENTER);
            add(_locationManager);

            _latitude = new TextField("Latitude : ", "");
            _longitude = new TextField("Longitude : ", "");
            _altitude = new TextField("Altitude : ", "");

            // Initialize the GeomagneticField in a non-event thread
            final Thread initializer = new Thread(new Runnable() {
                public void run() {
                    getLocation();
                }
            });
            initializer.start();
        }
    }

    /**
     * Displays magnetometer data on the screen
     * 
     * @param angle
     *            Clockwise angle from magnetic north
     * @param declination
     *            Difference between magnetic north and true north at the
     *            device's snapshot location
     * @param strength
     *            Strength of the magnetic field
     * @param quality
     *            Quality of the calibration
     * @param heading
     *            snapshot heading based on a 16 point compass rose
     */
    public void printStreaming(final float angle, final float declination,
            final float strength, final int quality, final String heading) {
        _angleField.setText((int) angle + "�");
        if (!Float.isNaN(declination)) {
            // If the declination is valid, print it to the screen.
            // It will become valid once the GeomagneticField has been
            // initialized.
            _declinationField.setText((int) declination + "�");
        }
        _strengthField.setText(Float.toString(strength));
        _calibrationQualityField.setText(Integer.toString(quality));
        _headingField.setText(heading);
    }

    /**
     * Displays a snapshot of magnetometer data
     * 
     * @param angle
     *            Snapshot of the clockwise angle from magnetic north
     * @param declination
     *            Snapshot of the difference between magnetic north and true
     *            north at teh device's snapshot location
     * @param strength
     *            Snapshot of the strength of the magnetic field
     * @param quality
     *            Snapshot of the quality of the calibration
     * @param heading
     *            Snapshot of the heading based on a 16 point compass rose
     */
    public void printSnapshot(final float angle, final float declination,
            final float strength, final int quality, final String heading) {
        _snapshotAngleField.setText((int) angle + "�");
        if (!Float.isNaN(declination)) {
            // If the declination is valid, print it to the screen.
            // It will become valid once the GeomagneticField has been
            // initialized.
            _snapshotDeclinationField.setText((int) declination + "�");
        }
        _snapshotStrengthField.setText(Float.toString(strength));
        _snapshotQualityField.setText(Integer.toString(quality));
        _snapshotHeadingField.setText(heading);
    }

    /**
     * Attempt to get the devices current location. If a valid location is found
     * then the GeomagneticField will be initialized. Otherwise, the declination
     * will be NaN.
     */
    public void getLocation() {
        try {
            final BlackBerryCriteria criteria = new BlackBerryCriteria();
            criteria.enableGeolocationWithGPS();

            _locationInfo.setText("Searching for location...");
            final BlackBerryLocationProvider bbProvider =
                    (BlackBerryLocationProvider) LocationProvider
                            .getInstance(criteria);

            final Location loc = bbProvider.getLocation(-1);

            synchronized (_app.getAppEventLock()) {
                _locationInfo.setText("Location has been found");
            }

            if (loc.isValid()) {
                final QualifiedCoordinates coordinates =
                        loc.getQualifiedCoordinates();
                final float altitude = coordinates.getAltitude();
                final double latitude = coordinates.getLatitude();
                final double longitude = coordinates.getLongitude();

                // Set the text for the location information fields
                _altitude.setText(Float.toString(altitude));
                _latitude.setText(Double.toString(latitude));
                _longitude.setText(Double.toString(longitude));

                synchronized (_app.getAppEventLock()) {
                    // Add the location information fields to the manager
                    _locationManager.add(_altitude);
                    _locationManager.add(_latitude);
                    _locationManager.add(_longitude);
                }

                // Initialize the GeomangeticField with the device's
                // latitude, longitude, and altitude.
                _geoField =
                        new GeomagneticField(latitude, longitude,
                                (int) altitude);
            }
        } catch (final InterruptedException iex) {
            synchronized (_app.getAppEventLock()) {
                add(new LabelField("Interrupted : " + iex.toString()));
            }
        } catch (final LocationException lex) {
            synchronized (_app.getAppEventLock()) {
                add(new LabelField("Location Error: " + lex.toString()));
            }
        }
    }

    /**
     * Retrieves the string version of the heading based on a 16 point compass
     * rose.
     * 
     * @param headingCode
     *            One of the integer constants representing a heading from the
     *            list in the MagnetometerData class
     * @return String version of the heading (e.g. North, North North East, etc)
     */
    public String getHeadingName(final int headingCode) {
        String headingName;

        switch (headingCode) {
        case MagnetometerData.MAGNETOMETER_HEADING_EAST:
            headingName = "East";
            break;
        case MagnetometerData.MAGNETOMETER_HEADING_EAST_NORTH_EAST:
            headingName = "East North East";
            break;
        case MagnetometerData.MAGNETOMETER_HEADING_EAST_SOUTH_EAST:
            headingName = "East South East";
            break;
        case MagnetometerData.MAGNETOMETER_HEADING_NORTH:
            headingName = "North";
            break;
        case MagnetometerData.MAGNETOMETER_HEADING_NORTH_EAST:
            headingName = "North East";
            break;
        case MagnetometerData.MAGNETOMETER_HEADING_NORTH_NORTH_EAST:
            headingName = "North North East";
            break;
        case MagnetometerData.MAGNETOMETER_HEADING_NORTH_NORTH_WEST:
            headingName = "North North West";
            break;
        case MagnetometerData.MAGNETOMETER_HEADING_NORTH_WEST:
            headingName = "North West";
            break;
        case MagnetometerData.MAGNETOMETER_HEADING_SOUTH:
            headingName = "South";
            break;
        case MagnetometerData.MAGNETOMETER_HEADING_SOUTH_EAST:
            headingName = "South East";
            break;
        case MagnetometerData.MAGNETOMETER_HEADING_SOUTH_SOUTH_EAST:
            headingName = "South South East";
            break;
        case MagnetometerData.MAGNETOMETER_HEADING_SOUTH_SOUTH_WEST:
            headingName = "South South West";
            break;
        case MagnetometerData.MAGNETOMETER_HEADING_SOUTH_WEST:
            headingName = "South West";
            break;
        case MagnetometerData.MAGNETOMETER_HEADING_WEST:
            headingName = "West";
            break;
        case MagnetometerData.MAGNETOMETER_HEADING_WEST_NORTH_WEST:
            headingName = "West North West";
            break;
        case MagnetometerData.MAGNETOMETER_HEADING_WEST_SOUTH_WEST:
            headingName = "West South West";
            break;
        default:
            headingName = Integer.toString(headingCode);
            break;
        }

        return headingName;
    }

    /**
     * @see net.rim.device.api.ui.container.MainScreen#onSavePrompt()
     */
    protected boolean onSavePrompt() {
        // Prevent the save dialog from being displayed
        return true;
    }

    /**
     * @see net.rim.device.api.ui.Screen#close()
     */
    public void close() {
        // Close the magnetometer channel and deregister listener
        _magnetometerChannel.close();
        _magnetometerChannel.removeMagnetometerListener(this);

        super.close();
    }

    /**
     * @see net.rim.device.api.ui.FieldChangeListener#fieldChanged(Field, int)
     */
    public void fieldChanged(final Field field, final int context) {
        if (field == _snapshotButton) {
            doSnapshot();
        } else if (field == _calibrateButton) {
            calibrate();
        }
    }

    /**
     * @see net.rim.device.api.system.MagnetometerListener#onData(MagnetometerData)
     */
    public void onData(final MagnetometerData magData) {
        // Check for calibration
        if (_magnetometerChannel.isCalibrating()) {
            // Stop calibrating when the desired quality achieved
            if (magData.getCalibrationQuality() == MagnetometerData.MAGNETOMETER_QUALITY_HIGH) {
                try {
                    _magnetometerChannel.stopCalibration();
                } catch (final Throwable t) {
                }
            }
        }

        float declination;

        try {
            // Get the declination from the GeomagneticField
            declination = _geoField.getDeclination();
        } catch (final Exception e) {
            // Indicate that we don't have a valid declination value yet
            declination = Float.NaN;
        }

        // Capture the new data
        final float direction = magData.getDirectionTop();
        final float strength = magData.getFieldStrength();
        final int quality = magData.getCalibrationQuality();
        final String heading =
                getHeadingName(MagnetometerData.getHeading(direction));

        // Print the data on the display
        printStreaming(direction, declination, strength, quality, heading);
    }

    /**
     * Retrieves a snapshot of data from the magnetometer and prints it to the
     * display.
     */
    public void doSnapshot() {
        float direction;
        float strength;
        int quality;

        // Get data from the magnetometer channel
        final MagnetometerData data = _magnetometerChannel.getData();

        // Synchronize on the data so that it doesn't change while we are
        // retrieving it
        synchronized (data) {
            direction = data.getDirectionTop();
            strength = data.getFieldStrength();
            quality = data.getCalibrationQuality();
        }

        float declination;

        try {
            // Get the declination from the GeomagneticField
            declination = _geoField.getDeclination();
        } catch (final Exception e) {
            // Indicate that we don't have a valid declination value yet
            declination = Float.NaN;
        }

        // Convert the heading from an int value into a String heading
        final String heading =
                getHeadingName(MagnetometerData.getHeading(direction));
        printSnapshot(direction, declination, strength, quality, heading);
    }

    /**
     * Starts the magnetometer calibration process
     */
    public void calibrate() {
        try {
            _magnetometerChannel.startCalibration();
        } catch (final Exception e) {
            MagnetometerDemo.errorDialog("Error calibrating: " + e.toString());
        }
    }
}

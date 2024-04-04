package org.openremote.model.reschool;

public class DeviceCharacteristic {

    // ID of the setting such as 'solar' or 'carCharger' etc.
    // Check database or existing attributes for the ones used currently.
    protected String id;

    // ANY value that should be saved into the characteristic, depends on the use case.
    // Similar if it would be an attribute that allows any value :)
    protected String value;

    // Brand related to the device.
    // Mostly used for frontend purposes to redirect to 3rd party apps.
    protected String brand;

    // Specifies the amount of Watt a user can save, when this characteristic is not active.
    // Uses a default value provided by the frontend, but can be tweaked at any time.
    protected Integer wattsSaved;

    // Controllable value by algorithms / rules, whether this characteristic is 'active'.
    // If active is false (and shown is true), it will be shown to the user as; "Hey, turning this off will save energy"
    // If active is TRUE, it will be hidden to the user at all times.
    protected boolean active;

    // Whether 'applicable' to the Meter; this can be controlled by the user in the UI.
    // Tips related to this characteristic will be shown when both this variable, and 'active' are TRUE.
    protected boolean shown;


    /* -------------------------------------------------------------------- */

    // constructor
    public DeviceCharacteristic() {

    }

    public DeviceCharacteristic(String id, String value, boolean active, boolean shown) {
        this.id = id;
        this.value = value;
        this.active = active;
        this.shown = shown;
    }

    public DeviceCharacteristic(String id, String value, String brand, Integer wattsSaved, boolean active, boolean shown) {
        this.id = id;
        this.value = value;
        this.brand = brand;
        this.wattsSaved = wattsSaved;
        this.active = active;
        this.shown = shown;
    }


    public String getId() {
        return id;
    }

    public DeviceCharacteristic setId(String id) {
        this.id = id;
        return this;
    }

    public String getValue() {
        return value;
    }

    public DeviceCharacteristic setValue(String value) {
        this.value = value;
        return this;
    }

    public String getBrand() {
        return brand;
    }

    public DeviceCharacteristic setBrand(String brand) {
        this.brand = brand;
        return this;
    }

    public Integer getWattsSaved() {
        return wattsSaved;
    }

    public DeviceCharacteristic setWattsSaved(Integer wattsSaved) {
        this.wattsSaved = wattsSaved;
        return this;
    }

    public boolean isActive() {
        return active;
    }

    public DeviceCharacteristic setActive(boolean active) {
        this.active = active;
        return this;
    }

    public boolean isShown() {
        return shown;
    }

    public DeviceCharacteristic setShown(boolean shown) {
        this.shown = shown;
        return this;
    }
}

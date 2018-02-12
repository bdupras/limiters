package com.duprasville.limiters.vizualization;

import com.duprasville.limiters.comms.Message;
import com.duprasville.limiters.treefill.Detect;
import com.duprasville.limiters.treefill.Full;
import com.duprasville.limiters.treefill.WindowFull;

import java.util.HashMap;
import java.util.Map;

public class MessageAppearance {
    static final Map<Class<? extends Message>, Double> colors = new HashMap<>();
    static {
        colors.put(Detect.class, lime());
        colors.put(Full.class, orange());
        colors.put(WindowFull.class, red());
    }

    public static double relColor(Message message) {
        Class<? extends Message> clazz = message.getClass();
        return colors.getOrDefault(clazz, white());
    }

    /**
     * Returns the numerical value corresponding to the color black.
     */
    public static double black() {
        return 0.0d;
    }

    /**
     * Returns the numerical value corresponding to the color gray.
     */
    public static double gray() {
        return 5.0d;
    }

    /**
     * Returns the numerical value corresponding to the color white.
     */
    public static double white() {
        return 9.99d;
    }

    /**
     * Returns the numerical value corresponding to the color red.
     */
    public static double red() {
        return 15.0d;
    }

    /**
     * Returns the numerical value corresponding to the color orange.
     */
    public static double orange() {
        return 25.0d;
    }

    /**
     * Returns the numerical value corresponding to the color brown.
     */
    public static double brown() {
        return 35.0d;
    }

    /**
     * Returns the numerical value corresponding to the color yellow.
     */
    public static double yellow() {
        return 45.0d;
    }

    /**
     * Returns the numerical value corresponding to the color green.
     */
    public static double green() {
        return 55.0d;
    }

    /**
     * Returns the numerical value corresponding to the color lime.
     */
    public static double lime() {
        return 65.0d;
    }

    /**
     * Returns the numerical value corresponding to the color turquoise.
     */
    public static double turquoise() {
        return 75.0d;
    }

    /**
     * Returns the numerical value corresponding to the color cyan.
     */
    public static double cyan() {
        return 85.0d;
    }

    /**
     * Returns the numerical value corresponding to the color sky.
     */
    public static double sky() {
        return 95.0d;
    }

    /**
     * Returns the numerical value corresponding to the color blue.
     */
    public static double blue() {
        return 105.0d;
    }

    /**
     * Returns the numerical value corresponding to the color violet.
     */
    public static double violet() {
        return 115.0d;
    }

    /**
     * Returns the numerical value corresponding to the color magenta.
     */
    public static double magenta() {
        return 125.0d;
    }

    /**
     * Returns the numerical value corresponding to the color pink.
     */
    public static double pink() {
        return 135.0d;
    }

}

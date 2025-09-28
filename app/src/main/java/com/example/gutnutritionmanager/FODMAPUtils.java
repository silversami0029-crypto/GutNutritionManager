package com.example.gutnutritionmanager;

import android.graphics.Color;

public class FODMAPUtils {
    public static int getColorForStatus(String status) {
        if (status == null) return Color.GRAY;

        switch (status) {
            case "LOW":
                return Color.GREEN;
            case "MODERATE":
                return Color.YELLOW;
            case "HIGH":
                return Color.RED;
            default:
                return Color.GRAY;
        }
    }

    public static String getDescriptionForStatus(String status) {
        if (status == null) return "Unknown FODMAP";

        switch (status) {
            case "LOW":
                return "Low FODMAP";
            case "MODERATE":
                return "Moderate FODMAP";
            case "HIGH":
                return "High FODMAP";
            default:
                return "Unknown FODMAP";
        }
    }
}

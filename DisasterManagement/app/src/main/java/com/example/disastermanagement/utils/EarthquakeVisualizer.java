package com.example.disastermanagement.utils;

import android.content.Context;
import android.graphics.Color;

import com.example.disastermanagement.models.EarthquakeData;
import com.github.mikephil.charting.charts.BarChart;
import com.github.mikephil.charting.charts.LineChart;
import com.github.mikephil.charting.charts.PieChart;
import com.github.mikephil.charting.components.Description;
import com.github.mikephil.charting.components.XAxis;
import com.github.mikephil.charting.data.BarData;
import com.github.mikephil.charting.data.BarDataSet;
import com.github.mikephil.charting.data.BarEntry;
import com.github.mikephil.charting.data.Entry;
import com.github.mikephil.charting.data.LineData;
import com.github.mikephil.charting.data.LineDataSet;
import com.github.mikephil.charting.data.PieData;
import com.github.mikephil.charting.data.PieDataSet;
import com.github.mikephil.charting.data.PieEntry;
import com.github.mikephil.charting.formatter.IndexAxisValueFormatter;
import com.github.mikephil.charting.formatter.ValueFormatter;
import com.github.mikephil.charting.utils.ColorTemplate;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/**
 * Utility class to create data visualizations for earthquake data
 */
public class EarthquakeVisualizer {

    /**
     * Generate a magnitude distribution chart showing the count of earthquakes by magnitude range
     */
    public static void setupMagnitudeDistributionChart(PieChart pieChart, Map<String, Integer> magnitudeDistribution) {
        ArrayList<PieEntry> entries = new ArrayList<>();
        
        // Add entries for each magnitude range
        if (magnitudeDistribution.containsKey("minor")) {
            entries.add(new PieEntry(magnitudeDistribution.get("minor"), "< 4.0"));
        }
        if (magnitudeDistribution.containsKey("light")) {
            entries.add(new PieEntry(magnitudeDistribution.get("light"), "4.0-4.9"));
        }
        if (magnitudeDistribution.containsKey("moderate")) {
            entries.add(new PieEntry(magnitudeDistribution.get("moderate"), "5.0-5.9"));
        }
        if (magnitudeDistribution.containsKey("strong")) {
            entries.add(new PieEntry(magnitudeDistribution.get("strong"), "6.0-6.9"));
        }
        if (magnitudeDistribution.containsKey("major")) {
            entries.add(new PieEntry(magnitudeDistribution.get("major"), "7.0-7.9"));
        }
        if (magnitudeDistribution.containsKey("great")) {
            entries.add(new PieEntry(magnitudeDistribution.get("great"), "8.0+"));
        }
        
        PieDataSet dataSet = new PieDataSet(entries, "Magnitude Distribution");
        dataSet.setColors(ColorTemplate.MATERIAL_COLORS);
        dataSet.setValueTextSize(12f);
        dataSet.setValueTextColor(Color.WHITE);
        
        PieData data = new PieData(dataSet);
        
        pieChart.setData(data);
        pieChart.getDescription().setEnabled(false);
        pieChart.setCenterText("Magnitude\nDistribution");
        pieChart.setCenterTextSize(14f);
        pieChart.setEntryLabelTextSize(12f);
        pieChart.animateY(1000);
        pieChart.invalidate();
    }
    
    /**
     * Generate a region distribution chart showing earthquake counts by region
     */
    public static void setupRegionDistributionChart(BarChart barChart, List<EarthquakeData> earthquakes) {
        // Count earthquakes by region
        Map<String, Integer> regionCounts = new HashMap<>();
        for (EarthquakeData earthquake : earthquakes) {
            String region = earthquake.getRegion();
            if (region != null && !region.isEmpty()) {
                if (regionCounts.containsKey(region)) {
                    regionCounts.put(region, regionCounts.get(region) + 1);
                } else {
                    regionCounts.put(region, 1);
                }
            }
        }
        
        // Convert to bar entries
        ArrayList<BarEntry> entries = new ArrayList<>();
        ArrayList<String> labels = new ArrayList<>();
        int index = 0;
        
        List<Map.Entry<String, Integer>> sortedEntries = new ArrayList<>(regionCounts.entrySet());
        Collections.sort(sortedEntries, (e1, e2) -> e2.getValue().compareTo(e1.getValue()));
        
        // Take top 5 regions if there are more
        int maxRegions = Math.min(sortedEntries.size(), 5);
        for (int i = 0; i < maxRegions; i++) {
            Map.Entry<String, Integer> entry = sortedEntries.get(i);
            entries.add(new BarEntry(index, entry.getValue()));
            labels.add(entry.getKey());
            index++;
        }
        
        BarDataSet dataSet = new BarDataSet(entries, "Earthquakes by Region");
        dataSet.setColors(ColorTemplate.MATERIAL_COLORS);
        
        BarData data = new BarData(dataSet);
        data.setBarWidth(0.6f);
        data.setValueTextSize(10f);
        
        barChart.setData(data);
        barChart.getDescription().setEnabled(false);
        barChart.getLegend().setEnabled(false);
        
        // Set X-axis labels
        XAxis xAxis = barChart.getXAxis();
        xAxis.setValueFormatter(new IndexAxisValueFormatter(labels));
        xAxis.setPosition(XAxis.XAxisPosition.BOTTOM);
        xAxis.setGranularity(1f);
        xAxis.setLabelRotationAngle(45);
        
        barChart.setFitBars(true);
        barChart.animateY(1000);
        barChart.invalidate();
    }
    
    /**
     * Generate a time series chart showing earthquake magnitudes over time
     */
    public static void setupTimeSeriesChart(LineChart lineChart, List<EarthquakeData> earthquakes) {
        // Sort earthquakes by time
        List<EarthquakeData> sortedEarthquakes = new ArrayList<>(earthquakes);
        Collections.sort(sortedEarthquakes, new Comparator<EarthquakeData>() {
            @Override
            public int compare(EarthquakeData eq1, EarthquakeData eq2) {
                return eq1.getOriginTime().compareTo(eq2.getOriginTime());
            }
        });
        
        ArrayList<Entry> entries = new ArrayList<>();
        ArrayList<String> labels = new ArrayList<>();
        SimpleDateFormat dateFormat = new SimpleDateFormat("MM/dd", Locale.US);
        
        for (int i = 0; i < sortedEarthquakes.size(); i++) {
            EarthquakeData earthquake = sortedEarthquakes.get(i);
            entries.add(new Entry(i, (float) earthquake.getMagnitude()));
            try {
                // Parse the ISO date format if it's available
                Date date = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.US)
                        .parse(earthquake.getOriginTime().split(" ")[0] + " " + 
                              earthquake.getOriginTime().split(" ")[1]);
                labels.add(dateFormat.format(date));
            } catch (Exception e) {
                labels.add("Day " + i);
            }
        }
        
        LineDataSet dataSet = new LineDataSet(entries, "Earthquake Magnitudes");
        dataSet.setColor(Color.BLUE);
        dataSet.setValueTextColor(Color.BLACK);
        dataSet.setValueTextSize(10f);
        dataSet.setLineWidth(2f);
        dataSet.setCircleColor(Color.RED);
        dataSet.setCircleRadius(4f);
        dataSet.setDrawValues(true);
        
        LineData data = new LineData(dataSet);
        
        // Set X-axis labels
        XAxis xAxis = lineChart.getXAxis();
        xAxis.setPosition(XAxis.XAxisPosition.BOTTOM);
        xAxis.setGranularity(1f);
        xAxis.setValueFormatter(new IndexAxisValueFormatter(labels));
        xAxis.setLabelRotationAngle(45);
        
        lineChart.setData(data);
        lineChart.getDescription().setEnabled(false);
        lineChart.animateX(1000);
        lineChart.invalidate();
    }
} 
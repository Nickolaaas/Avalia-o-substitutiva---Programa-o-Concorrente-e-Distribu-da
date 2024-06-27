package com.recuperacao.weather_fetcher;

import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.ArrayList;
import java.util.List;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.core.JsonProcessingException;

public class SingleThreadVersion {
    public static void main(String[] args) throws IOException, InterruptedException {
        List<Capital> capitals = loadCapitals(); // Load capital data from file
        List<Long> executionTimes = new ArrayList<>(); // List to store execution times

        for (int i = 0; i < 10; i++) { // 10 repetitions
            long startTime = System.currentTimeMillis();

            for (Capital capital : capitals) {
                String response = sendHttpRequest(capital.getLatitude(), capital.getLongitude());
                processResponse(response, capital);
            }

            long endTime = System.currentTimeMillis();
            long executionTime = endTime - startTime;
            executionTimes.add(executionTime); // Add execution time to the list
            System.out.println("Execution time for run " + (i + 1) + ": " + executionTime + " ms");
        }

        // Calculate and print the average, min, and max execution time
        long totalExecutionTime = 0;
        long minExecutionTime = Long.MAX_VALUE;
        long maxExecutionTime = Long.MIN_VALUE;
        
        for (long time : executionTimes) {
            totalExecutionTime += time;
            if (time < minExecutionTime) {
                minExecutionTime = time;
            }
            if (time > maxExecutionTime) {
                maxExecutionTime = time;
            }
        }
        
        double averageExecutionTime = (double) totalExecutionTime / executionTimes.size();
        System.out.println("Execution times: " + executionTimes);
        System.out.println("Average execution time: " + averageExecutionTime + " ms");
        System.out.println("Min execution time: " + minExecutionTime + " ms");
        System.out.println("Max execution time: " + maxExecutionTime + " ms");
    }

    private static String sendHttpRequest(double latitude, double longitude) throws IOException, InterruptedException {
        HttpClient client = HttpClient.newHttpClient();
        HttpRequest request = HttpRequest.newBuilder()
               .uri(URI.create("https://historical-forecast-api.open-meteo.com/v1/forecast?latitude=" + latitude + "&longitude=" + longitude + "&start_date=2024-01-01&end_date=2024-01-31&hourly=temperature_2m&daily=temperature_2m_max,temperature_2m_min&timezone=America%2FSao_Paulo"))
               .build();       
        HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());
        return response.body();
    }

    private static void processResponse(String response, Capital capital) {
        ObjectMapper mapper = new ObjectMapper();
        try {
            JsonNode root = mapper.readTree(response);
            System.out.println("API Response for " + capital.getName() + ": " + response);

            JsonNode daily = root.path("daily");
            if (daily.isMissingNode()) {
                System.out.println("Daily data is missing for " + capital.getName());
                return;
            }

            JsonNode maxTemperatures = daily.path("temperature_2m_max");
            JsonNode minTemperatures = daily.path("temperature_2m_min");
            if (maxTemperatures.isMissingNode() || minTemperatures.isMissingNode()) {
                System.out.println("Temperature data is missing for " + capital.getName());
                return;
            }

            System.out.println("Capital: " + capital.getName());

            for (int i = 0; i < maxTemperatures.size(); i++) {
                double maxTemp = maxTemperatures.get(i).asDouble();
                double minTemp = minTemperatures.get(i).asDouble();
                double avgTemp = (maxTemp + minTemp) / 2;

                System.out.println("Day " + (i + 1) + ":");
                System.out.println("Average temperature: " + avgTemp);
                System.out.println("Max temperature: " + maxTemp);
                System.out.println("Min temperature: " + minTemp);
                System.out.println();
            }

        } catch (JsonProcessingException e) {
            e.printStackTrace();
        }
    }

    private static List<Capital> loadCapitals() throws IOException {
        ObjectMapper mapper = new ObjectMapper();
        ClassLoader classLoader = SingleThreadVersion.class.getClassLoader();
        InputStream inputStream = classLoader.getResourceAsStream("coordinates.json");
        if (inputStream == null) {
            throw new IOException("File not found: coordinates.json");
        }
        JsonNode root = mapper.readTree(inputStream);
        List<Capital> capitals = new ArrayList<>();
        for (JsonNode capitalNode : root) {
            String name = capitalNode.get("name").asText();
            double latitude = capitalNode.get("latitude").asDouble();
            double longitude = capitalNode.get("longitude").asDouble();
            capitals.add(new Capital(name, latitude, longitude));
        }
        return capitals;
    }

    private static class Capital {
        private String name;
        private double latitude;
        private double longitude;

        public Capital(String name, double latitude, double longitude) {
            this.name = name;
            this.latitude = latitude;
            this.longitude = longitude;
        }

        public String getName() {
            return name;
        }

        public double getLatitude() {
            return latitude;
        }

        public double getLongitude() {
            return longitude;
        }
    }
}

package com.recuperacao.weather_fetcher;

import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.core.JsonProcessingException;

public class ThreeThreadVersion {
    private static final int NUM_THREADS = 3;

    public static void main(String[] args) throws IOException, InterruptedException {
        List<Capital> capitals = loadCapitals(); // Load capital data from file
        ExecutorService executor = Executors.newFixedThreadPool(NUM_THREADS);

        long[] executionTimes = new long[10]; // Array to store execution times

        for (int i = 0; i < 10; i++) { // 10 repetitions
            long startTime = System.currentTimeMillis();
            CountDownLatch latch = new CountDownLatch(NUM_THREADS);

            for (int j = 0; j < NUM_THREADS; j++) {
                int startIdx = j * (capitals.size() / NUM_THREADS);
                int endIdx = (j + 1) * (capitals.size() / NUM_THREADS);
                List<Capital> subList = capitals.subList(startIdx, endIdx);
                executor.submit(new RequestTask(subList, latch));
            }

            latch.await(); // Wait for all threads to finish
            long endTime = System.currentTimeMillis();
            long executionTime = endTime - startTime;
            executionTimes[i] = executionTime;

            System.out.println("Execution time: " + executionTime + " ms");
        }

        // Calculate and display average, min, and max execution times
        displayStatistics(executionTimes);

        executor.shutdown();
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

            JsonNode hourly = root.path("hourly");
            if (hourly.isMissingNode()) {
                System.out.println("Hourly data is missing for " + capital.getName());
                return;
            }

            JsonNode temperatures = hourly.path("temperature_2m");
            if (temperatures.isMissingNode()) {
                System.out.println("Temperature data is missing for " + capital.getName());
                return;
            }

            int numDays = temperatures.size() / 24; // Assuming 24 data points per day
            double[] dailyAverage = new double[numDays];
            double[] dailyMax = new double[numDays];
            double[] dailyMin = new double[numDays];

            // Process temperatures day by day
            for (int day = 0; day < numDays; day++) {
                double[] tempsOfDay = new double[24];
                // Extract temperatures for the current day
                for (int hour = 0; hour < 24; hour++) {
                    tempsOfDay[hour] = temperatures.get(day * 24 + hour).asDouble();
                }
                // Calculate average, min, max for the day
                dailyAverage[day] = calculateAverage(tempsOfDay);
                dailyMax[day] = calculateMax(tempsOfDay);
                dailyMin[day] = calculateMin(tempsOfDay);

                // Print temperatures for the day
                System.out.println("Capital: " + capital.getName());
                System.out.println("Day " + (day + 1) + ":");
                System.out.println("Average temperature: " + dailyAverage[day]);
                System.out.println("Max temperature: " + dailyMax[day]);
                System.out.println("Min temperature: " + dailyMin[day]);
                System.out.println();
            }
        } catch (JsonProcessingException e) {
            e.printStackTrace();
        }
    }


    private static double calculateAverage(double[] temps) {
        double sum = 0;
        for (double temp : temps) {
            sum += temp;
        }
        return sum / temps.length;
    }

    private static double calculateMin(double[] temps) {
        double min = temps[0];
        for (double temp : temps) {
            if (temp < min) {
                min = temp;
            }
        }
        return min;
    }

    private static double calculateMax(double[] temps) {
        double max = temps[0];
        for (double temp : temps) {
            if (temp > max) {
                max = temp;
            }
        }
        return max;
    }

    private static List<Capital> loadCapitals() throws IOException {
        ObjectMapper mapper = new ObjectMapper();
        ClassLoader classLoader = ThreeThreadVersion.class.getClassLoader();
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

    private static class RequestTask implements Runnable {
        private List<Capital> capitals;
        private CountDownLatch latch;

        public RequestTask(List<Capital> capitals, CountDownLatch latch) {
            this.capitals = capitals;
            this.latch = latch;
        }

        @Override
        public void run() {
            try {
                for (Capital capital : capitals) {
                    String response = sendHttpRequest(capital.getLatitude(), capital.getLongitude());
                    processResponse(response, capital);
                }
            } catch (IOException | InterruptedException e) {
                e.printStackTrace();
            } finally {
                latch.countDown();
            }
        }
    }

    private static void displayStatistics(long[] executionTimes) {
        long total = 0;
        long min = executionTimes[0];
        long max = executionTimes[0];

        for (long time : executionTimes) {
            total += time;
            if (time < min) {
                min = time;
            }
            if (time > max) {
                max = time;
            }
        }

        double average = (double) total / executionTimes.length;
        System.out.println("\nExecution times for 10 repetitions:");
        for (int i = 0; i < executionTimes.length; i++) {
            System.out.println("Execution " + (i + 1) + ": " + executionTimes[i] + " ms");
        }
        System.out.println("Average execution time: " + average + " ms");
        System.out.println("Minimum execution time: " + min + " ms");
        System.out.println("Maximum execution time: " + max + " ms");
    }
}

package ConversorDeMoedas;

import com.google.gson.FieldNamingPolicy;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonObject;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;

public class Main {

    private static final String API_URL = "https://v6.exchangerate-api.com/v6/caec236f4b7c85927bd2503d/latest/USD";
    private static final String EXIT_COMMAND = "exit";
    private static final String[] SUPPORTED_CURRENCIES = {"USD", "EUR", "BRL", "ARS", "GBP"};
    private static final Gson GSON = new GsonBuilder()
            .setFieldNamingPolicy(FieldNamingPolicy.UPPER_CAMEL_CASE)
            .setPrettyPrinting()
            .create();

    public Main() throws IOException {
    }

    public static void main(String[] args) throws IOException, InterruptedException {
        Scanner sc = new Scanner(System.in);

        JsonObject exchangeRates = fetchExchangeRates();

        if (exchangeRates == null) {
            System.out.println("Error fetching exchange rates. Please try again later.");
            return;
        }

        List<Map<String, Object>> conversions = new ArrayList<>();

        System.out.println("""
                Welcome to the currency converter application.
                Please follow all instructions to avoid issues.
                To exit, type "exit" at any time.
                """);

        while (true) {
            try {
                System.out.println("""
                        ===============================================
                        To convert the value, enter the currency code:
                        * The code must be in uppercase letters *
                        US Dollar (USD);
                        Euro (EUR);
                        Brazilian Real (BRL);
                        Argentine Peso (ARS);
                        British Pound (GBP).
                        """);

                String fromCurrency = sc.nextLine().trim().toUpperCase();
                if (EXIT_COMMAND.equalsIgnoreCase(fromCurrency)) {
                    break;
                }

                if (!isValidCurrency(fromCurrency, exchangeRates)) {
                    System.out.println("Unrecognized or unsupported currency.");
                    continue;
                }

                System.out.println("""
                        Now, enter the code of the currency to convert to:
                        * The code must be in uppercase letters *
                        US Dollar (USD);
                        Euro (EUR);
                        Brazilian Real (BRL);
                        Argentine Peso (ARS);
                        British Pound (GBP).
                        """);

                String toCurrency = sc.nextLine().trim().toUpperCase();
                if (EXIT_COMMAND.equalsIgnoreCase(toCurrency)) {
                    break;
                }

                if (!isValidCurrency(toCurrency, exchangeRates)) {
                    System.out.println("Unrecognized or unsupported currency.");
                    continue;
                }

                System.out.println("Enter the amount to be converted:");
                double amount = sc.nextDouble();
                sc.nextLine(); // Clear buffer

                double fromRate = exchangeRates.getAsJsonObject("conversion_rates").get(fromCurrency).getAsDouble();
                double toRate = exchangeRates.getAsJsonObject("conversion_rates").get(toCurrency).getAsDouble();
                double convertedAmount = convert(amount, fromRate, toRate);

                System.out.printf("%f %s is equivalent to %f %s%n", amount, fromCurrency, convertedAmount, toCurrency);

                saveConversion(conversions, fromCurrency, amount, toCurrency, convertedAmount);
            } catch (InputMismatchException e) {
                System.out.println("Invalid value. Please enter a valid number.");
                sc.nextLine(); // Clear buffer
            } catch (Exception e) {
                System.out.println("Error: " + e.getMessage());
            }
        }

        saveConversionsToFile(conversions);
        System.out.println("Additional conversions saved to file.");
        sc.close();
    }

    private static JsonObject fetchExchangeRates() {
        try {
            HttpClient client = HttpClient.newHttpClient();
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(API_URL))
                    .build();
            HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());
            return GSON.fromJson(response.body(), JsonObject.class);
        } catch (IOException | InterruptedException e) {
            e.printStackTrace();
            return null;
        }
    }

    private static boolean isValidCurrency(String currency, JsonObject exchangeRates) {
        return exchangeRates.getAsJsonObject("conversion_rates").has(currency);
    }

    private static double convert(double amount, double fromRate, double toRate) {
        return Math.round(amount * (toRate / fromRate) * 100.0) / 100.0;
    }

    private static void saveConversion(List<Map<String, Object>> conversions, String fromCurrency, double fromAmount, String toCurrency, double toAmount) {
        Map<String, Object> conversion = new HashMap<>();
        conversion.put("date", LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")));
        conversion.put("from_currency", fromCurrency);
        conversion.put("from_amount", fromAmount);
        conversion.put("to_currency", toCurrency);
        conversion.put("to_amount", toAmount);
        conversions.add(conversion);
    }

    private static void saveConversionsToFile(List<Map<String, Object>> conversions) {
        File directory = new File("C:\\Users\\ffgus\\Desktop\\CurrencyConverter\\src\\history");
        if (!directory.exists()) {
            directory.mkdirs();
        }
        File file = new File(directory, "conversions.txt");

        try (FileWriter writer = new FileWriter(file, true)) {
            writer.write("\n");
            writer.write(GSON.toJson(conversions));
        } catch (IOException e) {
            System.out.println("Error saving conversions to file: " + e.getMessage());
        }
    }
}

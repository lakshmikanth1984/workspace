
import static io.restassured.RestAssured.get;

import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.math.RoundingMode;
import java.text.DecimalFormat;
import java.util.Arrays;
import java.util.Scanner;
import java.util.concurrent.TimeUnit;

import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.json.simple.parser.ParseException;
import org.openqa.selenium.By;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.chrome.ChromeDriver;

import io.github.bonigarcia.wdm.WebDriverManager;
import io.restassured.response.Response;

public class Weather {

	public static void main(String args[]) throws ParseException {

		// Getting list of city from the user and passing it in an array
		Scanner s = new Scanner(System.in);
		System.out.println("Enter the length of the array:");
		String l = s.nextLine();
		if (!l.matches("[0-9]")) {
			System.out.println("Please enter a number ");
			main(args);
			;
		}
		int length = Integer.parseInt(l);
		String[] myArray = new String[length];

		// Looping the city from the array
		for (int i = 0; i < myArray.length; i++) {
			System.out.println("Enter City Name - " + (i + 1));
			myArray[i] = s.nextLine();
		}
		System.out.println(Arrays.toString(myArray));

		for (int j = 0; j < myArray.length; j++) {
			String City = myArray[j].toString();

			// Getting the city id from the json file
			long city_id = city_id_retrieval(City);

			if (city_id != 0L) {
				// Retrieving the details from the API
				String response = weather_api(city_id);

				JSONObject obj_JSONObject = (JSONObject) new JSONParser().parse(response);
				JSONObject main = (JSONObject) obj_JSONObject.get("main");
				JSONObject wind = (JSONObject) obj_JSONObject.get("wind");

				Number wind_speed = (Number) wind.get("speed");
				Number humidity = (Number) main.get("humidity");
				Number Max_temp = (Number) main.get("temp_max");
				Number Min_temp = (Number) main.get("temp_min");
				System.out.println("City - " + City);

				// Calling selenium method to compare the retrieved value against the ui
				comparing_ui(city_id, humidity, wind_speed, Max_temp, Min_temp);
			}

			else {
				System.out.println("City " + City + " not found in the list");
			}
		}

	}

	// Call the API using Rest Assured library
	static String weather_api(long city_id) {

		String api_key = "ce658e43d986d3db6fdafac97e5a006b";
		// String city_id = "5263045";
		String base_uri = "http://api.openweathermap.org";
		Response response = get(base_uri + "/data/2.5/weather?id=" + city_id + "&APPID=" + api_key + "&units=imperial");
		String json_reponse = response.asString();
		System.out.println(json_reponse);
		return json_reponse;

	}

	// Retrieving the city id from the json file

	static long city_id_retrieval(String City) {

		long id = 0;

		JSONParser parser = new JSONParser();

		try {
			// JSONObject jsonObject = (JSONObject) jsonParser.parse(new
			// FileReader("city.list.json"));
			JSONArray a = (JSONArray) parser.parse(new FileReader("city.list.json"));
			for (Object o : a) {
				JSONObject city_file = (JSONObject) o;

				String name = (String) city_file.get("name");
				if (name.equalsIgnoreCase(City)) {
					// System.out.println(name);

					id = (long) city_file.get("id");
					// System.out.println(id);

				}

			}

		} catch (FileNotFoundException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (ParseException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}

		return id;

	}

	// Comparing the UI to the values retrived from the API.

	static void comparing_ui(long city_id, Number humidity, Number wind_speed, Number Max_Temp, Number Min_Temp) {

		WebDriverManager.chromedriver().setup();
		WebDriver driver = new ChromeDriver();
		driver.manage().timeouts().implicitlyWait(10, TimeUnit.SECONDS);
		driver.get("https://openweathermap.org/city/" + city_id);
		String temp_unit = driver.findElement(By.xpath("/html/body/main/div[2]/div/div/div/div[3]/div[1]/span/div/h3"))
				.getText();
		if (temp_unit.contains("C")) {
			driver.findElement(By.id("imperial")).click();
		}
		String humidity_ui = driver.findElement(By.xpath("//*[@id=\"weather-widget\"]/table/tbody/tr[4]/td[2]"))
				.getText();
		// System.out.println("Humidity from UI = "+humidity);
		if (humidity_ui.contains(humidity.toString())) {
			System.out.println("Humidity - PASS");
		} else {
			System.out.println("Humidity - FAIL " + humidity_ui);
		}
		String wind_speed_ui = driver.findElement(By.xpath("//*[@id=\"weather-widget-wind\"]")).getText();

		DecimalFormat df = new DecimalFormat("#.#");
		df.setRoundingMode(RoundingMode.CEILING);
		String wind_speed1 = df.format(wind_speed);
		if (wind_speed_ui.contains(wind_speed1)) {
			System.out.println("Wind Speed - PASS");
		} else {
			System.out.println("Wind Speed - FAIL " + wind_speed_ui);
		}
		int Max_Temp1 = Max_Temp.intValue();
		int Min_Temp1 = Min_Temp.intValue();
		double temp_diff = Max_Temp1 - Min_Temp1;
		if (temp_diff <= 15) {
			System.out.println(" Temperature difference is less than 15 - PASS\n");
		} else {
			System.out.println(" Temperature difference is greater than 15 - FAIL\n");
		}

		driver.close();

	}

}
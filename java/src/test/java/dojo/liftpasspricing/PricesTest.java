package dojo.liftpasspricing;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.sql.Connection;
import java.sql.SQLException;

import org.junit.jupiter.api.*;

import io.restassured.RestAssured;
import io.restassured.path.json.JsonPath;
import spark.Spark;

public class PricesTest {

    private static Connection connection;

    @BeforeAll
    public static void createPrices() throws SQLException {
        connection = Prices.createApp();
        connection.setAutoCommit(false);
    }

    @AfterAll
    public static void stopApplication() throws SQLException {
        Spark.stop();
        connection.close();
    }

    @AfterEach
    public void rollBackChanges() throws SQLException {
        connection.rollback();
    }

    @Test
    public void price_1day() {
        assertPriceEquals(35, "type=1jour");
    }

    @Test
    void price_1day_regular_day() {
        assertPriceEquals(35, "type=1jour&date=2023-03-22");
    }

    @Test
    public void price_1day_kid() {
        assertPriceEquals(0, "type=1jour&age=4");
    }

    @Test
    public void price_1day_teen() {
        assertPriceEquals(25, "type=1jour&age=14");
    }

    @Test
    void price_1day_monday() {
        assertPriceEquals(23, "type=1jour&date=2023-03-20");
    }

    @Test
    void price_1day_monday_senior() {
        assertPriceEquals(18, "type=1jour&date=2023-03-20&age=73");
    }

    @Test
    void price_1day_monday_adult() {
        assertPriceEquals(23, "type=1jour&date=2023-03-20&age=25");
    }

    @Test
    void price_1day_monday_holiday() {
        assertPriceEquals(35, "type=1jour&date=2019-02-18");
    }

    @Test
    public void price_night_kid() {
        assertPriceEquals(0, "type=night&age=5");
    }

    @Test
    void price_night_adult() {
        assertPriceEquals(19, "type=night&age=25");
    }

    @Test
    void price_night_senior() {
        assertPriceEquals(8, "type=night&age=72");
    }

    @Test
    void set_base_price_day() {
        assertPriceEquals(35, "type=1jour");
        setPrice("1jour", 55);
        assertPriceEquals(55, "type=1jour");
    }

    private void setPrice(String type, int price) {
        RestAssured
            .given()
            .port(4567)
            .when()
            .put("/prices?type={type}&cost={price}", type, price)
            .then()
            .assertThat()
            .statusCode(200)
            .assertThat()
            .contentType("application/json")
            .extract().jsonPath();
    }

    private void assertPriceEquals(int price, String queryParams) {
        JsonPath response = RestAssured
                .given()
                .port(4567)
                .when()
                .get("/prices?" + queryParams)
                .then()
                .assertThat()
                .statusCode(200)
                .assertThat()
                .contentType("application/json")
                .extract().jsonPath();

        assertEquals(price, response.getInt("cost"));
    }

}

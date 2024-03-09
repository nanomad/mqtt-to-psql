import jakarta.enterprise.context.ApplicationScoped
import org.apache.camel.CamelContext
import org.apache.camel.Headers
import org.apache.camel.builder.RouteBuilder
import org.apache.camel.component.paho.mqtt5.PahoMqtt5Constants.MQTT_TOPIC
import java.time.ZonedDateTime
import java.time.format.DateTimeFormatter

@ApplicationScoped
class MainRoute(context: CamelContext?) : RouteBuilder(context) {
    override fun configure() {
        from("paho-mqtt5:saic/_internal/api/vehicle/status/response")
            .routeId("vehicle-status-response")
            .to("direct:store-on-db")

        from("paho-mqtt5:saic/_internal/api/vehicle/charging/mgmtData/response")
            .routeId("vehicle-charging-mgmtData-response")
            .to("direct:store-on-db")

        from("direct:store-on-db")
            .routeId("store-on-db")
            .process { x ->
                val date = jsonpath("\$.headers.date").evaluate(x, String::class.java);
                val parsed = ZonedDateTime.parse(date, DateTimeFormatter.RFC_1123_DATE_TIME)
                x.message.setHeader("DATE", parsed.toEpochSecond())
            }
            .setHeader("SOURCE", header(MQTT_TOPIC))
            .setHeader("CONTENT", bodyAs(String::class.java))
            .setBody(constant("INSERT INTO MQTT_MESSAGES(ts, source, content) VALUES (:?DATE, :?SOURCE, to_json(:?CONTENT::json))"))
            .to("jdbc:datasource?useHeadersAsParameters=true")
            .end()
    }
}
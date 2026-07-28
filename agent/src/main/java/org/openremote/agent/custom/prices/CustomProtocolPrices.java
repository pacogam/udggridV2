package org.openremote.agent.custom.prices;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.openremote.agent.protocol.AbstractProtocol;
import org.openremote.model.Container;
import org.openremote.model.asset.agent.DefaultAgentLink;
import org.openremote.model.attribute.Attribute;
import org.openremote.model.attribute.AttributeEvent;
import org.openremote.model.syslog.SyslogCategory;

import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URI;
import java.net.URL;
import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.time.OffsetDateTime;
import java.util.*;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.logging.Logger;

import static org.openremote.model.syslog.SyslogCategory.PROTOCOL;
import org.openremote.agent.custom.CustomGetEnv;

public class CustomProtocolPrices extends AbstractProtocol<CustomAgentPrices, DefaultAgentLink> {
  private final ScheduledExecutorService scheduler = Executors.newScheduledThreadPool(1);
  private final ObjectMapper objectMapper = new ObjectMapper();


  public static final String PROTOCOL_DISPLAY_NAME = "CustomProtocolPrices";
  private static final Logger LOG = SyslogCategory.getLogger(PROTOCOL, CustomProtocolPrices.class);

  public CustomProtocolPrices(CustomAgentPrices agent) {
    super(agent);
  }

  @Override
  public String getProtocolName() {
    return PROTOCOL_DISPLAY_NAME;
  }

  @Override
  public String getProtocolInstanceUri() {
    return "customProtocolPrices://" + agent.getId();
  }

  @Override
  protected void doStart(Container container) throws Exception {
    LocalTime targetTime = LocalTime.of(21, 0); // Hora específica d'inici
    long initialDelay = calculateInitialDelay(targetTime);
    long hrs = initialDelay / 3600;
    long mins = (initialDelay % 3600) / 60;
    long secs = initialDelay % 60;
    System.out.println("Time until next execution: " + String.format("%02d:%02d:%02d", hrs, mins, secs));
    long period = 24 * 60 * 60;  // Repeat task every 24 hours

    final Runnable runnable = () -> {
      try {
        connectHttp();
      } catch (Exception e) {
        e.printStackTrace();
        LOG.warning("Agent='" + agent.getName() + "'; Unable to continue execution ; Exception: " + e);
      }
    };

    scheduler.scheduleAtFixedRate(runnable, initialDelay, period, TimeUnit.SECONDS);
  }

  @Override
  protected void doStop(Container container) throws Exception {
    scheduler.shutdown();
  }

  @Override
  protected void doLinkAttribute(String assetId, Attribute<?> attribute, DefaultAgentLink agentLink) throws RuntimeException {

  }

  @Override
  protected void doUnlinkAttribute(String assetId, Attribute<?> attribute, DefaultAgentLink agentLink) {

  }

  @Override
  protected void doLinkedAttributeWrite(DefaultAgentLink agentLink, AttributeEvent event, Object processedValue) {

  }

  private long calculateInitialDelay(LocalTime targetTime) {
    LocalDateTime now = LocalDateTime.now();
    LocalDateTime nextRun = now.withHour(targetTime.getHour()).withMinute(targetTime.getMinute()).withSecond(0).withNano(0);

    if (now.compareTo(nextRun) > 0) {
      nextRun = nextRun.plusDays(1);  // Si l'hora actual ja ha passat el targetTime d'avui, el programa per demà
    }

    Duration duration = Duration.between(now, nextRun);
    return duration.getSeconds();
  }

  private void connectHttp() {
    // Get the dates
    LocalDate currentDate = LocalDate.now();
    LocalDate tomorrowDate = currentDate.plusDays(1);

    DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd");

    String formattedCurrentDate = currentDate.format(formatter) + "T00:00";
    String formattedPreviousDate = tomorrowDate.format(formatter) + "T23:59";

    // Send HTTP request
    try {
      JsonNode jsonResponse = sendGetRequest(formattedCurrentDate, formattedPreviousDate);
      onMessageReceived(jsonResponse);
    } catch (IOException e) {
      LOG.warning(agent.getType() + "='" + agent.getName() + "'; Failed HTTP request; Exception: " + e);
    }
  }

  private JsonNode sendGetRequest(String start_date, String end_date) throws IOException {
    // Get agent attribute value
    String apiUrlMercats = CustomGetEnv.get("API_REE_MERCADOS");
    String urlPostRequest = apiUrlMercats + "?start_date=" + start_date + "&end_date=" + end_date + "&time_trunc=hour";
    // Send post request
    URI uri = URI.create(urlPostRequest);
    URL url = uri.toURL();
    HttpURLConnection connection = (HttpURLConnection) url.openConnection();
    connection.setRequestMethod("GET");
//        connection.setRequestProperty("Content-Type", "application/json");
//        connection.setDoOutput(true);

    try (InputStream responseStream = connection.getInputStream()) {
      return objectMapper.readTree(responseStream);
    }
  }

  private void onMessageReceived(JsonNode rootNode) {
    long timestampMillisDay = 0L;
    JsonNode includedNode = rootNode.path("included");
    // Parse JSON message and update database
    if (includedNode.isArray()) {
      String attributeName;
      for (JsonNode includedItem : includedNode) {
        String type = includedItem.path("type").asText();
        if (type.contains("PVPC")) {
          attributeName = CustomAgentPrices.PRICE_PVPC.getName();
        } else {
          attributeName = CustomAgentPrices.PRICE_SPOT.getName();
        }

        // Obtener el nodo "values"
        JsonNode valuesNode = includedItem.path("attributes").path("values");

        // Iterar sobre los valores dentro de "values"
        int c=0;
        if (valuesNode.isArray()) {
          for (JsonNode valueItem : valuesNode) {
            double value = valueItem.path("value").asDouble() / 1000;
            String datetime = valueItem.path("datetime").asText();
            Long timestampMillis = convertToMillis(datetime);
            datapointService.upsertValue(agent.getId(), attributeName, value, timestampMillis);
            c++;
          }
        }
        System.out.println("Trobats " + c + " valors per a " + type);
      }
    }
  }

  private Long convertToMillis(String isoTimestamp) {
    // Convert timestamp string to milliseconds with correct offset
    long timestampMillisOffset;

    OffsetDateTime offsetDateTime = OffsetDateTime.parse(isoTimestamp, DateTimeFormatter.ISO_OFFSET_DATE_TIME);
    long unixTimestampMillis = offsetDateTime.toInstant().getEpochSecond() * 1000;

    // Add the time zone offset in milliseconds
    TimeZone localtimeZone = TimeZone.getDefault();  // per defecte és Europe/Madrid
//            int offsetMillis = timeZone.getRawOffset();
    int offsetMillis = localtimeZone.getOffset(unixTimestampMillis);
    timestampMillisOffset = unixTimestampMillis + offsetMillis;

    return timestampMillisOffset;
  }
}

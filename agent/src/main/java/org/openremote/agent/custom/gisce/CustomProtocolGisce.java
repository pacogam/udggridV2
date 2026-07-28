package org.openremote.agent.custom.gisce;


import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
//import org.openremote.agent.custom.reschool.ReschoolMeterAsset;
import org.openremote.agent.protocol.AbstractProtocol;
import org.openremote.model.util.UniqueIdentifierGenerator;
import org.openremote.model.Container;

import org.openremote.model.asset.Asset;
import org.openremote.model.asset.agent.DefaultAgentLink;
import org.openremote.model.attribute.Attribute;
import org.openremote.model.attribute.AttributeEvent;
//import org.openremote.model.custom.CustomAsset;
import org.openremote.model.syslog.SyslogCategory;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URI;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.Date;
import java.util.Objects;
import java.util.Optional;
import java.util.TimeZone;
import java.time.LocalTime;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.logging.Logger;
import java.util.HashMap;
import java.util.Map;
import java.util.TreeMap;

import static org.openremote.model.syslog.SyslogCategory.PROTOCOL;
import org.openremote.agent.custom.CustomGetEnv;

public class CustomProtocolGisce extends AbstractProtocol<CustomAgentGisce, DefaultAgentLink> {
  private final ScheduledExecutorService scheduler = Executors.newScheduledThreadPool(1);
  private final ObjectMapper objectMapper = new ObjectMapper();


  public static final String PROTOCOL_DISPLAY_NAME = "CustomProtocolGisce";
  private static final Logger LOG = SyslogCategory.getLogger(PROTOCOL, CustomProtocolGisce.class);

  public CustomProtocolGisce(CustomAgentGisce agent) {
    super(agent);
  }

  @Override
  public String getProtocolName() {
    return PROTOCOL_DISPLAY_NAME;
  }

  @Override
  public String getProtocolInstanceUri() {
    return "customProtocolGisce://" + agent.getId();
  }

  @Override
  protected void doStart(Container container) throws Exception {
//    sendAttributeEvent(new AttributeEvent(agent.getId(), CustomAgentGisce.ID_SUPERVISOR.getName(), "CIR2081523150"));  // assignem un valor per defecte
    System.out.println(LocalDate.now());
    System.out.println(" >>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>> doStart Iniciado <<<");
    LocalTime targetTime = LocalTime.of(10, 30); // Hora específica d'inici: 10:30 AM
    long initialDelay = calculateInitialDelay(targetTime);
    long hrs = initialDelay / 3600;
    long mins = (initialDelay % 3600) / 60;
    long secs = initialDelay % 60;
    System.out.println("Time until next execution: " + String.format("%02d:%02d:%02d", hrs, mins, secs));  //hrs, mins, secs)
    // Repeat task every 24 hours
    long period = 24 * 60 * 60;

    final Runnable runnable = () -> {
      try {
        connectHttp();
      } catch (Exception e) {
        e.printStackTrace();
        LOG.warning("Agent='" + agent.getName() + "'; Unable to continue execution ; Exception: " + e);
      }
    };

    scheduler.scheduleAtFixedRate(runnable, initialDelay, period, TimeUnit.SECONDS); //initialDelay
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
    LocalDate previousDate = currentDate.minusDays(3);

    DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd");

    String formattedCurrentDate = currentDate.format(formatter);
    String formattedPreviousDate = previousDate.format(formatter);

    // Send HTTP request
    try {
      String accessToken = getAccessToken();
      JsonNode requestData = objectMapper.createObjectNode()
        .put("data_inici", formattedPreviousDate)
        .put("data_fi", formattedCurrentDate);
      //.put("cups", "ES0176001900000035RS0F");

      JsonNode jsonResponse = sendPostRequest(accessToken, requestData);

      // fem la segona crida a l'API per obtenir les dades del supervisor
      String idSupervisor = agent.getIdSupervisor().orElse("CIR2081523150");
      ((ObjectNode) requestData).put("supervisor", idSupervisor);  // afegim el camp "supervisor" al json que enviem
      JsonNode jsonResponse2 = sendPostRequest(accessToken, requestData);
      onMessageReceived(jsonResponse, jsonResponse2);
    } catch (IOException e) {
      LOG.warning(agent.getType() + "='" + agent.getName() + "'; Failed HTTP request; Exception: " + e);
    }
  }

  private String getAccessToken() throws IOException {
    // Get agent attribute values
    String apiUrlGisce = CustomGetEnv.get("API_GISCE_URL");
    String urlBase = agent.getBaseURI().orElse(apiUrlGisce);
    String urlAccessToken = urlBase + "/token";  // agent.getUrlAccessToken().orElse(urlBase + "/token");
    String requestPropertyValue = CustomGetEnv.get("API_GISCE_TOKEN");

    // Get access token
    URI uri0 = URI.create(urlAccessToken);
    URL url0 = uri0.toURL();
    HttpURLConnection connection = (HttpURLConnection) url0.openConnection();
    connection.setRequestMethod("GET");
    connection.setRequestProperty("Authorization", requestPropertyValue);

    try (InputStream responseStream = connection.getInputStream()) {
      JsonNode jsonResponse = objectMapper.readTree(responseStream);
      return jsonResponse.get("token").asText();
    }
  }

  private JsonNode sendPostRequest(String accessToken, JsonNode requestData) throws IOException {
    // Get agent attribute value
    String urlPostRequest = CustomGetEnv.get("API_GISCE_URL_CORBES");
    // agent.getUrlPostRequest().orElse(urlPostRequest);

    // Send post request
    URI uri = URI.create(urlPostRequest);
    URL url = uri.toURL();

    HttpURLConnection connection = (HttpURLConnection) url.openConnection();
    connection.setRequestMethod("POST");
    connection.setRequestProperty("Authorization", "token " + accessToken);
    connection.setRequestProperty("Content-Type", "application/json");
    connection.setDoOutput(true);

    try (OutputStream outputStream = connection.getOutputStream()) {
      objectMapper.writeValue(outputStream, requestData);
    }

    try (InputStream responseStream = connection.getInputStream()) {
      return objectMapper.readTree(responseStream);
    }
  }

  private void onMessageReceived(JsonNode jsonNode, JsonNode jsonNode2) {
    int cont = 0;
    int activeDevices = 0;
    String name_ant = "";
    long timestampMillisDay = 0L;
    DynamicDataFrame df = new DynamicDataFrame();
    // Parse JSON message and update database
    JsonNode resArray = jsonNode.get("res");
    long oneDayInMillis = 24 * 60 * 60 * 1000L; // ms en un día
    if (resArray.isArray()) {
      int total_records = resArray.size();
      System.out.println(">> TOTAL RECORDS: " + total_records);
      datapointService.upsertValue(agent.getId(), CustomAgentGisce.TOTAL_RECORDS.getName(), total_records, timerService.getCurrentTimeMillis() - oneDayInMillis);
      for (JsonNode entry : resArray) {
        String name = entry.get("name").asText();
        Long timestampMillis = convertToMillis(entry.get("timestamp").asText());
        Optional<Double> r1 = Optional.of(entry.get("r1").asDouble());
        Optional<Double> r2 = Optional.of(entry.get("r2").asDouble());
        Optional<Double> r3 = Optional.of(entry.get("r3").asDouble());
        Optional<Double> r4 = Optional.of(entry.get("r4").asDouble());
        Optional<Double> ae = Optional.of(entry.get("ae").asDouble());
        Double ai = null;
        if (entry.findValue("ai") != null) {
          ai = (entry.get("ai").asDouble());
          df.addValue(timestampMillis, ai);
        } // else {ai = Optional.empty();}

        Optional<Double> ai_fix;
        if (entry.findValue("ai_fix") != null) {
          ai_fix = Optional.of(entry.get("ai_fix").asDouble());
        } else {ai_fix = Optional.empty();}
        Optional<Double> ai_fact;
        if (entry.findValue("ai_fact") != null) {
          ai_fact = Optional.of(entry.get("ai_fact").asDouble());
        } else {ai_fact = Optional.empty();}
        Optional<Double> ae_fix;
        if (entry.findValue("ae_fix") != null) {
          ae_fix = Optional.of(entry.get("ae_fix").asDouble());
        } else {ae_fix = Optional.empty();}
        Optional<Double> ae_fact;
        if (entry.findValue("ae_fact") != null) {
          ae_fact = Optional.of(entry.get("ae_fact").asDouble());
        } else {ae_fact = Optional.empty();}

        Optional<Double> r1_fact;
        if (entry.findValue("r1_fact") != null) {
          r1_fact = Optional.of(entry.get("r1_fact").asDouble());
        } else {r1_fact = Optional.empty();}
        Optional<Double> r2_fact;
        if (entry.findValue("r2_fact") != null) {
          r2_fact = Optional.of(entry.get("r2_fact").asDouble());
        } else {r2_fact = Optional.empty();}
        Optional<Double> r3_fact;
        if (entry.findValue("r3_fact") != null) {
          r3_fact = Optional.of(entry.get("r3_fact").asDouble());
        } else {r3_fact = Optional.empty();}
        Optional<Double> r4_fact;
        if (entry.findValue("r4_fact") != null) {
          r4_fact = Optional.of(entry.get("r4_fact").asDouble());
        } else {r4_fact = Optional.empty();}

        // Generate asset ID from JSON message
        String assetId = UniqueIdentifierGenerator.generateId(agent.getRealm() + name);
        // Find if asset already exists
        Asset<?> asset = assetService.findAsset(assetId);

        // Create automatically a new asset if it doesn't exist
        if (asset == null) {
          createAsset(name, assetId);
          System.out.println(name);
        }

        if (!Objects.equals(name, name_ant)) {
          cont=0;
          activeDevices++;
        }
//                if (cont<24) {
//                    if (ai != null) {
//                        dailyTotalEnergy = ai + dailyTotalEnergy;
//                    }
//                }
//                if (cont == 23) {System.out.println(name);}
        cont++;
        name_ant = name;
        if (timestampMillisDay < timestampMillis) {
          timestampMillisDay = timestampMillis;
        }

        // Put data-points directly into database
        if (timestampMillis != null) {
          datapointService.upsertValue(assetId, CustomAssetGisce.ENERGY_R1.getName(), r1, timestampMillis);
          datapointService.upsertValue(assetId, CustomAssetGisce.ENERGY_R2.getName(), r2, timestampMillis);
          datapointService.upsertValue(assetId, CustomAssetGisce.ENERGY_R3.getName(), r3, timestampMillis);
          datapointService.upsertValue(assetId, CustomAssetGisce.ENERGY_R4.getName(), r4, timestampMillis);
          datapointService.upsertValue(assetId, CustomAssetGisce.ENERGY_IMPORT.getName(), ai, timestampMillis);
          datapointService.upsertValue(assetId, CustomAssetGisce.ENERGY_EXPORT.getName(), ae, timestampMillis);
          if (ai_fix.isPresent()) {datapointService.upsertValue(assetId, CustomAssetGisce.ENERGY_IMPORT_FIX.getName(), ai_fix, timestampMillis);}
          if (ai_fact.isPresent()) {datapointService.upsertValue(assetId, CustomAssetGisce.ENERGY_IMPORT_FACT.getName(), ai_fact, timestampMillis);}
          if (ae_fix.isPresent()) {datapointService.upsertValue(assetId, CustomAssetGisce.ENERGY_EXPORT_FIX.getName(), ae_fix, timestampMillis);}
          if (ae_fact.isPresent()) {datapointService.upsertValue(assetId, CustomAssetGisce.ENERGY_EXPORT_FACT.getName(), ae_fact, timestampMillis);}
          if (r1_fact.isPresent()) {datapointService.upsertValue(assetId, CustomAssetGisce.ENERGY_R1_FACT.getName(), r1_fact, timestampMillis);}
          if (r2_fact.isPresent()) {datapointService.upsertValue(assetId, CustomAssetGisce.ENERGY_R2_FACT.getName(), r2_fact, timestampMillis);}
          if (r3_fact.isPresent()) {datapointService.upsertValue(assetId, CustomAssetGisce.ENERGY_R3_FACT.getName(), r3_fact, timestampMillis);}
          if (r4_fact.isPresent()) {datapointService.upsertValue(assetId, CustomAssetGisce.ENERGY_R4_FACT.getName(), r4_fact, timestampMillis);}
        }
      }
      // timestampMillisDay = timestampMillisDay + 86400000;
      //sendAttributeEvent(new AttributeEvent(agent.getId(), CustomAgentGisce.ACTIVE_DEVICES.getName(), activeDevices, timerService.getCurrentTimeMillis()));
      datapointService.upsertValue(agent.getId(), CustomAgentGisce.ACTIVE_DEVICES.getName(), activeDevices, timerService.getCurrentTimeMillis() - oneDayInMillis);

      Map<Long, Double> sortedData = df.getSortedData();  // Obtenir les dades ordenades
      // Iterem sobre les dades ordenades
      for (Map.Entry<Long, Double> entry : sortedData.entrySet()) {
        long timestamp = entry.getKey();
        double value = entry.getValue();
        datapointService.upsertValue(agent.getId(), CustomAgentGisce.TOTAL_HOURLY_ENERGY.getName(), value, timestamp);
      }
      System.out.println(">> Call to decoSupervisor");
      decoSupervisor(jsonNode2);

      // cridem el forecasting extern, si està activat
      Boolean isForecastEnabled = agent.getEnableForecast().orElse(false);
      if (isForecastEnabled) {
        try {
//                    getHttpTest(agent.getId());
          JsonNode jsonResponse2 = sendPostForecasting(agent.getId(), timestampMillisDay);
          JsonNode info = jsonResponse2.get("message");
          //sendAttributeEvent(new AttributeEvent(agent.getId(), CustomAgentGisce.NOTES.getName(), info));
        } catch (IOException e) {
          LOG.warning(agent.getType() + "='" + agent.getName() + "'; Failed external HTTP request; Exception: " + e);
        }
      }
    }
  }

  private void decoSupervisor(JsonNode jsonNode) {
    JsonNode resArray = jsonNode.get("res");
    if (resArray.isArray()) {
      for (JsonNode entry : resArray) {
        String name = entry.get("name").asText();
        // int mult = entry.get("magn").asInt();  anava a multiplicar per 1000 per fer la conversió a KWh però no cal, ja ho definiré amb les unitats
        Long timestampMillis = convertToMillis(entry.get("timestamp").asText());
        Optional<Double> r1 = Optional.of(entry.get("r1").asDouble());
        Optional<Double> r2 = Optional.of(entry.get("r2").asDouble());
        Optional<Double> r3 = Optional.of(entry.get("r3").asDouble());
        Optional<Double> r4 = Optional.of(entry.get("r4").asDouble());
        Optional<Double> ae = Optional.of(entry.get("ae").asDouble());
        Double ai = entry.get("ai").asDouble();

        // Put data-points directly into database
        if (timestampMillis != null) {
          datapointService.upsertValue(agent.getId(), CustomAgentGisce.ENERGY_R1.getName(), r1, timestampMillis);
          datapointService.upsertValue(agent.getId(), CustomAgentGisce.ENERGY_R2.getName(), r2, timestampMillis);
          datapointService.upsertValue(agent.getId(), CustomAgentGisce.ENERGY_R3.getName(), r3, timestampMillis);
          datapointService.upsertValue(agent.getId(), CustomAgentGisce.ENERGY_R4.getName(), r4, timestampMillis);
          datapointService.upsertValue(agent.getId(), CustomAgentGisce.ENERGY_IMPORT.getName(), ai, timestampMillis);
          datapointService.upsertValue(agent.getId(), CustomAgentGisce.ENERGY_EXPORT.getName(), ae, timestampMillis);
        }
      }
    }
  }

  public static class DynamicDataFrame {
    private Map<Long, Double> data;

    public DynamicDataFrame() {
      data = new HashMap<>();
    }

    public void addValue(long timestamp, double value) {
      // Si el timestamp ja existeix, suma el nou valor a l'existent
      data.put(timestamp, data.getOrDefault(timestamp, 0.0) + value);
    }

    // Mètode per obtenir les dades ordenades per timestamp
    public Map<Long, Double> getSortedData() {
      return new TreeMap<>(data);
    }

    public void printData() {
      for (Map.Entry<Long, Double> entry : data.entrySet()) {
        System.out.println("Timestamp: " + entry.getKey() + ", Value: " + entry.getValue());
      }
    }
////    df.printData();
  }

  private Long convertToMillis(String timestamp) {
    // Convert timestamp string to milliseconds with correct offset
    long timestampMillisOffset;
    long timestampMillis;
    try {
      SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
      Date date = sdf.parse(timestamp);
      timestampMillis = date.getTime();  // ho passa a epoch time

      // Add the time zone offset in milliseconds
      TimeZone localtimeZone = TimeZone.getDefault();  // per defecte és Europe/Madrid
//            int offsetMillis = timeZone.getRawOffset();
      int offsetMillis = localtimeZone.getOffset(timestampMillis);
      timestampMillisOffset = timestampMillis + offsetMillis;
    } catch (ParseException e) {
      return null;
    }
    return timestampMillis;  // retorna TS en UTC
    //return timestampMillisOffset;
  }

  private String getHttpTest(String site_id) throws IOException {
    // Get agent attribute values
    String urlUdg = CustomGetEnv.get("API_UDG_URL");
    String apiUdgToken = CustomGetEnv.get("API_UDG_TOKEN");
    String urlAccess = urlUdg + "/dummy/" + site_id;

    // Get access token
    URI uri = URI.create(urlAccess);
    URL url = uri.toURL();

    HttpURLConnection connection = (HttpURLConnection) url.openConnection();
    connection.setRequestMethod("GET");
    connection.setRequestProperty("Authorization", apiUdgToken);

    try (InputStream responseStream = connection.getInputStream()) {
      JsonNode jsonResponse = objectMapper.readTree(responseStream);
      return jsonResponse.get("value").asText();
    }
  }

  private JsonNode sendPostForecasting(String agent_id, Long sch_date) throws IOException {
    // Get agent attribute value
    String urlAccess = CustomGetEnv.get("API_UDG_FORECAST");
    String apiUdgToken = CustomGetEnv.get("API_UDG_TOKEN");
    // Send HTTP request
    JsonNode requestData = objectMapper.createObjectNode()
      .put("date", sch_date)
      .put("agent_id", agent_id);
    // JsonNode jsonResponse = sendPostForecasting(agent_id, sch_date);

    // Send post request
    URI uri1 = URI.create(urlAccess);
    URL url1 = uri1.toURL();
    HttpURLConnection connection = (HttpURLConnection) url1.openConnection();
    connection.setRequestMethod("POST");
    connection.setRequestProperty("Authorization", apiUdgToken);
    connection.setRequestProperty("Content-Type", "application/json");
    connection.setDoOutput(true);

    try (OutputStream outputStream = connection.getOutputStream()) {
      objectMapper.writeValue(outputStream, requestData);
    }

    try (InputStream responseStream = connection.getInputStream()) {
      return objectMapper.readTree(responseStream);
    }
  }

  private void createAsset(String assetName, String assetId) {
    // Create new asset
    CustomAssetGisce customAssetGisce = new CustomAssetGisce(assetName);
    // Set asset ID (required)
    customAssetGisce.setId(assetId);
    // Set agent as parent of asset
    customAssetGisce.setParentId(agent.getId());

    // Add asset to database
    assetService.mergeAsset(customAssetGisce);

    LOG.info(agent.getType() + "='" + agent.getName() + "'; Created " + customAssetGisce.getType() + ":'" + assetId + "'");
  }
}

## Introductie

OurGrid laat je weten wanneer er netcongestie is in jouw buurt. Als je dan minder stroom verbruikt, kun je punten verdienen—en daarmee ook geld.
Koppel openHAB en laat je slimme apparaten automatisch reageren op uitdagingen. Je krijgt alle info die nodig is om het stroomdoel te halen en de uitdaging te voltooien.

## Verbind met openHAB

Voordat je begint, open de openHAB gebruikersinterface in je browser.

### 1. MQTT Binding add-on installeren

1. Ga naar **Add-on Winkel** in de zijbalk
2. Klik op **Bindings**
3. In het zoekveld dat verschijnt, type "MQTT"
4. De "MQTT Binding" verschijnt als zoekresultaat
5. Klik op de **Install** knop om de add-on te installeren

### 2. RegEx Transformation add-on installeren

1. Ga naar **Add-on Winkel** in de zijbalk
2. Klik op **Transformations**
3. In het zoekveld dat verschijnt, type "RegEx"
4. De "RegEx Transformation" verschijnt als zoekresultaat
5. Klik op de **Install** knop om de add-on te installeren

### 3. OurGrid Things toevoegen

Maak eerst een verbinding met de OurGrid Broker:

1. Ga naar **Instellingen > Things** in de zijbalk
2. Klik op de **Toevoegen** knop in de rechteronderhoek
3. Klik op **MQTT Binding**
4. Klik op **MQTT Broker**
5. Voer de volgende gegevens in:
    ```
    - Thing ID: ourgrid
    - Label: OurGrid Broker
    - Broker Hostname/IP: {{hostname}}
    ```
6. Klik op "Create Thing"
7. Selecteer het "OurGrid Broker" Thing
8. Open het tabblad **Code** in de rechterbovenhoek
9. Voer de volgende gegevens in:
    ```yaml
    UID: mqtt:broker:ourgrid
    label: OurGrid Broker
    thingTypeUID: mqtt:broker
    configuration:
      lwtQos: 0
      publickeypin: false
      clientID: {{client_id}}
      keepAlive: 60
      hostnameValidated: true
      birthRetain: true
      secure: true
      certificatepin: false
      shutdownRetain: true
      password: {{client_secret}}
      protocol: TCP
      qos: 1
      reconnectTime: 5000
      mqttVersion: V3
      host: {{hostname}}
      lwtRetain: true
      enableDiscovery: false
      username: {{realm}}:{{client_id}}
    ```
10. Klik op "Save" in de rechterbovenhoek
11. Klik op "Back"
12. Het "OurGrid Broker" Thing zou nu online moeten zijn

Maak vervolgens een OurGrid Challenge Thing:

1. Ga naar **Instellingen > Things** in de zijbalk
2. Klik op de **Toevoegen** knop in de rechteronderhoek
3. Klik op **MQTT Binding**
4. Klik op **Generic MQTT Thing**
5. Voer de volgende gegevens in:
    ```
    - Thing ID: challenge
    - Label: OurGrid Challenge
    - Bridge: OurGrid Broker
    ```
6. Klik op "Create Thing"
7. Selecteer het "OurGrid Challenge" Thing
8. Open het tabblad **Code** in de rechterbovenhoek
9. Voer de volgende gegevens in:
    ```yaml
    UID: mqtt:topic:ourgrid:challenge
    label: OurGrid Challenge
    thingTypeUID: mqtt:topic
    configuration: {}
    bridgeUID: mqtt:broker:ourgrid
    channels:
      - id: start
        channelTypeUID: mqtt:datetime
        label: Start
        configuration:
          stateTopic: {{realm}}/{{client_id}}/attributevalue/challengeStart/{{challenge_asset_id}}
          transformationPattern:
            - REGEX:s/ /T/
            - REGEX:"(.+)"
      - id: end
        channelTypeUID: mqtt:datetime
        label: End
        configuration:
          stateTopic: {{realm}}/{{client_id}}/attributevalue/challengeEnd/{{challenge_asset_id}}
          transformationPattern:
            - REGEX:s/ /T/
            - REGEX:"(.+)"
      - id: pointsExchangeRate
        channelTypeUID: mqtt:number
        label: Points Exchange Rate
        configuration:
          stateTopic: {{realm}}/{{client_id}}/attributevalue/challengePointsExchangeRate/{{challenge_asset_id}}
    ```
10. Klik op "Save" in de rechterbovenhoek
11. Klik op "Back"
12. Het "OurGrid Challenge" Thing zou nu online moeten zijn

Maak vervolgens een OurGrid Meter Thing:

1. Ga naar **Instellingen > Things** in de zijbalk
2. Klik op de **Toevoegen** knop in de rechteronderhoek
3. Klik op **MQTT Binding**
4. Klik op **Generic MQTT Thing**
5. Voer de volgende gegevens in:
    ```
    - Thing ID: meter
    - Label: OurGrid Meter
    - Bridge: OurGrid Broker
    ```
6. Klik op "Create Thing"
7. Selecteer het "OurGrid Meter" Thing
8. Open het tabblad **Code** in de rechterbovenhoek
9. Voer de volgende gegevens in:
    ```yaml
    UID: mqtt:topic:ourgrid:meter
    label: OurGrid Meter
    thingTypeUID: mqtt:topic
    configuration: {}
    bridgeUID: mqtt:broker:ourgrid
    channels:
      - id: challengeStatus
        channelTypeUID: mqtt:string
        label: Challenge Status
        configuration:
          stateTopic: {{realm}}/{{client_id}}/attributevalue/challengeStatus/{{meter_asset_id}}
          transformationPattern:
            - REGEX:"(.+)"
      - id: challengePowerLimit
        channelTypeUID: mqtt:number
        label: Challenge Power Limit
        configuration:
          stateTopic: {{realm}}/{{client_id}}/attributevalue/challengePowerLimit/{{meter_asset_id}}
          unit: W
      - id: joinButton
        channelTypeUID: mqtt:switch
        label: Join Button
        configuration:
          postCommand: false
          retained: true
          qos: 1
          transformationPatternOut:
            - REGEX:s/ON/true/
            - REGEX:s/OFF/false/
          commandTopic: {{realm}}/{{client_id}}/writeattributevalue/challengeJoinButton/{{meter_asset_id}}
          stateTopic: {{realm}}/{{client_id}}/attributevalue/challengeJoinButton/{{meter_asset_id}}
          transformationPattern:
            - REGEX:s/true/ON/
            - REGEX:s/false/OFF/
          off: "false"
          on: "true"
      - id: power
        channelTypeUID: mqtt:number
        label: Power
        configuration:
          stateTopic: {{realm}}/{{client_id}}/attributevalue/power/{{meter_asset_id}}
          unit: W
    ```
10. Klik op "Save" in de rechterbovenhoek
11. Klik op "Back"
12. Het "OurGrid Meter" Thing zou nu online moeten zijn

### Einde van de installatie

De Things zouden nu allemaal aanwezig moeten zijn in openHAB. Je kunt deze nu toevoegen aan je Sitemaps of andere onderdelen van je openHAB installatie.

## Reageren op een uitdaging

Gebruik de volgende Thing channels van OurGrid om je automatiseringsregels in te stellen:

| Thing     | Channel               | Gebruik                                                |
| --------- | --------------------- | ------------------------------------------------------ |
| Challenge | `start`, `end`        | Wanneer de uitdaging plaatsvindt                       |
| Challenge | `pointsExchangeRate`  | Hoeveel je punten waard zijn (€)                       |
| Meter     | `challengeStatus`     | Geeft aan of er een actieve uitdaging is (noChallenge, joinChallenge, joinedChallenge, activeChallenge) |
| Meter     | `challengePowerLimit` | Maximaal toegestaan vermogen (kW)                      |
| Meter     | `joinButton`          | Zet op `ON` om automatisch mee te doen en te verdienen |
| Meter     | `power`               | Energieverbruik van je woning                          |

Wanneer `challengeStatus` de waarde "joinChallenge" heeft, begint de challenge binnenkort.
Verlaag dan je netto `power` verbruik tot onder de `challengePowerLimit` tussen `start` en `end`.
Zet `joinButton` op `ON` om mee te doen aan de uitdaging en punten te verdienen.

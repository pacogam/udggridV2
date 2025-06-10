## Introductie
OurGrid laat je weten wanneer er netcongestie is in jouw buurt. Als je dan minder stroom verbruikt, kun je punten verdienen—en daarmee ook geld.

Koppel je Home Assistant en laat je slimme apparaten automatisch reageren op uitdagingen. Je krijgt alle info die nodig is om het stroomdoel te halen en de uitdaging te voltooien.

## Verbind met Home Assistant

Voordat je begint, open de Home Assistant gebruikersinterface in je browser.

### 1. Geavanceerde Modus Inschakelen

1. Klik in Home Assistant op je gebruikersprofielafbeelding in de linkerbenedenhoek
2. Zorg ervoor dat **Geavanceerde Modus** is ingeschakeld in je gebruikersprofielinstellingen

### 2. MQTT Integratie Toevoegen

1. Ga naar **Instellingen** in de Home Assistant interface
2. Klik op **Apparaten & Diensten**
3. Zoek de **Integratie toevoegen** knop in de rechterbenedenhoek en klik erop
4. Typ "MQTT" in het zoekveld dat verschijnt
5. Klik op de **MQTT** integratie wanneer deze in de resultaten verschijnt

### 3. MQTT Integratie Configureren

1. Selecteer bij de prompt **Handmatig de MQTT broker verbindingsgegevens opgeven**
2. Schakel de **Geavanceerde opties** in (dit is vereist voor de juiste configuratie)
3. Voer de volgende verbindingsgegevens in:
    ```
    - Broker: {{hostname}}
    - Poort: 8883
    - Gebruikersnaam: {{realm}}:{{client_id}}
    - Wachtwoord: {{client_secret}}
    ```
4. Klik op **Verzenden** om door te gaan naar de geavanceerde opties

### 4. Geavanceerde MQTT Configuratie

1. In de geavanceerde opties hoef je alleen het volgende in te stellen:
    ```
    - Client ID: {{client_id}}
    - Broker certificaat validatie: Automatisch
    ```
2. Klik op **Verzenden** om de configuratie op te slaan

### 5. File Editor Addon Installeren

> **Let op:** Als je de File Editor addon al hebt geïnstalleerd en draait, kun je dit onderdeel overslaan.

1. Ga naar **Instellingen** in de Home Assistant interface
2. Klik op **Add-ons**
3. Klik op **Add-on winkel** in de rechterbenedenhoek
4. Gebruik het zoekveld om "File Editor" te vinden
5. Klik op de **File Editor** addon wanneer deze verschijnt
6. Klik op de **Installeren** knop
7. Na installatie kun je de **Weergeven in zijbalk** schakelaar naar rechts zetten (aan)
8. Klik op **Starten** om ervoor te zorgen dat de addon draait

### 6. OurGrid Entiteiten Toevoegen

1. Ga naar de **File Editor** addon in de sidebar
2. Klik op het **Folder icoontje** in de linkerbovenhoek van de file editor
3. Selecteer het **configuration.yaml** bestand
4. Kopieer en plak het volgende gedeelte in je **configuration.yaml** bestand:
    ```yaml
    mqtt:
        sensor:
          # Meter sensoren
          - name: "OurGrid Meter Power"
            state_topic: "{{realm}}/{{client_id}}/attributevalue/power/{{meter_asset_id}}"
            unit_of_measurement: "W"
            device_class: "power"
            state_class: "measurement"
            suggested_display_precision: 0
            icon: "mdi:lightning-bolt"

          - name: "OurGrid Challenge Status"
            state_topic: "{{realm}}/{{client_id}}/attributevalue/challengeStatus/{{meter_asset_id}}"
            icon: "mdi:information-outline"
            value_template: "{{ value | default('unknown') }}"
              
          - name: "OurGrid Challenge Vermogenslimiet"
            state_topic: "{{realm}}/{{client_id}}/attributevalue/challengePowerLimit/{{meter_asset_id}}"
            unit_of_measurement: "W"
            device_class: "power"
            state_class: "measurement"
            suggested_display_precision: 0
            icon: "mdi:lightning-bolt"

          # Challenge sensoren
          - name: "OurGrid Challenge Punten Wisselkoers"
            state_topic: "{{realm}}/{{client_id}}/attributevalue/challengePointsExchangeRate/{{challenge_asset_id}}"
            state_class: "measurement"
            suggested_display_precision: 2
            icon: "mdi:currency-eur"

          - name: "OurGrid Challenge Start"
            state_topic: "{{realm}}/{{client_id}}/attributevalue/challengeStart/{{challenge_asset_id}}"
            icon: "mdi:flag-checkered"

          - name: "OurGrid Challenge Eind"
            state_topic: "{{realm}}/{{client_id}}/attributevalue/challengeEnd/{{challenge_asset_id}}"
            icon: "mdi:flag-checkered"

        switch:
          # Challenge schakelaar
          - name: "OurGrid Challenge Deelnemen Knop"
            state_topic: "{{realm}}/{{client_id}}/attributevalue/challengeJoinButton/{{meter_asset_id}}"
            command_topic: "{{realm}}/{{client_id}}/writeattributevalue/challengeJoinButton/{{meter_asset_id}}"
            payload_on: "true"
            payload_off: "false"
            state_on: "true"
            state_off: "false"
            retain: true
    ```
5. Klik op de **Opslaan** knop in de rechterbovenhoek van de file editor
6. Ga naar **Ontwikkelhulpmiddelen** in de linkersidebar
7. Klik onder het **YAML-configuratie herladen** gedeelte op de **ALLE YAML CONFIGURATIE** knop
8. De entiteiten zouden nu toegevoegd moeten zijn aan je Home Assistant installatie.

> **Let op:** Als je problemen ondervindt met het toevoegen van de entiteiten, controleer dan de logs op eventuele fouten. In sommige gevallen kan het nodig zijn om Home Assistant opnieuw op te starten.

### Einde van de installatie
De entiteiten zouden nu allemaal aanwezig moeten zijn in je Home Assistant installatie. Je kunt deze nu toevoegen aan je dashboards of andere onderdelen van je Home Assistant installatie.

## Reageren op een uitdaging
Gebruik de volgende attributen van OurGrid om je automatiseringsregels in te stellen:

| Asset     | Attribute                        | Gebruik                                                  |
| --------- | -------------------------------- | -------------------------------------------------------- |
| Challenge | `challengeStart`, `challengeEnd` | Wanneer de uitdaging plaatsvindt                         |
| Challenge | `challengePointsExchangeRate`    | Hoeveel je punten waard zijn (€)                         |
| Meter     | `challengeStatus`                | Geeft aan of er een actieve uitdaging is                 |
| Meter     | `challengePowerLimit`            | Maximaal toegestaan vermogen (kW)                        |
| Meter     | `challengeJoinButton`            | Zet op `true` om automatisch mee te doen en te verdienen |
| Meter     | `power`                          | Energieverbruik van je woning                            |

Wanneer `challengeStatus` de waarde "joinChallenge" heeft, begint de challenge binnenkort. Verlaag dan je netto `power` verbruik tot onder de `challengePowerLimit` tussen `challengeStart` en `challengeEnd`. Zet `challengeJoinButton` op `true` om mee te doen aan de uitdaging en punten te verdienen.
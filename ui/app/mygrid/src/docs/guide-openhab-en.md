## Introduction

OurGrid notifies you when there's grid congestion in your area. By reducing your power use during these moments, you can earn points—and money. 
Connect openHAB and respond to challenges automatically. You'll receive all the information needed for your system to meet the power target and complete the challenge.

## Connect to openHAB

Before you start, please open the openHAB main user interface in a browser.

### 1. Install MQTT Binding add-on

1. Go to **Add-on Store** in the sidebar
2. Click on **Bindings**
3. In the search field that appears, type "MQTT"
4. The "MQTT Binding"" will show
5. Click on the **Install** button to install the add-on

### 2. Install RegEx Transformation add-on

1. Go to **Add-on Store** in the sidebar
2. Click on **Transformations**
3. In the search field that appears, type "RegEx"
4. The "RegEx Transformation"" will show
5. Click on the **Install** button to install the add-on

### 3. Add OurGrid Things

First create a connection to the OurGrid Broker:

1. Go to **Settings > Things** in in the sidebar
2. Click on the **Add** button in the bottom right corner
3. Click on **MQTT Binding**
4. Click on **MQTT Broker**
5. Enter the following details:
    ```
    - Thing ID: ourgrid
    - Label: OurGrid Broker
    - Broker Hostname/IP: {{hostname}}
    ```
6. Click on "Create Thing"
7. Select the "OurGrid Broker" Thing
8. Open the **Code** tab in the top right corner
9. Enter the following details:
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
10. Click on "Save" in the top right corner
11. Click on "Back"
12. The "OurGrid Broker" Thing should now be online

Next create an OurGrid Challenge Thing:

1. Go to **Settings > Things** in in the sidebar
2. Click on the **Add** button in the bottom right corner
3. Click on **MQTT Binding**
4. Click on **Generic MQTT Thing**
5. Enter the following details:
    ```
    - Thing ID: challenge
    - Label: OurGrid Challenge
    - Bridge: OurGrid Broker
    ```
6. Click on "Create Thing"
7. Select the "OurGrid Challenge" Thing
8. Open the **Code** tab in the top right corner
9. Enter the following details:
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
10. Click on "Save" in the top right corner
11. Click on "Back"
12. The "OurGrid Challenge" Thing should now be online

Next create an OurGrid Meter Thing:

1. Go to **Settings > Things** in in the sidebar
2. Click on the **Add** button in the bottom right corner
3. Click on **MQTT Binding**
4. Click on **Generic MQTT Thing**
5. Enter the following details:
    ```
    - Thing ID: meter
    - Label: OurGrid Meter
    - Bridge: OurGrid Broker
    ```
6. Click on "Create Thing"
7. Select the "OurGrid Meter" Thing
8. Open the **Code** tab in the top right corner
9. Enter the following details:
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
10. Click on "Save" in the top right corner
11. Click on "Back"
12. The "OurGrid Meter" Thing should now be online

### End of installation

The Things should now all be present in your openHAB instance. You can now add these to your Sitemaps or other parts of your openHAB instance.

## Responding to a challenge

Use the following Thing channels from OurGrid to set up your automation rules:

| Thing     | Channel               | Use                                     |
| --------- | --------------------- | --------------------------------------- |
| Challenge | `start`, `end`        | When the challenge runs                 |
| Challenge | `pointsExchangeRate`  | How much your points are worth (€)      |
| Meter     | `challengeStatus`     | Shows if there's an active challenge (noChallenge, joinChallenge, joinedChallenge, activeChallenge) |
| Meter     | `challengePowerLimit` | Max power allowed (kW) during challenge |
| Meter     | `joinButton`          | Set to `ON` to auto-join and earn       |
| Meter     | `power`               | Power consumption of your house         |

When `challengeStatus` is `joinChallenge`, a challenge will begin shortly.
Reduce your net `power` use below `challengePowerLimit` from `start` to `end`.
Set `joinButton` to `ON` to join the challenge and start earning points.

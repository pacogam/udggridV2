## Introduction
OurGrid notifies you when there’s grid congestion in your area. By reducing your power use during these moments, you can earn points—and money. 

Connect your Home Assistant and respond to challenges automatically. You'll receive all the information needed for your system to meet the power target and complete the challenge.

## Connect to Home Assistant
Before you start, please open the Home Assistant user interface in a browser.

### 1. Enable Advanced Mode

1. In Home Assistant, click on your user profile picture in the bottom left corner
2. Ensure that **Advanced Mode** is enabled in your user profile settings

### 2. Add MQTT Integration

1. Go to **Settings** in the Home Assistant interface
2. Click on **Devices & Services**
3. Look for the **Add Integration** button in the bottom right corner and click it
4. In the search field that appears, type "MQTT"
5. Click on the **MQTT** integration when it appears in the results

### 3. Configure the MQTT Integration

1. When prompted, select **Manually enter the MQTT broker connection details**
2. Enable the **Advanced options** toggle (this is required for proper configuration)
3. Enter the following connection details:
    ```
    - Broker: {{hostname}}
    - Port: 8883
    - Username: {{realm}}:{{client_id}}
    - Password: {{client_secret}}
    ```
4. Click **Submit** to proceed to advanced options configuration

### 4. Advanced MQTT Configuration

1. In the advanced options, you only have to set the following:
    ```
    - Client ID: {{client_id}}
    - Broker certificate validation: Auto
    ```
2. Click **Submit** to save the configuration

### 5. Install File Editor Addon

> **Note:** If you already have the File Editor addon installed and running, you can skip this section.

1. Go to **Settings** in the Home Assistant interface
2. Click on **Add-ons**
3. Click on **Add-on store** in the bottom right corner
4. Use the search field to find "File Editor"
5. Click on the **File Editor** addon when it appears
6. Click the **Install** button
7. Once installed you can toggle the **Show in sidebar** switch to the right
8. Click **Start** to ensure the addon is running

### 6. Add OurGrid Entities

1. Go to the **File Editor** addon in the sidebar
2. Click on the **Folder icon** in the top left corner of the file editor
3. Select the **configuration.yaml** file
4. Copy and paste the following section to your **configuration.yaml** file:
    ```yaml
    mqtt:
        sensor:
          # Meter sensors
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
              
          - name: "OurGrid Challenge Power Limit"
            state_topic: "{{realm}}/{{client_id}}/attributevalue/challengePowerLimit/{{meter_asset_id}}"
            unit_of_measurement: "W"
            device_class: "power"
            state_class: "measurement"
            suggested_display_precision: 0
            icon: "mdi:lightning-bolt"

          # Challenge sensors
          - name: "OurGrid Challenge Points Exchange Rate"
            state_topic: "{{realm}}/{{client_id}}/attributevalue/challengePointsExchangeRate/{{challenge_asset_id}}"
            state_class: "measurement"
            suggested_display_precision: 2
            icon: "mdi:currency-eur"

          - name: "OurGrid Challenge Start"
            state_topic: "{{realm}}/{{client_id}}/attributevalue/challengeStart/{{challenge_asset_id}}"
            icon: "mdi:flag-checkered"

          - name: "OurGrid Challenge End"
            state_topic: "{{realm}}/{{client_id}}/attributevalue/challengeEnd/{{challenge_asset_id}}"
            icon: "mdi:flag-checkered"

        switch:
          # Challenge button
          - name: "OurGrid Challenge Join Button"
            state_topic: "{{realm}}/{{client_id}}/attributevalue/challengeJoinButton/{{meter_asset_id}}"
            command_topic: "{{realm}}/{{client_id}}/writeattributevalue/challengeJoinButton/{{meter_asset_id}}"
            payload_on: "true"
            payload_off: "false"
            state_on: "true"
            state_off: "false"
            retain: true
    ```
5. Click on the **Save** button in the top right corner of the file editor
6. Go to **Developer Tools** in the left sidebar
7. Under the **YAML configuration reloading** section, click on the **ALL YAML CONFIGURATION** button
8. The entities should now be added to your Home Assistant instance.

Note: If you have any issues with the entities not being added, please check the logs for any errors. In some cases restarting Home Assistant might be required.

### End of installation
The entities should now all be present in your Home Assistant instance. You can now add these to your dashboards or other parts of your Home Assistant instance.

## Responding to a challenge

Use the following attributes from OurGrid to set up your automation rules:

| Asset     | Attribute                        | Use                                     |
| --------- | -------------------------------- | --------------------------------------- |
| Challenge | `challengeStart`, `challengeEnd` | When the challenge runs                 |
| Challenge | `challengePointsExchangeRate`    | How much your points are worth (€)      |
| Meter     | `challengeStatus`                | Shows if there’s an active challenge    |
| Meter     | `challengePowerLimit`            | Max power allowed (kW) during challenge |
| Meter     | `challengeJoinButton`            | Set to `true` to auto-join and earn     |
| Meter     | `power`                          | Power consumption of your house         |

When `challengeStatus` is "joinChallenge", a challenge will begin shortly. Reduce your net `power` use below `challengePowerLimit` from `challengeStart` to `challengeEnd`. Set `challengeJoinButton` to `true` to join the challenge and start earning points.



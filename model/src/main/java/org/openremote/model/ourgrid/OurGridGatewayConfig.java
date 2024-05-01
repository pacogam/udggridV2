package org.openremote.model.ourgrid;

public class OurGridGatewayConfig {

    /**
     * Location of the cities JSON file containing the cities that can be searched.
     * Should be in a hash format, such as;
     * <pre>
     * {
     *     "My City Name": {
     *         "lang": "en"
     *     },
     *     "Another City": {
     *         "lang": "nl"
     *     }
     * }
     * </pre>
     */
    protected String searchCitiesFile;

    /**
     * List of Cities with their respective URLs and realms the gateway should redirect to
     */
    protected OurGridGatewayCity[] cities;
}

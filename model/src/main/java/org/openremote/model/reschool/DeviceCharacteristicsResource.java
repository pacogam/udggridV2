package org.openremote.model.reschool;

import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.ws.rs.BeanParam;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.core.Response;
import org.openremote.model.http.RequestParams;

import java.io.Serializable;

@Tag(name = "Reschool Device")
@Path("reschool/device/characteristics")
public interface DeviceCharacteristicsResource {

    class SetCharacteristicsDetails implements Serializable {

        public DeviceCharacteristic[] characteristics;

        public SetCharacteristicsDetails() {

        }
        public SetCharacteristicsDetails(DeviceCharacteristic[] characteristics) {
            this.characteristics = characteristics;
        }
    }

    @GET
    DeviceCharacteristic[] getCharacteristics(@BeanParam RequestParams params);

    @POST
    Response setCharacteristics(@BeanParam RequestParams params, SetCharacteristicsDetails details);
}

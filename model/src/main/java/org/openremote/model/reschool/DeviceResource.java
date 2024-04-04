package org.openremote.model.reschool;

import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.ws.rs.BeanParam;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.DELETE;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import org.openremote.model.http.RequestParams;

import java.io.Serializable;

@Tag(name = "Reschool Device")
@Path("reschool/device")
public interface DeviceResource {

    class DeviceLinkDetails implements Serializable {
        public DeviceLinkDetails() {
        }

        public DeviceLinkDetails(String deviceName) {
            this.deviceName = deviceName;
        }

        public String deviceName;
    }


    @Path("link")
    @POST
    @Consumes(MediaType.APPLICATION_JSON)
    Response linkDevice(@BeanParam RequestParams requestParams, DeviceLinkDetails details);

    @Path("link")
    @DELETE
    @Consumes(MediaType.APPLICATION_JSON)
    Response removeDevice(@BeanParam RequestParams requestParams, DeviceLinkDetails details);
}

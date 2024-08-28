package org.openremote.model.reschool;

import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.ws.rs.*;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import org.openremote.model.asset.Asset;
import org.openremote.model.http.RequestParams;

import java.io.Serializable;

@Tag(name = "Reschool Device")
@Path("reschool/device/battery")
public interface DeviceBatteryResource {

    class AutomaticControlDetails implements Serializable {

        public String meterId;
        public boolean automaticControl;

        public AutomaticControlDetails() {

        }
        public AutomaticControlDetails(String meterId, boolean automaticControl) {
            this.meterId = meterId;
            this.automaticControl = automaticControl;
        }
    }

    class ActionButtonDetails implements Serializable {

        public String meterId;
        public boolean buttonState;

        public ActionButtonDetails() {

        }
        public ActionButtonDetails(String meterId, boolean buttonState) {
            this.meterId = meterId;
            this.buttonState = buttonState;
        }
    }

    @GET
    @Path("{meterId}")
    @Produces(MediaType.APPLICATION_JSON)
    Asset<?> getBattery(@BeanParam RequestParams params, @PathParam("meterId") String meterId);

    @POST
    @Path("automaticControl")
    @Consumes(MediaType.APPLICATION_JSON)
    Response automaticControl(@BeanParam RequestParams params, AutomaticControlDetails details);

    @POST
    @Path("actionButton")
    @Consumes(MediaType.APPLICATION_JSON)
    Response actionButton(@BeanParam RequestParams params, ActionButtonDetails details);
}

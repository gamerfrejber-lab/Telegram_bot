package com.company;

import javax.ws.rs.core.EntityTag;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.Variant;
import java.util.Date;
import java.util.List;

public class Request implements javax.ws.rs.core.Request {
    private final int id;
    private final Long requesterId;
    private final String requesterName;
    private final String targetName;

    public Request(int id, Long requesterId, String requesterName, String targetName) {
        this.id = id;
        this.requesterId = requesterId;
        this.requesterName = requesterName;
        this.targetName = targetName;
    }

    public int getId() {return id;}
    public Long getRequesterId() {return requesterId;}
    public String getRequesterName() {return requesterName;}
    public String getTargetName() {return targetName;}

    @Override
    public String getMethod() {
        return "";
    }

    @Override
    public Variant selectVariant(List<Variant> list) {
        return null;
    }

    @Override
    public Response.ResponseBuilder evaluatePreconditions(EntityTag entityTag) {
        return null;
    }

    @Override
    public Response.ResponseBuilder evaluatePreconditions(Date date) {
        return null;
    }

    @Override
    public Response.ResponseBuilder evaluatePreconditions(Date date, EntityTag entityTag) {
        return null;
    }

    @Override
    public Response.ResponseBuilder evaluatePreconditions() {
        return null;
    }
}

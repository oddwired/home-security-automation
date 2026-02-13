package com.kshem.homesecurity.server;

import fi.iki.elonen.NanoHTTPD;

public abstract class RequestHandler {
    public abstract String getEndpoint();
    public abstract NanoHTTPD.Response post(NanoHTTPD.IHTTPSession ihttpSession, String body);
    public abstract NanoHTTPD.Response get(NanoHTTPD.IHTTPSession ihttpSession);
}

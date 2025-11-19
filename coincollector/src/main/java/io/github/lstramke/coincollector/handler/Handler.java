package io.github.lstramke.coincollector.handler;

import com.sun.net.httpserver.HttpExchange;
import java.io.IOException;

public interface Handler {
    void handle(HttpExchange exchange) throws IOException;
}

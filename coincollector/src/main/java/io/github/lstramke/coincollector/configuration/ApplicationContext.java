package io.github.lstramke.coincollector.configuration;

import io.github.lstramke.coincollector.handler.GroupHandler;
import io.github.lstramke.coincollector.handler.LoginHandler;
import io.github.lstramke.coincollector.handler.RegistrationHandler;
import io.github.lstramke.coincollector.services.SessionManager;

public record ApplicationContext(
        SessionManager sessionManager,
        LoginHandler loginHandler,
        RegistrationHandler registrationHandler,
        GroupHandler groupHandler
    ) {}

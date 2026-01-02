package io.github.lstramke.coincollector.configuration;

import io.github.lstramke.coincollector.handler.CoinHandler;
import io.github.lstramke.coincollector.handler.CollectionHandler;
import io.github.lstramke.coincollector.handler.GroupHandler;
import io.github.lstramke.coincollector.handler.LoginHandler;
import io.github.lstramke.coincollector.handler.LogoutHandler;
import io.github.lstramke.coincollector.handler.RegistrationHandler;
import io.github.lstramke.coincollector.services.SessionManager;

public record ApplicationContext(
    SessionManager sessionManager,
    LoginHandler loginHandler,
    LogoutHandler logoutHandler,
    RegistrationHandler registrationHandler,
    GroupHandler groupHandler,
    CollectionHandler collectionHandler,
    CoinHandler coinHandler
) {}

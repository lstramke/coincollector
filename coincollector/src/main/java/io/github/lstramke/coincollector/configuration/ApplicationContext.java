package io.github.lstramke.coincollector.configuration;

import io.github.lstramke.coincollector.handler.CoinHandler;
import io.github.lstramke.coincollector.handler.CollectionHandler;
import io.github.lstramke.coincollector.handler.GroupHandler;
import io.github.lstramke.coincollector.handler.LoginHandler;
import io.github.lstramke.coincollector.handler.LogoutHandler;
import io.github.lstramke.coincollector.handler.RegistrationHandler;
import io.github.lstramke.coincollector.services.SessionManager;

/**
 * Application context record that holds all core application components.
 * Provides centralized access to session management and all HTTP request handlers.
 * This immutable container ensures consistent dependency injection across the application.
 *
 * @param sessionManager the service for managing user sessions
 * @param loginHandler the handler for user login requests
 * @param logoutHandler the handler for user logout requests
 * @param registrationHandler the handler for user registration requests
 * @param groupHandler the handler for collection group operations
 * @param collectionHandler the handler for collection operations
 * @param coinHandler the handler for coin operations
 */
public record ApplicationContext(
    SessionManager sessionManager,
    LoginHandler loginHandler,
    LogoutHandler logoutHandler,
    RegistrationHandler registrationHandler,
    GroupHandler groupHandler,
    CollectionHandler collectionHandler,
    CoinHandler coinHandler
) {}

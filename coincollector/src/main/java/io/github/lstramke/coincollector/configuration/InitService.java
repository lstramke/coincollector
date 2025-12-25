package io.github.lstramke.coincollector.configuration;

import java.util.List;

import javax.sql.DataSource;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.sqlite.SQLiteDataSource;

import io.github.lstramke.coincollector.exceptions.StorageInitializeException;
import io.github.lstramke.coincollector.handler.CoinHandler;
import io.github.lstramke.coincollector.handler.CollectionHandler;
import io.github.lstramke.coincollector.handler.GroupHandler;
import io.github.lstramke.coincollector.handler.LoginHandler;
import io.github.lstramke.coincollector.handler.RegistrationHandler;
import io.github.lstramke.coincollector.model.EuroCoinCollectionFactory;
import io.github.lstramke.coincollector.model.EuroCoinCollectionGroupFactory;
import io.github.lstramke.coincollector.model.EuroCoinFactory;
import io.github.lstramke.coincollector.model.UserFactory;
import io.github.lstramke.coincollector.repositories.sqlite.EuroCoinCollectionGroupSqliteRepository;
import io.github.lstramke.coincollector.repositories.sqlite.EuroCoinCollectionSqliteRepository;
import io.github.lstramke.coincollector.repositories.sqlite.EuroCoinSqliteRepository;
import io.github.lstramke.coincollector.repositories.sqlite.UserSqliteRepository;
import io.github.lstramke.coincollector.services.EuroCoinCollectionGroupStorageServiceImpl;
import io.github.lstramke.coincollector.services.EuroCoinCollectionStorageServiceImpl;
import io.github.lstramke.coincollector.services.EuroCoinStorageServiceImpl;
import io.github.lstramke.coincollector.services.SessionManager;
import io.github.lstramke.coincollector.services.SessionManagerImpl;
import io.github.lstramke.coincollector.services.UserStorageServiceImpl;
import tools.jackson.databind.ObjectMapper;

public class InitService {
    
    private static final Logger logger = LoggerFactory.getLogger(InitService.class);
    
    public static ApplicationContext initialize(String dbFilePath) throws StorageInitializeException {
        logger.info("Initializing application context...");
        
        // Database setup
        SQLiteDataSource dataSource = new SQLiteDataSource();
        dataSource.setUrl("jdbc:sqlite:" + dbFilePath);
        
        DataSource configuredDataSource = new DataSourceAutoActivateForeignKeys(dataSource);
        List<String> tableNames = List.of("users", "euroCoinCollectionGroups", "euroCoinCollections", "euroCoins");
        StorageInitializer storageInitializer = new SqliteInitializer(configuredDataSource, tableNames);
        
        storageInitializer.init();
        logger.info("Database initialized successfully");
        
        SessionManager sessionManager = new SessionManagerImpl();
        
        var userFactory = new UserFactory();
        var groupFactory = new EuroCoinCollectionGroupFactory();
        var collectionFactory = new EuroCoinCollectionFactory();
        var coinFactory = new EuroCoinFactory();
        
        var userStorageRepository = new UserSqliteRepository(tableNames.get(0), userFactory);
        var groupStorageRepository = new EuroCoinCollectionGroupSqliteRepository(tableNames.get(1), groupFactory);
        var collectionStorageRepository = new EuroCoinCollectionSqliteRepository(tableNames.get(2), collectionFactory);
        var coinStorageRepository = new EuroCoinSqliteRepository(tableNames.get(3), coinFactory);
        
        var userStorageService = new UserStorageServiceImpl(userStorageRepository, configuredDataSource);
        var coinStorageService = new EuroCoinStorageServiceImpl(coinStorageRepository, configuredDataSource);
        var collectionStorageService = new EuroCoinCollectionStorageServiceImpl(configuredDataSource, collectionStorageRepository, coinStorageService);
        var groupStorageService = new EuroCoinCollectionGroupStorageServiceImpl(configuredDataSource, groupStorageRepository, collectionStorageService);
        
        var mapper = new ObjectMapper();
        var loginHandler = new LoginHandler(userStorageService, sessionManager);
        var registrationHandler = new RegistrationHandler(userStorageService, sessionManager);
        var groupHandler = new GroupHandler(groupStorageService, mapper);
        var collectionHandler = new CollectionHandler(collectionStorageService, groupStorageService);
        var coinHandler = new CoinHandler(coinStorageService, collectionStorageService, groupStorageService);
        
        logger.info("Application context initialized successfully");
        
        return new ApplicationContext(sessionManager, loginHandler, registrationHandler, groupHandler, collectionHandler, coinHandler);
    }
}
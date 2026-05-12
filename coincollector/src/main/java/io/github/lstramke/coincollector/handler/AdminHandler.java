package io.github.lstramke.coincollector.handler;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.SpringApplication;
import org.springframework.context.ApplicationContext;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("api/v1")
public class AdminHandler {

    private static final Logger logger = LoggerFactory.getLogger(AdminHandler.class);
    private final ApplicationContext context;

    @Autowired
    public AdminHandler(ApplicationContext context) {
        this.context = context;
    }
    
    @PostMapping("/shutdown")
    public ResponseEntity<Void> shutdown(){
        logger.info("Shutdown requestet via /api/v1/shutdown");

        new Thread(() -> {
            try {
                Thread.sleep(100);
            } catch (InterruptedException ignored) {}
            SpringApplication.exit(context, () -> 0);
        }).start();
        return ResponseEntity.ok().build();
    }
}

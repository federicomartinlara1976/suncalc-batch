package net.bounceme.chronos.suncalc.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

import lombok.Getter;
import lombok.Setter;

@Configuration
@ConfigurationProperties(prefix = "spring.datasource.batch")
public class BatchProperties {
    
	@Getter
    @Setter
    private int minPoolSize;
    
	@Getter
    @Setter
    private int maxPoolSize;
    
	@Getter
    @Setter
    private int maxIdleTime;
    
	@Getter
    @Setter
    private String driverClass;
    
	@Getter
    @Setter
    private String password;
    
	@Getter
    @Setter
    private String user;
    
	@Getter
    @Setter
    private String jdbcUrl;
}
package net.bounceme.chronos.suncalc.config;

import java.beans.PropertyVetoException;

import javax.sql.DataSource;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;

import com.mchange.v2.c3p0.ComboPooledDataSource;

@Configuration
public class BatchDataSourceConfiguration {

	@Bean
	@Primary
	public DataSource dataSource(@Autowired BatchProperties c3P0Properties) throws PropertyVetoException {
		ComboPooledDataSource pooledDataSource = new ComboPooledDataSource();
		pooledDataSource.setDriverClass(c3P0Properties.getDriverClass());
		pooledDataSource.setUser(c3P0Properties.getUser());
		pooledDataSource.setPassword(c3P0Properties.getPassword());
		pooledDataSource.setJdbcUrl(c3P0Properties.getJdbcUrl());
		pooledDataSource.setMaxPoolSize(c3P0Properties.getMaxPoolSize());
		pooledDataSource.setMaxIdleTime(c3P0Properties.getMaxIdleTime());

		return pooledDataSource;
	}
}

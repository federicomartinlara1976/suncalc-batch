package net.bounceme.chronos.suncalc.repository;

import org.springframework.data.mongodb.repository.MongoRepository;

import net.bounceme.chronos.suncalc.model.TimeData;

public interface TimeDataRepository extends MongoRepository<TimeData, String> {

}
package net.bounceme.chronos.suncalc.repository;

import org.springframework.data.mongodb.repository.MongoRepository;

import net.bounceme.chronos.suncalc.model.Differences;

public interface DifferencesDataRepository extends MongoRepository<Differences, String> {

}
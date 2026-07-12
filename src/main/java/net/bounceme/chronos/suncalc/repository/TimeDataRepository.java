package net.bounceme.chronos.suncalc.repository;

import java.util.List;

import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.data.mongodb.repository.Query;

import net.bounceme.chronos.suncalc.model.TimeData;

public interface TimeDataRepository extends MongoRepository<TimeData, String> {

	@Query("{ $and : [ { '_id' : { $gte : ?0 } }, { '_id' : { $lte : ?1 } } ] }")
	List<TimeData> listRegistros(String from, String to);
}
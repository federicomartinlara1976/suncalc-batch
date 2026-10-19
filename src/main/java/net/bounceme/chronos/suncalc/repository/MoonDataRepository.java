package net.bounceme.chronos.suncalc.repository;

import java.util.List;

import org.springframework.data.domain.Sort;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.data.mongodb.repository.Query;

import net.bounceme.chronos.suncalc.model.MoonData;

public interface MoonDataRepository extends MongoRepository<MoonData, String> {

	@Query("{ $and : [ { '_id' : { $gte : ?0 } }, { '_id' : { $lte : ?1 } } ] }")
	List<MoonData> listRegistros(String from, String to, Sort sort);
}
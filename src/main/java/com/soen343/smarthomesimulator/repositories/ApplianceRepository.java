package com.soen343.smarthomesimulator.repositories;

import com.soen343.smarthomesimulator.models.Appliance;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.CrudRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface ApplianceRepository extends CrudRepository<Appliance, Long> {
}

package io.appnaut.poc.data;

import io.micronaut.data.annotation.Repository;
import io.micronaut.data.jpa.repository.JpaRepository;

@Repository
public interface DataRepository extends JpaRepository<Data, Long> {

}

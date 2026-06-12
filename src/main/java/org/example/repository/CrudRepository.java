package org.example.repository;

import java.util.List;
import java.util.Optional;

public interface CrudRepository<T, ID> {

    void save(T entity);

    Optional<T> findById(ID id);

    List<T> findAll();

    void update(T entity);

    void deleteById(ID id);
}

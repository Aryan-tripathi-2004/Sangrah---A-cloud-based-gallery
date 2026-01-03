package com.example.Auth.dao;

import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.data.mongodb.core.query.Update;
import org.springframework.beans.factory.annotation.Autowired;

import java.util.List;
import java.util.Optional;

/**
 * Base DAO class providing common database operations.
 * All DAOs should extend this class to inherit common functionality.
 * This layer sits between Service and Repository, handling:
 * - Entity to Model transformations
 * - Complex queries using MongoTemplate
 * - Update operations
 * - Data validation and pre/post-processing
 *
 * @param <E>  Entity type (database model)
 * @param <M>  Business Model type (domain model)
 * @param <ID> ID type
 */
public abstract class BaseDao<E, M, ID> {

    @Autowired
    protected MongoTemplate mongoTemplate;

    /**
     * Get the entity class type.
     *
     * @return the entity class
     */
    protected abstract Class<E> getEntityClass();

    /**
     * Convert entity to business model.
     *
     * @param entity the entity to convert
     * @return the business model
     */
    protected abstract M toModel(E entity);

    /**
     * Convert business model to entity.
     *
     * @param model the business model to convert
     * @return the entity
     */
    protected abstract E toEntity(M model);

    /**
     * Find entity by ID and convert to model.
     *
     * @param id the entity ID
     * @return Optional containing the model if found
     */
    public Optional<M> findById(ID id) {
        E entity = mongoTemplate.findById(id, getEntityClass());
        return Optional.ofNullable(entity).map(this::toModel);
    }

    /**
     * Find all entities and convert to models.
     *
     * @return list of business models
     */
    public List<M> findAll() {
        List<E> entities = mongoTemplate.findAll(getEntityClass());
        return entities.stream()
                .map(this::toModel)
                .toList();
    }

    /**
     * Save a business model (converts to entity, saves, converts back).
     *
     * @param model the business model to save
     * @return the saved business model
     */
    public M save(M model) {
        E entity = toEntity(model);
        E savedEntity = mongoTemplate.save(entity);
        return toModel(savedEntity);
    }

    /**
     * Delete entity by ID.
     *
     * @param id the entity ID
     */
    public void deleteById(ID id) {
        Query query = new Query();
        query.addCriteria(org.springframework.data.mongodb.core.query.Criteria.where("_id").is(id));
        mongoTemplate.remove(query, getEntityClass());
    }

    /**
     * Update entity using query and update objects.
     *
     * @param query  the query to find entities
     * @param update the update operations
     * @return number of modified documents
     */
    protected long update(Query query, Update update) {
        var result = mongoTemplate.updateMulti(query, update, getEntityClass());
        return result.getModifiedCount();
    }

    /**
     * Update a single entity.
     *
     * @param query  the query to find the entity
     * @param update the update operations
     * @return true if entity was updated
     */
    protected boolean updateOne(Query query, Update update) {
        var result = mongoTemplate.updateFirst(query, update, getEntityClass());
        return result.getModifiedCount() > 0;
    }

    /**
     * Count documents matching query.
     *
     * @param query the query
     * @return count of matching documents
     */
    protected long count(Query query) {
        return mongoTemplate.count(query, getEntityClass());
    }

    /**
     * Check if document exists matching query.
     *
     * @param query the query
     * @return true if document exists
     */
    protected boolean exists(Query query) {
        return mongoTemplate.exists(query, getEntityClass());
    }

    /**
     * Find entities matching query and convert to models.
     *
     * @param query the query
     * @return list of business models
     */
    protected List<M> find(Query query) {
        List<E> entities = mongoTemplate.find(query, getEntityClass());
        return entities.stream()
                .map(this::toModel)
                .toList();
    }

    /**
     * Find one entity matching query and convert to model.
     *
     * @param query the query
     * @return Optional containing the model if found
     */
    protected Optional<M> findOne(Query query) {
        E entity = mongoTemplate.findOne(query, getEntityClass());
        return Optional.ofNullable(entity).map(this::toModel);
    }
}

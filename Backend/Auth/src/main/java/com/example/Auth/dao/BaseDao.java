package com.example.Auth.dao;

import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import jakarta.persistence.TypedQuery;
import jakarta.persistence.criteria.CriteriaBuilder;
import jakarta.persistence.criteria.CriteriaQuery;
import jakarta.persistence.criteria.Root;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

/**
 * Base DAO class providing common database operations.
 * All DAOs should extend this class to inherit common functionality.
 * This layer sits between Service and Repository, handling:
 * - Entity to Model transformations
 * - Complex queries using EntityManager
 * - Update operations
 * - Data validation and pre/post-processing
 *
 * @param <E>  Entity type (database model)
 * @param <M>  Business Model type (domain model)
 * @param <ID> ID type
 */
public abstract class BaseDao<E, M, ID> {

    @PersistenceContext
    protected EntityManager entityManager;

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
        E entity = entityManager.find(getEntityClass(), id);
        return Optional.ofNullable(entity).map(this::toModel);
    }

    /**
     * Find all entities and convert to models.
     *
     * @return list of business models
     */
    public List<M> findAll() {
        CriteriaBuilder cb = entityManager.getCriteriaBuilder();
        CriteriaQuery<E> cq = cb.createQuery(getEntityClass());
        Root<E> root = cq.from(getEntityClass());
        cq.select(root);

        List<E> entities = entityManager.createQuery(cq).getResultList();
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
    @Transactional
    public M save(M model) {
        E entity = toEntity(model);
        E savedEntity = entityManager.merge(entity);
        entityManager.flush();
        return toModel(savedEntity);
    }

    /**
     * Delete entity by ID.
     *
     * @param id the entity ID
     */
    @Transactional
    public void deleteById(ID id) {
        E entity = entityManager.find(getEntityClass(), id);
        if (entity != null) {
            entityManager.remove(entity);
        }
    }

    /**
     * Count all entities.
     *
     * @return count of entities
     */
    protected long count() {
        CriteriaBuilder cb = entityManager.getCriteriaBuilder();
        CriteriaQuery<Long> cq = cb.createQuery(Long.class);
        Root<E> root = cq.from(getEntityClass());
        cq.select(cb.count(root));
        return entityManager.createQuery(cq).getSingleResult();
    }

    /**
     * Execute a JPQL query and return results as models.
     *
     * @param jpql the JPQL query string
     * @return list of business models
     */
    protected List<M> executeQuery(String jpql) {
        TypedQuery<E> query = entityManager.createQuery(jpql, getEntityClass());
        List<E> entities = query.getResultList();
        return entities.stream()
                .map(this::toModel)
                .toList();
    }

    /**
     * Execute a JPQL query and return a single result as model.
     *
     * @param jpql the JPQL query string
     * @return Optional containing the model if found
     */
    protected Optional<M> executeQuerySingle(String jpql) {
        TypedQuery<E> query = entityManager.createQuery(jpql, getEntityClass());
        query.setMaxResults(1);
        List<E> results = query.getResultList();
        return results.isEmpty() ? Optional.empty() : Optional.of(toModel(results.get(0)));
    }

    /**
     * Refresh entity state from database.
     *
     * @param entity the entity to refresh
     */
    protected void refresh(E entity) {
        if (entityManager.contains(entity)) {
            entityManager.refresh(entity);
        }
    }

    /**
     * Flush pending changes to database.
     */
    @Transactional
    protected void flush() {
        entityManager.flush();
    }
}

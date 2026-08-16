package com.dietapp.diet_app.health_profile.service;

import com.dietapp.diet_app.health_profile.entity.HealthProfile;
import com.dietapp.diet_app.health_profile.repository.HealthProfileRepository;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;

import java.util.Optional;
import java.util.UUID;

@RequiredArgsConstructor
public abstract class AbstractHealthProfileSectionService<
        ENTITY,
        REQUEST,
        RESPONSE> {

    protected final HealthProfileRepository healthProfileRepository;

    /**
     * Find section by health profile id.
     */
    protected abstract Optional<ENTITY> findByHealthProfileId(
            UUID healthProfileId
    );

    /**
     * Create new entity from request.
     */
    protected abstract ENTITY createEntity(
            REQUEST request
    );

    /**
     * Attach Health Profile.
     */
    protected abstract void setHealthProfile(
            ENTITY entity,
            HealthProfile healthProfile
    );

    /**
     * Update existing entity.
     */
    protected abstract void updateEntity(
            REQUEST request,
            ENTITY entity
    );

    /**
     * Save entity.
     */
    protected abstract ENTITY save(
            ENTITY entity
    );

    /**
     * Convert entity to response.
     */
    protected abstract RESPONSE toResponse(
            ENTITY entity
    );

    /**
     * Shared save/update algorithm.
     */
    protected RESPONSE saveOrUpdateInternal(
            UUID healthProfileId,
            REQUEST request
    ) {
        // find health profile by id, if not found throw exception
        HealthProfile healthProfile =
                healthProfileRepository
                        .findById(healthProfileId)
                        .orElseThrow(() ->
                                new EntityNotFoundException(
                                        "Health Profile not found."
                                ));

        // find the particular section by health profile id, if not found create a new one
        ENTITY entity =
                findByHealthProfileId(healthProfileId)
                        .orElseGet(() -> {

                            ENTITY e = createEntity(request);

                            setHealthProfile(e, healthProfile);

                            return e;

                        });

        updateEntity(request, entity);

        entity = save(entity);

        return toResponse(entity);

    }

}
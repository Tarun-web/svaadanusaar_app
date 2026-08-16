package com.dietapp.diet_app.health_profile.service.PersonalProfile;

import com.dietapp.diet_app.common.exception.EntityNotFoundException;
import com.dietapp.diet_app.common.security.AuthenticationFacade;
import com.dietapp.diet_app.health_profile.dto.request.PersonalProfileRequest;
import com.dietapp.diet_app.health_profile.dto.response.PersonalProfileResponse;
import com.dietapp.diet_app.health_profile.entity.HealthProfile;
import com.dietapp.diet_app.health_profile.entity.PersonalProfile;
import com.dietapp.diet_app.health_profile.mapper.PersonalProfileMapper;
import com.dietapp.diet_app.health_profile.repository.HealthProfileRepository;
import com.dietapp.diet_app.health_profile.repository.PersonalProfileRepository;
import com.dietapp.diet_app.health_profile.service.HealthProfileContext.HealthProfileContextService;
import com.dietapp.diet_app.health_profile.service.ProfileCompletion.ProfileCompletionService;
import org.springframework.transaction.annotation.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.Optional;
import java.util.UUID;

@Service
@Transactional
@RequiredArgsConstructor
public class PersonalProfileServiceImpl implements PersonalProfileService {

    private final PersonalProfileRepository personalProfileRepository;

    private final HealthProfileRepository healthProfileRepository;

    private final PersonalProfileMapper personalProfileMapper;

    private final HealthProfileContextService healthProfileContextService;

    private final ProfileCompletionService profileCompletionService;


    // Extract authenticated user from JWT token
    private final AuthenticationFacade authenticationFacade;


    @Override
    public PersonalProfileResponse saveOrUpdate(PersonalProfileRequest request) {

        // Extract current user from JWT token
        UUID userId = authenticationFacade.getCurrentUserId();

        /*
         * Get the HealthProfile belonging to the
         * user authenticated through JWT.
         */
        HealthProfile healthProfile =
                healthProfileContextService
                        .getCurrentUserHealthProfile();

        /*
         * Check whether a PersonalProfile already exists
         * for this HealthProfile.
         */
        PersonalProfile personalProfile =
                personalProfileRepository
                        .findByHealthProfileId(
                                healthProfile.getId()
                        )
                        .orElse(null);

        /*
         * CREATE
         */
        if (personalProfile == null) {

            personalProfile =
                    personalProfileMapper.toEntity(request);

            /*
             * Establish the relationship.
             */
            personalProfile.setHealthProfile(
                    healthProfile
            );

        }
        /*
         * UPDATE
         */
        else {

            personalProfileMapper.updateEntity(
                    request,
                    personalProfile
            );
        }



        /*
         * Save PersonalProfile.
         */
        personalProfile =
                personalProfileRepository.save(
                        personalProfile
                );

        /*
         * Recalculate overall HealthProfile completion.
         */
        profileCompletionService
                .refreshProfileCompletion(
                        healthProfile.getId()
                );

        return personalProfileMapper.toResponse(personalProfile);
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<PersonalProfileResponse> getMyProfile() {

        // check if exist else return exception
        HealthProfile healthProfile =
                healthProfileContextService
                        .getCurrentUserHealthProfile();

        // return the personal profile if it exists, else return empty optional
        return personalProfileRepository
                .findByHealthProfileId(
                        healthProfile.getId()
                )
                .map(personalProfileMapper::toResponse);
    }
}

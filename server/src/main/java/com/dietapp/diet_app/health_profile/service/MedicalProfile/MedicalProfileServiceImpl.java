package com.dietapp.diet_app.health_profile.service.MedicalProfile;

import com.dietapp.diet_app.health_profile.dto.request.MedicalProfileRequest;
import com.dietapp.diet_app.health_profile.dto.response.MedicalProfileResponse;
import com.dietapp.diet_app.health_profile.entity.HealthProfile;
import com.dietapp.diet_app.health_profile.entity.MedicalProfile;
import com.dietapp.diet_app.health_profile.mapper.MedicalProfileMapper;
import com.dietapp.diet_app.health_profile.repository.HealthProfileRepository;
import com.dietapp.diet_app.health_profile.repository.MedicalProfileRepository;
import com.dietapp.diet_app.health_profile.service.HealthProfileContext.HealthProfileContextService;
import com.dietapp.diet_app.health_profile.service.ProfileCompletion.ProfileCompletionService;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Transactional
public class MedicalProfileServiceImpl
        implements MedicalProfileService {

    private final MedicalProfileRepository medicalProfileRepository;
    private final ProfileCompletionService  profileCompletionService;
    private final MedicalProfileMapper medicalProfileMapper;
    private final HealthProfileContextService
    healthProfileContextService;

    @Override
    public MedicalProfileResponse saveOrUpdate(
            MedicalProfileRequest request
    ) {

        HealthProfile healthProfile = healthProfileContextService
                .getCurrentUserHealthProfile();

        MedicalProfile medicalProfile =
                medicalProfileRepository
                        .findByHealthProfileId(healthProfile.getId())
                        .orElse(null);

        // create
        if(medicalProfile == null) {
            medicalProfile = medicalProfileMapper.toEntity(request);
            medicalProfile.setHealthProfile(healthProfile);
        }
        // update
        else{
            medicalProfileMapper.updateEntity(request, medicalProfile);
        }



        medicalProfile = medicalProfileRepository.save(medicalProfile);

        /*
         * Recalculate overall HealthProfile completion.
         */
        profileCompletionService.refreshProfileCompletion(
                healthProfile.getId()
        );

        return medicalProfileMapper.toResponse(medicalProfile);
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<MedicalProfileResponse> getMyProfile(
    ) {
        HealthProfile healthProfile = healthProfileContextService.getCurrentUserHealthProfile();

        return medicalProfileRepository.findByHealthProfileId(healthProfile.getId())
                .map(medicalProfileMapper::toResponse);
    }
}
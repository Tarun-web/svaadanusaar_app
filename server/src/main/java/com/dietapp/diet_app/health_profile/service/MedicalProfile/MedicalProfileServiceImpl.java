package com.dietapp.diet_app.health_profile.service.MedicalProfile;

import com.dietapp.diet_app.health_profile.dto.request.MedicalProfileRequest;
import com.dietapp.diet_app.health_profile.dto.response.MedicalProfileResponse;
import com.dietapp.diet_app.health_profile.entity.HealthProfile;
import com.dietapp.diet_app.health_profile.entity.MedicalProfile;
import com.dietapp.diet_app.health_profile.mapper.MedicalProfileMapper;
import com.dietapp.diet_app.health_profile.repository.HealthProfileRepository;
import com.dietapp.diet_app.health_profile.repository.MedicalProfileRepository;
import com.dietapp.diet_app.health_profile.service.HealthProfileContext.HealthProfileContextService;
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
    private final HealthProfileRepository healthProfileRepository;
    private final MedicalProfileMapper medicalProfileMapper;
    private final HealthProfileContextService
    healthProfileContextService;

    @Override
    public MedicalProfileResponse saveOrUpdate(
            MedicalProfileRequest request
    ) {

        HealthProfile healthProfile = healthProfileRepository
                .findById(request.getHealthProfileId())
                .orElseThrow(() ->
                        new EntityNotFoundException("Health Profile not found."));

        MedicalProfile medicalProfile =
                medicalProfileRepository
                        .findByHealthProfileId(request.getHealthProfileId())
                        .orElseGet(() -> {

                            MedicalProfile entity =
                                    medicalProfileMapper.toEntity(request);

                            entity.setHealthProfile(healthProfile);

                            return entity;
                        });

        if (medicalProfile.getId() != null) {
            medicalProfileMapper.updateEntity(request, medicalProfile);
        }

        medicalProfile = medicalProfileRepository.save(medicalProfile);

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
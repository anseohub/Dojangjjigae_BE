package com.dojangjjigae.service;

import com.dojangjjigae.config.AppConstants;
import com.dojangjjigae.domain.Pet;
import com.dojangjjigae.dto.PetCreateRequest;
import com.dojangjjigae.dto.PetDto;
import com.dojangjjigae.repository.PetRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.NoSuchElementException;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class PetService {

    private final PetRepository petRepository;

    public List<PetDto> listMyPets() {
        return petRepository.findByUserId(AppConstants.TEST_USER_ID)
                .stream().map(PetDto::from).toList();
    }

    public PetDto getById(Long id) {
        return PetDto.from(findOwnedPet(id));
    }

    @Transactional
    public PetDto create(PetCreateRequest request) {
        Pet pet = Pet.builder()
                .userId(AppConstants.TEST_USER_ID)
                .name(request.getName())
                .photoUrl(request.getPhotoUrl())
                .breed(request.getBreed())
                .birthYear(request.getBirthYear())
                .birthMonth(request.getBirthMonth())
                .size(request.getSize())
                .female(request.isFemale())
                .neutered(request.isNeutered())
                .temperamentTags(request.getTemperamentTags())
                .memo(request.getMemo())
                .build();
        return PetDto.from(petRepository.save(pet));
    }

    @Transactional
    public PetDto update(Long id, PetCreateRequest request) {
        Pet pet = findOwnedPet(id);
        pet.setName(request.getName());
        pet.setPhotoUrl(request.getPhotoUrl());
        pet.setBreed(request.getBreed());
        pet.setBirthYear(request.getBirthYear());
        pet.setBirthMonth(request.getBirthMonth());
        pet.setSize(request.getSize());
        pet.setFemale(request.isFemale());
        pet.setNeutered(request.isNeutered());
        pet.setTemperamentTags(request.getTemperamentTags());
        pet.setMemo(request.getMemo());
        return PetDto.from(pet); // 같은 트랜잭션 안이라 dirty checking 으로 자동 반영
    }

    private Pet findOwnedPet(Long id) {
        Pet pet = petRepository.findById(id)
                .orElseThrow(() -> new NoSuchElementException("반려동물을 찾을 수 없습니다. id=" + id));
        if (!pet.getUserId().equals(AppConstants.TEST_USER_ID)) {
            throw new NoSuchElementException("반려동물을 찾을 수 없습니다. id=" + id);
        }
        return pet;
    }
}

package com.dojangjjigae.dto;

import com.dojangjjigae.domain.Pet;
import com.dojangjjigae.domain.PetSize;
import lombok.Builder;
import lombok.Getter;

import java.time.LocalDate;
import java.util.List;

@Getter
@Builder
public class PetDto {

    private Long id;
    private String name;
    private String photoUrl;
    private String breed;
    private Integer birthYear;
    private Integer birthMonth;
    private String size;       // SMALL | MEDIUM | LARGE
    private String sizeLabel;  // "중형견" 처럼 화면 표시용
    private String ageLabel;   // "만 3세" 처럼 화면 표시용
    private boolean female;
    private boolean neutered;
    private List<String> temperamentTags;
    private String memo;

    public static PetDto from(Pet pet) {
        return PetDto.builder()
                .id(pet.getId())
                .name(pet.getName())
                .photoUrl(pet.getPhotoUrl())
                .breed(pet.getBreed())
                .birthYear(pet.getBirthYear())
                .birthMonth(pet.getBirthMonth())
                .size(pet.getSize() != null ? pet.getSize().name() : null)
                .sizeLabel(sizeLabel(pet.getSize()))
                .ageLabel(ageLabel(pet.getBirthYear()))
                .female(pet.isFemale())
                .neutered(pet.isNeutered())
                .temperamentTags(pet.getTemperamentTags())
                .memo(pet.getMemo())
                .build();
    }

    private static String sizeLabel(PetSize size) {
        if (size == null) return null;
        return switch (size) {
            case SMALL -> "소형견";
            case MEDIUM -> "중형견";
            case LARGE -> "대형견";
        };
    }

    private static String ageLabel(Integer birthYear) {
        if (birthYear == null) return null;
        int age = LocalDate.now().getYear() - birthYear + 1; // 한국식 만 나이 근사치
        return "만 " + age + "세";
    }
}

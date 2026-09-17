package com.dojangjjigae.dto;

import com.dojangjjigae.domain.PetSize;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

import java.util.List;

@Getter
@Setter
public class PetCreateRequest {

    @NotBlank(message = "이름은 필수입니다.")
    private String name;

    private String photoUrl;
    private String breed;
    private Integer birthYear;
    private Integer birthMonth;

    @NotNull(message = "크기는 필수입니다.")
    private PetSize size;

    private boolean female;
    private boolean neutered;
    private List<String> temperamentTags;

    @Size(max = 200, message = "메모는 200자 이내로 작성해주세요.")
    private String memo;
}

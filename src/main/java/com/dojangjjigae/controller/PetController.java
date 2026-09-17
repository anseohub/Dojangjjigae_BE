package com.dojangjjigae.controller;

import com.dojangjjigae.dto.PetCreateRequest;
import com.dojangjjigae.dto.PetDto;
import com.dojangjjigae.service.PetService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/pets")
@RequiredArgsConstructor
public class PetController {

    private final PetService petService;

    /** Profile.jsx 의 "등록된 반려동물" 목록 */
    @GetMapping
    public List<PetDto> list() {
        return petService.listMyPets();
    }

    @GetMapping("/{id}")
    public PetDto getById(@PathVariable Long id) {
        return petService.getById(id);
    }

    /** Profile.jsx 의 등록 폼 제출 */
    @PostMapping
    public PetDto create(@Valid @RequestBody PetCreateRequest request) {
        return petService.create(request);
    }

    @PutMapping("/{id}")
    public PetDto update(@PathVariable Long id, @Valid @RequestBody PetCreateRequest request) {
        return petService.update(id, request);
    }
}

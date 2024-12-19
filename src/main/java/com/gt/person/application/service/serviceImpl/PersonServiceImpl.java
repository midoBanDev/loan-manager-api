package com.gt.person.application.service.serviceImpl;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.gt.global.common.exception.EntityNotSaveException;
import com.gt.person.api.dto.PersonCreateRequest;
import com.gt.person.application.service.PersonService;
import com.gt.person.domain.entity.Person;
import com.gt.person.domain.repository.PersonRepository;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Service
@RequiredArgsConstructor
public class PersonServiceImpl implements PersonService {
    
    private final PersonRepository personRepository;

    @Override
    @Transactional
    public Long createPerson(PersonCreateRequest createRequest) {
        try {
            Person savedPerson = personRepository.save(Person.createPerson(createRequest));
            log.info("Person created successfully with ID: {}", savedPerson.getId());
            return savedPerson.convertToCreateResponseDto().getId();
        } catch (Exception e) {
            log.error("Failed to create person: {}", e.getMessage());
            throw new EntityNotSaveException("Person creation failed", e);
        }
    }

}

package com.gt.person.application.service;

import com.gt.person.api.dto.PersonCreateRequest;

public interface PersonService {
    Long createPerson(PersonCreateRequest createRequest);
}

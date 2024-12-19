package com.gt.person.domain.repository;

import org.springframework.data.jpa.repository.JpaRepository;

import com.gt.person.domain.entity.Person;

public interface PersonRepository extends JpaRepository<Person, Long> {

}

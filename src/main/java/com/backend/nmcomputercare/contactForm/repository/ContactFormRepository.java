package com.backend.nmcomputercare.contactForm.repository;

import com.backend.nmcomputercare.contactForm.entity.ContactForm;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;


@Repository
public interface ContactFormRepository extends JpaRepository<ContactForm,Long> {

    List<ContactForm> findByNumbers(String numbers);

    List<ContactForm> findByEmail(String email);
}



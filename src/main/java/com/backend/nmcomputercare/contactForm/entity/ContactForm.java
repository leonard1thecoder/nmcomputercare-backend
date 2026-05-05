package com.backend.nmcomputercare.contactForm.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@AllArgsConstructor
@NoArgsConstructor
@Data
@Builder
@Entity
@Table(name = "customer_requests")
public class ContactForm {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id; // Unique numeric ID, auto-generated

    @Column(nullable = false)
    private String name;

    @Column(nullable = false)
    private String email;

    @Column(nullable = false)
    private String numbers;

    @Column(nullable = false)
    private String service;

    @Column(nullable = false)
    private String message;

    @Column(nullable = false)
    private LocalDateTime sentDate;

    @Column(nullable = false)
    private Byte status;
}
package com.example.Billing.application.service.interfaces;

/**
 * Service contract for distributed sequence generation.
 * Adheres to Dependency Inversion Principle (DIP).
 */
public interface ISequenceGeneratorService {
    long generateSequence(String seqName);
}

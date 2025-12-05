package com.thiefspin.bookingsystem;

import com.thiefspin.bookingsystem.appointments.AppointmentEntity;
import com.thiefspin.bookingsystem.appointments.AppointmentRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.RepeatedTest;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;
import java.util.HashSet;
import java.util.Set;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("BookingReferenceGenerator Tests")
class BookingReferenceGeneratorTest {

    @Mock
    private AppointmentRepository repository;

    @InjectMocks
    private BookingReferenceGenerator generator;

    @BeforeEach
    void setUp() {
        reset(repository);
    }

    @Test
    @DisplayName("Should generate unique reference with correct format")
    void shouldGenerateUniqueReferenceWithCorrectFormat() {
        // Given
        when(repository.findByBookingReference(anyString())).thenReturn(Optional.empty());

        // When
        String reference = generator.generate();

        // Then
        assertThat(reference).isNotNull();
        assertThat(reference).startsWith("BK");
        assertThat(reference).hasSize(10);
        assertThat(reference.substring(2)).matches("[A-Z0-9]{8}");

        verify(repository, atLeastOnce()).findByBookingReference(anyString());
    }

    @Test
    @DisplayName("Should retry when reference already exists")
    void shouldRetryWhenReferenceAlreadyExists() {
        AppointmentEntity existingAppointment = mock(AppointmentEntity.class);
        when(repository.findByBookingReference(anyString()))
            .thenReturn(Optional.of(existingAppointment))
            .thenReturn(Optional.of(existingAppointment))
            .thenReturn(Optional.empty());

        // When
        String reference = generator.generate();

        // Then
        assertThat(reference).isNotNull();
        assertThat(reference).startsWith("BK");

        verify(repository, atLeast(3)).findByBookingReference(anyString());
    }

    @Test
    @DisplayName("Should throw exception when max attempts exceeded")
    void shouldThrowExceptionWhenMaxAttemptsExceeded() {
        // Given
        AppointmentEntity existingAppointment = mock(AppointmentEntity.class);
        when(repository.findByBookingReference(anyString()))
            .thenReturn(Optional.of(existingAppointment));

        // When & Then
        assertThatThrownBy(() -> generator.generate())
            .isInstanceOf(IllegalStateException.class)
            .hasMessage("Failed to generate unique reference");

        verify(repository, times(5)).findByBookingReference(anyString());
    }

    @RepeatedTest(10)
    @DisplayName("Should generate different references each time")
    void shouldGenerateDifferentReferencesEachTime() {
        // Given
        when(repository.findByBookingReference(anyString())).thenReturn(Optional.empty());

        // When
        Set<String> generatedReferences = new HashSet<>();
        for (int i = 0; i < 5; i++) {
            generatedReferences.add(generator.generate());
        }

        // Then
        assertThat(generatedReferences).hasSize(5);
    }

    @Test
    @DisplayName("Should only use uppercase alphanumeric characters after prefix")
    void shouldOnlyUseUppercaseAlphanumericCharacters() {
        // Given
        when(repository.findByBookingReference(anyString())).thenReturn(Optional.empty());

        // When
        String reference = generator.generate();

        // Then
        String withoutPrefix = reference.substring(2);
        assertThat(withoutPrefix).matches("^[A-Z0-9]+$");
        assertThat(withoutPrefix).doesNotContainPattern("[a-z]");
        assertThat(withoutPrefix).doesNotContain("-", "_", " ");
    }

    @Test
    @DisplayName("Should find available reference on first attempt")
    void shouldFindAvailableReferenceOnFirstAttempt() {
        // Given
        when(repository.findByBookingReference(anyString())).thenReturn(Optional.empty());

        // When
        String reference = generator.generate();

        // Then
        assertThat(reference).isNotNull();
        verify(repository, times(1)).findByBookingReference(anyString());
    }

    @Test
    @DisplayName("Should use consistent prefix")
    void shouldUseConsistentPrefix() {
        // Given
        when(repository.findByBookingReference(anyString())).thenReturn(Optional.empty());

        // When
        Set<String> prefixes = new HashSet<>();
        for (int i = 0; i < 10; i++) {
            String reference = generator.generate();
            prefixes.add(reference.substring(0, 2));
        }

        // Then
        assertThat(prefixes).hasSize(1);
        assertThat(prefixes).containsExactly("BK");
    }

    @Test
    @DisplayName("Should handle concurrent generation attempts")
    void shouldHandleConcurrentGenerationAttempts() {
        // Given
        when(repository.findByBookingReference(anyString())).thenReturn(Optional.empty());

        // When
        Set<String> references = new HashSet<>();
        for (int i = 0; i < 20; i++) {
            references.add(generator.generate());
        }

        // Then
        assertThat(references).hasSize(20);
        references.forEach(ref -> {
            assertThat(ref).startsWith("BK");
            assertThat(ref).hasSize(10);
        });
    }

    @Test
    @DisplayName("Should validate reference length is exactly 10 characters")
    void shouldValidateReferenceLength() {
        // Given
        when(repository.findByBookingReference(anyString())).thenReturn(Optional.empty());

        // When
        String reference = generator.generate();

        // Then
        assertThat(reference).hasSize(10);
        assertThat(reference.substring(0, 2)).isEqualTo("BK");
        assertThat(reference.substring(2)).hasSize(8);
    }
}

package com.netflixclone.api.services;

import com.netflixclone.api.models.Movie;
import com.netflixclone.api.repositories.MovieRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.web.server.ResponseStatusException;

import java.security.KeyPairGenerator;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class StreamingServiceTest {

    private static final String EMAIL = "subscriber@example.com";

    @Mock
    private SubscriptionService subscriptionService;

    @Mock
    private MovieRepository movieRepository;

    @InjectMocks
    private StreamingService streamingService;

    @BeforeEach
    void setPrivateKey() throws Exception {
        KeyPairGenerator generator = KeyPairGenerator.getInstance("RSA");
        generator.initialize(2048);
        ReflectionTestUtils.setField(streamingService, "privateKey", generator.generateKeyPair().getPrivate());
    }

    @Test
    void generateStreamingTicketRejectsInactiveSubscriberBeforeMovieLookup() {
        when(subscriptionService.hasActiveSubscription(EMAIL)).thenReturn(false);

        ResponseStatusException exception = assertThrows(
                ResponseStatusException.class,
                () -> streamingService.generateStreamingTicket("sintel", EMAIL)
        );

        assertEquals(HttpStatus.FORBIDDEN, exception.getStatusCode());
        verifyNoInteractions(movieRepository);
    }

    @Test
    void generateStreamingTicketRejectsUnsafeFolderName() {
        when(subscriptionService.hasActiveSubscription(EMAIL)).thenReturn(true);

        ResponseStatusException exception = assertThrows(
                ResponseStatusException.class,
                () -> streamingService.generateStreamingTicket("../secret", EMAIL)
        );

        assertEquals(HttpStatus.BAD_REQUEST, exception.getStatusCode());
        verifyNoInteractions(movieRepository);
    }

    @Test
    void generateStreamingTicketRejectsFolderMissingFromCatalog() {
        when(subscriptionService.hasActiveSubscription(EMAIL)).thenReturn(true);
        when(movieRepository.findByVideoFolderUrl("unknown")).thenReturn(Optional.empty());

        ResponseStatusException exception = assertThrows(
                ResponseStatusException.class,
                () -> streamingService.generateStreamingTicket("unknown", EMAIL)
        );

        assertEquals(HttpStatus.NOT_FOUND, exception.getStatusCode());
    }

    @Test
    void generateStreamingTicketSignsTicketForCatalogFolder() {
        Movie movie = Movie.builder().videoFolderUrl("sintel").build();
        when(subscriptionService.hasActiveSubscription(EMAIL)).thenReturn(true);
        when(movieRepository.findByVideoFolderUrl("sintel")).thenReturn(Optional.of(movie));

        String ticket = streamingService.generateStreamingTicket("sintel", EMAIL);

        assertFalse(ticket.isBlank());
    }
}

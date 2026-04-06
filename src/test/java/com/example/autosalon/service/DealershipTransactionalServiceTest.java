package com.example.autosalon.service;

import com.example.autosalon.entity.Dealership;
import com.example.autosalon.repository.CarRepository;
import com.example.autosalon.repository.DealershipRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class DealershipTransactionalServiceTest {

    @Mock private DealershipRepository dealershipRepository;
    @Mock private CarRepository carRepository;
    @InjectMocks private DealershipTransactionalService service;

    @Test
    void getDealershipById_shouldReturn() {
        Dealership dealership = new Dealership();
        dealership.setId(1L);
        when(dealershipRepository.findById(1L)).thenReturn(Optional.of(dealership));
        assertThat(service.getDealershipById(1L)).isEqualTo(dealership);
    }

    @Test
    void getDealershipById_notFound_throws() {
        when(dealershipRepository.findById(99L)).thenReturn(Optional.empty());
        assertThatThrownBy(() -> service.getDealershipById(99L))
                .isInstanceOf(IllegalArgumentException.class);
    }
}


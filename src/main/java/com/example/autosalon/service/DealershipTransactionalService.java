package com.example.autosalon.service;

import com.example.autosalon.entity.Dealership;
import com.example.autosalon.repository.CarRepository;
import com.example.autosalon.repository.DealershipRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class DealershipTransactionalService {

    private final DealershipRepository dealershipRepository;
    private final CarRepository carRepository;

    @Transactional(readOnly = true)
    public Dealership getDealershipById(Long id) {
        return dealershipRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException(
                        String.format(DealershipService.DEALERSHIP_NOT_FOUND_MESSAGE, id)));
    }

}
package com.example.autosalon.service;

import com.example.autosalon.entity.Customer;
import com.example.autosalon.repository.CustomerRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.beans.factory.ObjectProvider;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class CustomerServiceTest {

    @Mock private CustomerRepository customerRepository;
    @Mock private ObjectProvider<CustomerService> selfProvider;
    @InjectMocks private CustomerService customerService;

    private Customer customer;

    @BeforeEach
    void setUp() {
        customer = new Customer();
        customer.setId(1L);
        customer.setFirstName("John");
        customer.setLastName("Doe");
        customer.setEmail("john@example.com");
        customer.setPhone("123456789");
    }

    @Test
    void getAllCustomers_shouldReturnList() {
        when(customerRepository.findAll()).thenReturn(List.of(customer));
        List<Customer> customers = customerService.getAllCustomers();
        assertThat(customers).hasSize(1);
    }

    @Test
    void getCustomerById_shouldReturnCustomer() {
        when(customerRepository.findById(1L)).thenReturn(Optional.of(customer));
        Customer found = customerService.getCustomerById(1L);
        assertThat(found).isEqualTo(customer);
    }

    @Test
    void getCustomerById_notFound_throws() {
        when(customerRepository.findById(99L)).thenReturn(Optional.empty());
        assertThatThrownBy(() -> customerService.getCustomerById(99L))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void getCustomerByEmail_shouldReturnCustomer() {
        when(customerRepository.findByEmail("john@example.com")).thenReturn(Optional.of(customer));
        Customer found = customerService.getCustomerByEmail("john@example.com");
        assertThat(found).isEqualTo(customer);
    }

    @Test
    void getCustomerByEmail_notFound_throws() {
        when(customerRepository.findByEmail("missing@example.com")).thenReturn(Optional.empty());
        assertThatThrownBy(() -> customerService.getCustomerByEmail("missing@example.com"))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void createCustomer_shouldSave() {
        when(customerRepository.save(any(Customer.class))).thenReturn(customer);
        Customer created = customerService.createCustomer(customer);
        assertThat(created).isEqualTo(customer);
        verify(customerRepository).save(customer);
    }

    @Test
    void updateCustomer_shouldUpdateFields() {
        Customer details = new Customer();
        details.setFirstName("Jane");
        details.setLastName("Roe");
        details.setEmail("jane@example.com");
        details.setPhone("000");

        when(customerRepository.findById(1L)).thenReturn(Optional.of(customer));
        when(selfProvider.getObject()).thenReturn(customerService);

        Customer updated = customerService.updateCustomer(1L, details);
        assertThat(updated.getFirstName()).isEqualTo("Jane");
        assertThat(updated.getLastName()).isEqualTo("Roe");
        assertThat(updated.getEmail()).isEqualTo("jane@example.com");
        assertThat(updated.getPhone()).isEqualTo("000");
    }

    @Test
    void updatePhone_shouldUpdate() {
        when(customerRepository.findById(1L)).thenReturn(Optional.of(customer));
        when(selfProvider.getObject()).thenReturn(customerService);
        Customer updated = customerService.updatePhone(1L, "987654321");
        assertThat(updated.getPhone()).isEqualTo("987654321");
    }

    @Test
    void deleteCustomer_noSales_deletes() {
        customer.setSales(List.of());
        when(customerRepository.findByIdWithSales(1L)).thenReturn(Optional.of(customer));
        customerService.deleteCustomer(1L);
        verify(customerRepository).delete(customer);
    }

    @Test
    void deleteCustomer_notFound_throws() {
        when(customerRepository.findByIdWithSales(99L)).thenReturn(Optional.empty());
        assertThatThrownBy(() -> customerService.deleteCustomer(99L))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void deleteCustomer_withSales_throws() {
        customer.setSales(List.of(new com.example.autosalon.entity.Sale()));
        when(customerRepository.findByIdWithSales(1L)).thenReturn(Optional.of(customer));
        assertThatThrownBy(() -> customerService.deleteCustomer(1L))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("Невозможно удалить покупателя");
        verify(customerRepository, never()).delete(any());
    }
}
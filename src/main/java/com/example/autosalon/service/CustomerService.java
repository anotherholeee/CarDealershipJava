package com.example.autosalon.service;

import com.example.autosalon.entity.Customer;
import com.example.autosalon.repository.CustomerRepository;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class CustomerService {

    private final CustomerRepository customerRepository;
    private final ObjectProvider<CustomerService> self;

    @Transactional(readOnly = true)
    public List<Customer> getAllCustomers() {
        return customerRepository.findAll();
    }

    @Transactional(readOnly = true)
    public Customer getCustomerById(Long id) {
        return customerRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException(
                        "Customer not found with id: " + id));
    }

    @Transactional(readOnly = true)
    public Customer getCustomerByEmail(String email) {
        return customerRepository.findByEmail(email)
                .orElseThrow(() -> new IllegalArgumentException(
                        "Customer not found with email: " + email));
    }

    @Transactional
    public Customer createCustomer(Customer customer) {
        customer.setId(null);
        return customerRepository.save(customer);
    }

    @Transactional
    public Customer updateCustomer(Long id, Customer customerDetails) {
        Customer existingCustomer = self.getObject().getCustomerById(id);

        existingCustomer.setFirstName(customerDetails.getFirstName());
        existingCustomer.setLastName(customerDetails.getLastName());
        existingCustomer.setEmail(customerDetails.getEmail());
        existingCustomer.setPhone(customerDetails.getPhone());

        return existingCustomer;
    }

    @Transactional
    public Customer updatePhone(Long id, String phone) {
        Customer customer = self.getObject().getCustomerById(id);
        customer.setPhone(phone);
        return customer;
    }

    @Transactional
    public void deleteCustomer(Long id) {
        Customer customer = self.getObject().getCustomerById(id);

        if (!customer.getSales().isEmpty()) {
            throw new IllegalStateException(
                    String.format(
                            "Невозможно удалить покупателя ID=%d %s %s - у него есть продажи (количество: %d)",
                            customer.getId(),
                            customer.getFirstName(),
                            customer.getLastName(),
                            customer.getSales().size()));
        }

        customerRepository.delete(customer);
    }
}
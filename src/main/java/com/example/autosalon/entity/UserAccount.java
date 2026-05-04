package com.example.autosalon.entity;

import com.example.autosalon.enums.AccountType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;
import java.util.ArrayList;
import java.util.List;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.ToString;

@Entity
@Table(name = "user_accounts")
@Data
@EqualsAndHashCode(exclude = {"cars"})
@NoArgsConstructor
@AllArgsConstructor
@ToString(exclude = "cars")
public class UserAccount {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true, length = 120)
    private String username;

    @Column(nullable = false, length = 255)
    private String passwordHash;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private AccountType accountType;

    @Column(length = 120)
    private String personName;

    @Column(length = 200)
    private String companyName;

    @Column(length = 40)
    private String phone;

    @Column(length = 255)
    private String address;

    @OneToMany(mappedBy = "owner")
    private List<Car> cars = new ArrayList<>();
}

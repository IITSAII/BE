package com.iitsaii.photobooth.domain.payment.repository;

import com.iitsaii.photobooth.domain.payment.entity.Payment;
import org.springframework.data.jpa.repository.JpaRepository;

public interface PaymentRepository extends JpaRepository<Payment, Long> {
}

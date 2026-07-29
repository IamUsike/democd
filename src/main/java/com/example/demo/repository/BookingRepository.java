package com.example.demo.repository;

import com.example.demo.model.Booking;
import java.util.List;
import java.util.Optional;

public interface BookingRepository {
    Booking save(Booking booking);
    Optional<Booking> findById(Integer id);
    List<Booking> findAll();
    Booking update(Booking booking);
    Boolean delete(Integer id);
}


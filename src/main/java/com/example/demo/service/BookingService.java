package com.example.demo.service;

import com.example.demo.model.Booking;
import java.util.List;
import java.util.Optional;

public interface BookingService {
    Booking createBooking(Booking booking);
    Optional<Booking> getBookingById(Integer id);
    List<Booking> getAllBookings();
    Booking updateBooking(Booking booking);
    Boolean deleteBooking(Integer id);
}


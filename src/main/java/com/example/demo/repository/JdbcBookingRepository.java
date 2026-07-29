package com.example.demo.repository;

import com.example.demo.model.Booking;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.stereotype.Repository;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.List;
import java.util.Optional;

@Repository
public class JdbcBookingRepository implements BookingRepository {

    @Autowired
    private JdbcTemplate jdbcTemplate;

    private final RowMapper<Booking> rowMapper = new RowMapper<Booking>() {
        @Override
        public Booking mapRow(ResultSet rs, int rowNum) throws SQLException {
            Booking booking = new Booking();
            booking.setId(rs.getInt("id"));
            booking.setUserName(rs.getString("user_name"));
            booking.setWorkspace(rs.getString("workspace"));
            return booking;
        }
    };

    @Override
    public Booking save(Booking booking) {
        String sql = "INSERT INTO booking (user_name, workspace) VALUES (?, ?)";
        jdbcTemplate.update(sql, booking.getUserName(), booking.getWorkspace());
        return booking;
    }

    @Override
    public Optional<Booking> findById(Integer id) {
        String sql = "SELECT * FROM booking WHERE id = ?";
        try {
            Booking booking = jdbcTemplate.queryForObject(sql, rowMapper, id);
            return Optional.of(booking);
        } catch (Exception e) {
            return Optional.empty();
        }
    }

    @Override
    public List<Booking> findAll() {
        String sql = "SELECT * FROM booking";
        return jdbcTemplate.query(sql, rowMapper);
    }

    @Override
    public Booking update(Booking booking) {
        String sql = "UPDATE booking SET user_name = ?, workspace = ? WHERE id = ?";
        jdbcTemplate.update(sql, booking.getUserName(), booking.getWorkspace(), booking.getId());
        return booking;
    }

    @Override
    public Boolean delete(Integer id) {
        String sql = "DELETE FROM booking WHERE id = ?";
        return jdbcTemplate.update(sql, id) > 0;
    }
}


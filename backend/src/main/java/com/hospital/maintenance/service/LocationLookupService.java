package com.hospital.maintenance.service;

import com.hospital.maintenance.dto.WardOption;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.stereotype.Service;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

@Service
public class LocationLookupService {

    private final JdbcTemplate jdbcTemplate;

    public LocationLookupService(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    public List<WardOption> getWardOptions() {
        List<WardOption> wards = jdbcTemplate.query(
                "select location_code, location_name from ref_locations " +
                        "where location_name like 'Wa%' order by location_name",
                new RowMapper<WardOption>() {
                    @Override
                    public WardOption mapRow(ResultSet rs, int rowNum) throws SQLException {
                        return new WardOption(
                                rs.getString("location_code"),
                                rs.getString("location_name")
                        );
                    }
                }
        );

        return new ArrayList<WardOption>(wards);
    }
}

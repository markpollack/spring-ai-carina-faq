package com.example.carina.data;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/data")
public class DataController {

    private final DataLoadingService dataLoadingService;

    private final JdbcTemplate jdbcTemplate;

    @Autowired
    public DataController(DataLoadingService dataLoadingService, JdbcTemplate jdbcTemplate) {
        this.dataLoadingService = dataLoadingService;
        this.jdbcTemplate = jdbcTemplate;
    }

    @PostMapping("/load")
    public ResponseEntity<String> load() {
        try {
            this.dataLoadingService.load();
            return ResponseEntity.ok("Data loaded successfully!");
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body("An error occurred while loading data: " + e.getMessage());
        }
    }

    @GetMapping("/count")
    public int count() {
        String sql = "SELECT COUNT(*) FROM vector_store";
        return jdbcTemplate.queryForObject(sql, Integer.class);
    }

    @PostMapping("/delete")
    public void delete() {
        String sql = "DELETE FROM vector_store";
        jdbcTemplate.update(sql);
    }


    @ExceptionHandler(Exception.class)
    public ResponseEntity<String> handleException(Exception e) {
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body("An error occurred in the controller: " + e.getMessage());
    }
}

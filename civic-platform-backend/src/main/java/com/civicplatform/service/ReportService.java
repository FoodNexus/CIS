package com.civicplatform.service;

import com.civicplatform.enums.UserType;

import java.time.LocalDate;

public interface ReportService {

    byte[] generateReport(LocalDate from, LocalDate to, UserType type, String format);
}

package com.hospital.maintenance.service;

import com.hospital.maintenance.dto.DamaDiagnosisReportResponse;
import com.hospital.maintenance.dto.DamaDiagnosisRow;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Service;

import java.nio.charset.Charset;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

@Service
public class DamaDiagnosisReportService {

    private static final Charset UTF8 = Charset.forName("UTF-8");

    private final NamedParameterJdbcTemplate namedParameterJdbcTemplate;

    public DamaDiagnosisReportService(NamedParameterJdbcTemplate namedParameterJdbcTemplate) {
        this.namedParameterJdbcTemplate = namedParameterJdbcTemplate;
    }

    public DamaDiagnosisReportResponse loadReport(String fromDate, String toDate) {
        validateDates(fromDate, toDate);

        List<DamaDiagnosisRow> rows = loadRows(fromDate, toDate);
        DamaDiagnosisReportResponse response = new DamaDiagnosisReportResponse();
        response.setFromDate(fromDate);
        response.setToDate(toDate);
        response.setTotalRecords(rows.size());
        response.setRows(rows);
        return response;
    }

    public byte[] exportExcel(String fromDate, String toDate) {
        validateDates(fromDate, toDate);
        List<DamaDiagnosisRow> rows = loadRows(fromDate, toDate);
        return buildExcelHtml(rows, fromDate, toDate).getBytes(UTF8);
    }

    private List<DamaDiagnosisRow> loadRows(String fromDate, String toDate) {
        String sql = "SELECT a.patient_mrn, person_register, b.person_name, n.diagnosis_clinical, f.discharge_type_name, "
                + "UPPER(b.person_cur_street_1) AS person_cur_street_1, "
                + "UPPER(b.person_cur_street_2) AS person_cur_street_2, "
                + "UPPER(b.person_cur_street_3) AS person_cur_street_3, "
                + "b.person_cur_city, b.person_cur_postcode, b.person_cur_state, b.person_new_ic, "
                + "DATE_FORMAT(b.person_birthdate, '%Y-%m-%d') AS person_birthdate, "
                + "CAST(b.person_age AS CHAR) AS person_age, b.person_cur_country, b.nation_code, b.marital_code, "
                + "b.person_birth_certificate, b.person_cur_phone_home, b.person_cur_phone_mobile, b.person_cur_phone_office, "
                + "b.occupation_code, b.person_old_ic, b.person_passport, b.person_police_id, b.person_military_id, "
                + "b.person_other_id, b.person_splp, e.discharge_ward, "
                + "DATE_FORMAT(e.encounter_timestamp, '%Y-%m-%d %H:%i:%s') AS encounter_timestamp, "
                + "CONCAT(DATE_FORMAT(d.discharge_date, '%Y-%m-%d'), ' ', m.pre_discharge_time, ':00') AS discharge_date, "
                + "DATE_FORMAT(e.encounter_timestamp, '%Y-%m-%d %H:%i:%s') AS admission_date, "
                + "specialist_mmc, citizenship "
                + "FROM encounters e "
                + "INNER JOIN enc_discharges d ON d.encounter_id = e.encounter_id "
                + "INNER JOIN patients a ON a.patient_id = e.patient_id "
                + "INNER JOIN persons b ON a.person_id = b.person_id "
                + "INNER JOIN enc_admissions m ON m.admission_id = d.admission_id "
                + "INNER JOIN enc_diagnosis n ON n.encounter_id = e.encounter_id "
                + "INNER JOIN ref_discharge_type f ON f.discharge_type_code = d.discharge_type_code "
                + "WHERE d.discharge_date >= STR_TO_DATE(:fromDate, '%Y-%m-%d') "
                + "AND d.discharge_date < DATE_ADD(STR_TO_DATE(:toDate, '%Y-%m-%d'), INTERVAL 1 DAY) "
                + "AND TRIM(d.discharge_type_code) = '4' "
                + "AND e.domain_code IN ('IN', 'DC') "
                + "AND e.discharge_date IS NOT NULL "
                + "AND n.diagnosis_inactive = 0 "
                + "ORDER BY e.encounter_id";

        return namedParameterJdbcTemplate.query(sql,
                new MapSqlParameterSource()
                        .addValue("fromDate", fromDate)
                        .addValue("toDate", toDate),
                new RowMapper<DamaDiagnosisRow>() {
                    @Override
                    public DamaDiagnosisRow mapRow(ResultSet rs, int rowNum) throws SQLException {
                        DamaDiagnosisRow row = new DamaDiagnosisRow();
                        row.setPatientMrn(rs.getString("patient_mrn"));
                        row.setPersonRegister(rs.getString("person_register"));
                        row.setPersonName(rs.getString("person_name"));
                        row.setDiagnosisClinical(rs.getString("diagnosis_clinical"));
                        row.setDischargeTypeName(rs.getString("discharge_type_name"));
                        row.setPersonCurStreet1(rs.getString("person_cur_street_1"));
                        row.setPersonCurStreet2(rs.getString("person_cur_street_2"));
                        row.setPersonCurStreet3(rs.getString("person_cur_street_3"));
                        row.setPersonCurCity(rs.getString("person_cur_city"));
                        row.setPersonCurPostcode(rs.getString("person_cur_postcode"));
                        row.setPersonCurState(rs.getString("person_cur_state"));
                        row.setPersonNewIc(rs.getString("person_new_ic"));
                        row.setPersonBirthdate(rs.getString("person_birthdate"));
                        row.setPersonAge(rs.getString("person_age"));
                        row.setPersonCurCountry(rs.getString("person_cur_country"));
                        row.setNationCode(rs.getString("nation_code"));
                        row.setMaritalCode(rs.getString("marital_code"));
                        row.setPersonBirthCertificate(rs.getString("person_birth_certificate"));
                        row.setPersonCurPhoneHome(rs.getString("person_cur_phone_home"));
                        row.setPersonCurPhoneMobile(rs.getString("person_cur_phone_mobile"));
                        row.setPersonCurPhoneOffice(rs.getString("person_cur_phone_office"));
                        row.setOccupationCode(rs.getString("occupation_code"));
                        row.setPersonOldIc(rs.getString("person_old_ic"));
                        row.setPersonPassport(rs.getString("person_passport"));
                        row.setPersonPoliceId(rs.getString("person_police_id"));
                        row.setPersonMilitaryId(rs.getString("person_military_id"));
                        row.setPersonOtherId(rs.getString("person_other_id"));
                        row.setPersonSplp(rs.getString("person_splp"));
                        row.setDischargeWard(rs.getString("discharge_ward"));
                        row.setEncounterTimestamp(rs.getString("encounter_timestamp"));
                        row.setDischargeDate(rs.getString("discharge_date"));
                        row.setAdmissionDate(rs.getString("admission_date"));
                        row.setSpecialistMmc(rs.getString("specialist_mmc"));
                        row.setCitizenship(rs.getString("citizenship"));
                        return row;
                    }
                });
    }

    private void validateDates(String fromDate, String toDate) {
        if (fromDate == null || fromDate.trim().isEmpty() || toDate == null || toDate.trim().isEmpty()) {
            throw new IllegalArgumentException("Please provide both From Date and To Date.");
        }
        if (fromDate.compareTo(toDate) > 0) {
            throw new IllegalArgumentException("From Date cannot be later than To Date.");
        }
    }

    private String buildExcelHtml(List<DamaDiagnosisRow> rows, String fromDate, String toDate) {
        List<String[]> columns = columns();
        StringBuilder html = new StringBuilder();
        html.append("<html><head><meta charset=\"UTF-8\"></head><body>");
        html.append("<table border=\"1\">\n");
        html.append("<tr><th colspan=\"").append(columns.size()).append("\">Data Diagnosis DAMA Bulanan</th></tr>\n");
        html.append("<tr><th colspan=\"").append(columns.size()).append("\">Tempoh: ").append(escape(fromDate)).append(" hingga ").append(escape(toDate)).append("</th></tr>\n");
        html.append("<tr>");
        for (int i = 0; i < columns.size(); i++) {
            html.append("<th>").append(escape(columns.get(i)[1])).append("</th>");
        }
        html.append("</tr>\n");

        for (int i = 0; i < rows.size(); i++) {
            DamaDiagnosisRow row = rows.get(i);
            html.append("<tr>");
            for (int j = 0; j < columns.size(); j++) {
                html.append("<td>").append(escape(valueFor(row, columns.get(j)[0]))).append("</td>");
            }
            html.append("</tr>\n");
        }

        if (rows.isEmpty()) {
            html.append("<tr><td colspan=\"").append(columns.size()).append("\">No data found.</td></tr>");
        }

        html.append("</table></body></html>");
        return html.toString();
    }

    private List<String[]> columns() {
        List<String[]> columns = new ArrayList<String[]>();
        columns.add(new String[] {"patientMrn", "Patient MRN"});
        columns.add(new String[] {"personRegister", "Person Register"});
        columns.add(new String[] {"personName", "Person Name"});
        columns.add(new String[] {"diagnosisClinical", "Diagnosis Clinical"});
        columns.add(new String[] {"dischargeTypeName", "Discharge Type"});
        columns.add(new String[] {"personCurStreet1", "Address Line 1"});
        columns.add(new String[] {"personCurStreet2", "Address Line 2"});
        columns.add(new String[] {"personCurStreet3", "Address Line 3"});
        columns.add(new String[] {"personCurCity", "City"});
        columns.add(new String[] {"personCurPostcode", "Postcode"});
        columns.add(new String[] {"personCurState", "State"});
        columns.add(new String[] {"personNewIc", "New IC"});
        columns.add(new String[] {"personBirthdate", "Birthdate"});
        columns.add(new String[] {"personAge", "Age"});
        columns.add(new String[] {"personCurCountry", "Country"});
        columns.add(new String[] {"nationCode", "Nation Code"});
        columns.add(new String[] {"maritalCode", "Marital Code"});
        columns.add(new String[] {"personBirthCertificate", "Birth Certificate"});
        columns.add(new String[] {"personCurPhoneHome", "Phone Home"});
        columns.add(new String[] {"personCurPhoneMobile", "Phone Mobile"});
        columns.add(new String[] {"personCurPhoneOffice", "Phone Office"});
        columns.add(new String[] {"occupationCode", "Occupation Code"});
        columns.add(new String[] {"personOldIc", "Old IC"});
        columns.add(new String[] {"personPassport", "Passport"});
        columns.add(new String[] {"personPoliceId", "Police ID"});
        columns.add(new String[] {"personMilitaryId", "Military ID"});
        columns.add(new String[] {"personOtherId", "Other ID"});
        columns.add(new String[] {"personSplp", "SPLP"});
        columns.add(new String[] {"dischargeWard", "Discharge Ward"});
        columns.add(new String[] {"encounterTimestamp", "Encounter Timestamp"});
        columns.add(new String[] {"dischargeDate", "Discharge Date"});
        columns.add(new String[] {"admissionDate", "Admission Date"});
        columns.add(new String[] {"specialistMmc", "Specialist MMC"});
        columns.add(new String[] {"citizenship", "Citizenship"});
        return columns;
    }

    private String valueFor(DamaDiagnosisRow row, String key) {
        if ("patientMrn".equals(key)) return row.getPatientMrn();
        if ("personRegister".equals(key)) return row.getPersonRegister();
        if ("personName".equals(key)) return row.getPersonName();
        if ("diagnosisClinical".equals(key)) return row.getDiagnosisClinical();
        if ("dischargeTypeName".equals(key)) return row.getDischargeTypeName();
        if ("personCurStreet1".equals(key)) return row.getPersonCurStreet1();
        if ("personCurStreet2".equals(key)) return row.getPersonCurStreet2();
        if ("personCurStreet3".equals(key)) return row.getPersonCurStreet3();
        if ("personCurCity".equals(key)) return row.getPersonCurCity();
        if ("personCurPostcode".equals(key)) return row.getPersonCurPostcode();
        if ("personCurState".equals(key)) return row.getPersonCurState();
        if ("personNewIc".equals(key)) return row.getPersonNewIc();
        if ("personBirthdate".equals(key)) return row.getPersonBirthdate();
        if ("personAge".equals(key)) return row.getPersonAge();
        if ("personCurCountry".equals(key)) return row.getPersonCurCountry();
        if ("nationCode".equals(key)) return row.getNationCode();
        if ("maritalCode".equals(key)) return row.getMaritalCode();
        if ("personBirthCertificate".equals(key)) return row.getPersonBirthCertificate();
        if ("personCurPhoneHome".equals(key)) return row.getPersonCurPhoneHome();
        if ("personCurPhoneMobile".equals(key)) return row.getPersonCurPhoneMobile();
        if ("personCurPhoneOffice".equals(key)) return row.getPersonCurPhoneOffice();
        if ("occupationCode".equals(key)) return row.getOccupationCode();
        if ("personOldIc".equals(key)) return row.getPersonOldIc();
        if ("personPassport".equals(key)) return row.getPersonPassport();
        if ("personPoliceId".equals(key)) return row.getPersonPoliceId();
        if ("personMilitaryId".equals(key)) return row.getPersonMilitaryId();
        if ("personOtherId".equals(key)) return row.getPersonOtherId();
        if ("personSplp".equals(key)) return row.getPersonSplp();
        if ("dischargeWard".equals(key)) return row.getDischargeWard();
        if ("encounterTimestamp".equals(key)) return row.getEncounterTimestamp();
        if ("dischargeDate".equals(key)) return row.getDischargeDate();
        if ("admissionDate".equals(key)) return row.getAdmissionDate();
        if ("specialistMmc".equals(key)) return row.getSpecialistMmc();
        if ("citizenship".equals(key)) return row.getCitizenship();
        return "";
    }

    private String escape(String value) {
        if (value == null) {
            return "";
        }
        return value.replace("&", "&amp;")
                .replace("<", "&lt;")
                .replace(">", "&gt;")
                .replace("\"", "&quot;");
    }
}

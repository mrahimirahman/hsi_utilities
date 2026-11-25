import java.sql.*;

public class DamaCountCheck {
    public static void main(String[] args) throws Exception {
        Class.forName("com.mysql.jdbc.Driver");
        Connection conn = DriverManager.getConnection(
            "jdbc:mysql://10.29.217.206:3306/his_kkm?useSSL=false&allowPublicKeyRetrieval=true",
            "root",
            "R00t@!23"
        );

        String[] queries = new String[] {
            "SELECT COUNT(*) FROM (SELECT a.patient_mrn, person_register, b.person_name, n.diagnosis_clinical, f.discharge_type_name, UPPER(b.person_cur_street_1) AS person_cur_street_1, UPPER(b.person_cur_street_2) AS person_cur_street_2, UPPER(b.person_cur_street_3) AS person_cur_street_3, b.person_cur_city, b.person_cur_postcode, b.person_cur_state, b.person_new_ic, b.person_birthdate, b.person_age,b.person_cur_country, b.nation_code, b.marital_code, b.person_birth_certificate, b.person_cur_phone_home, b.person_cur_phone_mobile, b.person_cur_phone_office, b.occupation_code, b.person_old_ic, b.person_passport, b.person_police_id, b.person_military_id, b.person_other_id, b.person_splp,e.discharge_ward, e.encounter_timestamp, CONCAT(d.discharge_date, ' ',  m.pre_discharge_time, ':00') AS discharge_date, e.encounter_timestamp AS admission_date, specialist_mmc, citizenship FROM encounters e INNER JOIN enc_discharges d ON d.encounter_id = e.encounter_id INNER JOIN patients a ON a.patient_id = e.patient_id INNER JOIN persons b ON a.person_id = b.person_id INNER JOIN enc_admissions m ON m.admission_id = d.admission_id INNER JOIN enc_diagnosis n ON n.encounter_id = e.encounter_id INNER JOIN ref_discharge_type f ON f.discharge_type_code=d.discharge_type_code WHERE d.discharge_date >= DATE('2026-03-01') AND d.discharge_date < DATE('2026-03-31') AND TRIM(d.discharge_type_code) = '4' AND e.domain_code IN ('IN', 'DC') AND e.discharge_date IS NOT NULL AND n.diagnosis_inactive=0 ORDER BY e.encounter_id) x",
            "SELECT COUNT(*) FROM (SELECT a.patient_mrn, person_register, b.person_name, n.diagnosis_clinical, f.discharge_type_name, UPPER(b.person_cur_street_1) AS person_cur_street_1, UPPER(b.person_cur_street_2) AS person_cur_street_2, UPPER(b.person_cur_street_3) AS person_cur_street_3, b.person_cur_city, b.person_cur_postcode, b.person_cur_state, b.person_new_ic, DATE_FORMAT(b.person_birthdate, '%Y-%m-%d') AS person_birthdate, CAST(b.person_age AS CHAR) AS person_age, b.person_cur_country, b.nation_code, b.marital_code, b.person_birth_certificate, b.person_cur_phone_home, b.person_cur_phone_mobile, b.person_cur_phone_office, b.occupation_code, b.person_old_ic, b.person_passport, b.person_police_id, b.person_military_id, b.person_other_id, b.person_splp, e.discharge_ward, DATE_FORMAT(e.encounter_timestamp, '%Y-%m-%d %H:%i:%s') AS encounter_timestamp, CONCAT(DATE_FORMAT(d.discharge_date, '%Y-%m-%d'), ' ', m.pre_discharge_time, ':00') AS discharge_date, DATE_FORMAT(e.encounter_timestamp, '%Y-%m-%d %H:%i:%s') AS admission_date, specialist_mmc, citizenship FROM encounters e INNER JOIN enc_discharges d ON d.encounter_id = e.encounter_id INNER JOIN patients a ON a.patient_id = e.patient_id INNER JOIN persons b ON a.person_id = b.person_id INNER JOIN enc_admissions m ON m.admission_id = d.admission_id INNER JOIN enc_diagnosis n ON n.encounter_id = e.encounter_id INNER JOIN ref_discharge_type f ON f.discharge_type_code = d.discharge_type_code WHERE d.discharge_date >= STR_TO_DATE('2026-03-01', '%Y-%m-%d') AND d.discharge_date < DATE_ADD(STR_TO_DATE('2026-03-31', '%Y-%m-%d'), INTERVAL 1 DAY) AND TRIM(d.discharge_type_code) = '4' AND e.domain_code IN ('IN', 'DC') AND e.discharge_date IS NOT NULL AND n.diagnosis_inactive = 0 ORDER BY e.encounter_id) x",
            "SELECT COUNT(*) FROM (SELECT a.patient_mrn, person_register, b.person_name, n.diagnosis_clinical FROM encounters e INNER JOIN enc_discharges d ON d.encounter_id = e.encounter_id INNER JOIN patients a ON a.patient_id = e.patient_id INNER JOIN persons b ON a.person_id = b.person_id INNER JOIN enc_admissions m ON m.admission_id = d.admission_id INNER JOIN enc_diagnosis n ON n.encounter_id = e.encounter_id INNER JOIN ref_discharge_type f ON f.discharge_type_code = d.discharge_type_code WHERE d.discharge_date >= STR_TO_DATE('2026-03-01', '%Y-%m-%d') AND d.discharge_date < STR_TO_DATE('2026-03-31', '%Y-%m-%d') AND TRIM(d.discharge_type_code) = '4' AND e.domain_code IN ('IN', 'DC') AND e.discharge_date IS NOT NULL AND n.diagnosis_inactive = 0 ORDER BY e.encounter_id) x"
        };

        for (int i = 0; i < queries.length; i++) {
            Statement st = conn.createStatement();
            ResultSet rs = st.executeQuery(queries[i]);
            rs.next();
            System.out.println("query" + (i + 1) + "=" + rs.getInt(1));
            rs.close();
            st.close();
        }
        conn.close();
    }
}

import java.sql.*;

public class DamaDualDbCheck {
    private static void check(String host) throws Exception {
        Class.forName("com.mysql.jdbc.Driver");
        Connection conn = DriverManager.getConnection(
            "jdbc:mysql://" + host + ":3306/his_kkm?useSSL=false&allowPublicKeyRetrieval=true",
            "root",
            "R00t@!23"
        );
        String sql = "SELECT COUNT(*) FROM (SELECT a.patient_mrn, person_register, b.person_name, n.diagnosis_clinical FROM encounters e INNER JOIN enc_discharges d ON d.encounter_id = e.encounter_id INNER JOIN patients a ON a.patient_id = e.patient_id INNER JOIN persons b ON a.person_id = b.person_id INNER JOIN enc_admissions m ON m.admission_id = d.admission_id INNER JOIN enc_diagnosis n ON n.encounter_id = e.encounter_id INNER JOIN ref_discharge_type f ON f.discharge_type_code = d.discharge_type_code WHERE d.discharge_date >= DATE('2026-03-01') AND d.discharge_date < DATE('2026-03-31') AND TRIM(d.discharge_type_code) = '4' AND e.domain_code IN ('IN', 'DC') AND e.discharge_date IS NOT NULL AND n.diagnosis_inactive = 0 ORDER BY e.encounter_id) x";
        Statement st = conn.createStatement();
        ResultSet rs = st.executeQuery(sql);
        rs.next();
        System.out.println(host + "=" + rs.getInt(1));
        rs.close();
        st.close();
        conn.close();
    }

    public static void main(String[] args) throws Exception {
        check("10.29.217.206");
        check("10.29.59.55");
    }
}

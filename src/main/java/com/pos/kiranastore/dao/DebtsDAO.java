package com.pos.kiranastore.dao;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.ArrayList;
import java.util.List;

import com.pos.kiranastore.bean.Customer;
import com.pos.kiranastore.connection.DBConnection;

public class DebtsDAO {

    public List<Customer> getDebtors() {
        List<Customer> debtors = new ArrayList<>();
        String sql = "SELECT * FROM customers WHERE outstanding > 0 AND status='A'";

        try (Connection conn = DBConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {

            while (rs.next()) {
                Customer c = new Customer();
                c.setId(rs.getInt("id"));
                c.setName(rs.getString("name"));
                c.setPhone(rs.getString("phone"));
                c.setAddress(rs.getString("address"));
                c.setOutstanding(rs.getDouble("outstanding"));
                debtors.add(c);
            }

        } catch (Exception e) {
            e.printStackTrace();
        }
        return debtors;
    }

    
    public boolean payDebt(int customerId, double paidAmount) {
        boolean success = false;

        String updateCustomerSql =
            "UPDATE customers SET outstanding = outstanding - ? " +
            "WHERE id = ? AND outstanding >= ?";

        String closeBillSql =
            "UPDATE bills SET status = 'PAID' " +
            "WHERE customer_id = ? AND status = 'OPEN'";

        try (Connection conn = DBConnection.getConnection()) {
            conn.setAutoCommit(false); // 🔐 transaction

            // 1️⃣ Reduce outstanding
            try (PreparedStatement ps = conn.prepareStatement(updateCustomerSql)) {
                ps.setDouble(1, paidAmount);
                ps.setInt(2, customerId);
                ps.setDouble(3, paidAmount);

                int rows = ps.executeUpdate();
                if (rows == 0) {
                    conn.rollback();
                    return false;
                }
            }

            // 2️⃣ Close OPEN bills
            try (PreparedStatement ps2 = conn.prepareStatement(closeBillSql)) {
                ps2.setInt(1, customerId);
                ps2.executeUpdate();
            }

            conn.commit();
            success = true;

        } catch (Exception e) {
            e.printStackTrace();
        }

        return success;
    }

}

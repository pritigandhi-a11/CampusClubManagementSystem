package com.campusclub.service;

import com.campusclub.dao.StudentDAO;
import com.campusclub.exception.DatabaseOperationException;
import com.campusclub.model.Student;
import com.campusclub.util.PasswordUtil;
import com.campusclub.util.ValidationUtil;

import java.util.*;

/**
 * Service managing Student-specific domain operations, statistics, and profile management.
 * Demonstrates Collections usage: ArrayList, HashMap, and Comparator.
 */
public class StudentService {
    private final StudentDAO studentDAO;

    public StudentService() {
        this.studentDAO = new StudentDAO();
    }

    public StudentService(StudentDAO studentDAO) {
        this.studentDAO = studentDAO;
    }

    public Optional<Student> getStudentProfile(int studentId) throws DatabaseOperationException {
        if (studentId <= 0) {
            throw new IllegalArgumentException("Invalid student ID: " + studentId);
        }
        return studentDAO.findById(studentId);
    }

    public Optional<Student> getStudentByUserId(int userId) throws DatabaseOperationException {
        if (userId <= 0) {
            throw new IllegalArgumentException("Invalid user ID: " + userId);
        }
        return studentDAO.findByUserId(userId);
    }

    public Optional<Student> getStudentByRegisterNumber(String registerNumber) throws DatabaseOperationException {
        if (ValidationUtil.isNullOrEmpty(registerNumber)) {
            return Optional.empty();
        }
        return studentDAO.findByRegisterNumber(registerNumber.trim());
    }

    public List<Student> getAllStudents() throws DatabaseOperationException {
        return studentDAO.findAll();
    }

    public int registerNewStudent(Student student, String plainPassword) throws DatabaseOperationException {
        if (student == null) {
            throw new IllegalArgumentException("Student object cannot be null.");
        }
        ValidationUtil.validateUsername(student.getUsername());
        ValidationUtil.validateEmail(student.getEmail());
        ValidationUtil.validateRequired(student.getRegisterNumber(), "Register Number");
        ValidationUtil.validatePassword(plainPassword);

        // Hash password before persistence
        student.setPasswordHash(PasswordUtil.hashPassword(plainPassword));
        return studentDAO.createStudent(student);
    }

    public boolean updateProfile(Student student) throws DatabaseOperationException {
        if (student == null) {
            throw new IllegalArgumentException("Invalid student entity for update.");
        }
        return studentDAO.updateStudent(student);
    }

    // ==========================================================
    // Collections, Searching, Sorting & Statistics
    // ==========================================================

    /**
     * Computes statistics: count of students by academic department using a HashMap.
     */
    public Map<String, Integer> getStudentCountByDepartment() throws DatabaseOperationException {
        List<Student> students = getAllStudents();
        Map<String, Integer> deptCounts = new LinkedHashMap<>();
        for (Student s : students) {
            String dept = s.getDepartment() != null ? s.getDepartment() : "General";
            deptCounts.put(dept, deptCounts.getOrDefault(dept, 0) + 1);
        }
        return deptCounts;
    }

    /**
     * Searches students by matching keywords against name, roll number, department, or interests.
     */
    public List<Student> searchStudents(String keyword) throws DatabaseOperationException {
        List<Student> all = getAllStudents();
        if (keyword == null || keyword.trim().isEmpty()) {
            return all;
        }
        String term = keyword.trim().toLowerCase();
        List<Student> results = new ArrayList<>();
        for (Student s : all) {
            boolean nameMatch = s.getFullName() != null && s.getFullName().toLowerCase().contains(term);
            boolean regMatch = s.getRegisterNumber() != null && s.getRegisterNumber().toLowerCase().contains(term);
            boolean deptMatch = s.getDepartment() != null && s.getDepartment().toLowerCase().contains(term);
            boolean interestMatch = s.getInterests() != null && s.getInterests().toLowerCase().contains(term);
            if (nameMatch || regMatch || deptMatch || interestMatch) {
                results.add(s);
            }
        }
        return results;
    }

    /**
     * Sorts students by full name using a Comparator.
     */
    public List<Student> sortStudentsByName(List<Student> students, boolean ascending) {
        if (students == null) return Collections.emptyList();
        List<Student> sorted = new ArrayList<>(students);
        Comparator<Student> comp = Comparator.comparing(
                s -> s.getFullName() != null ? s.getFullName().toLowerCase() : ""
        );
        if (!ascending) {
            comp = comp.reversed();
        }
        sorted.sort(comp);
        return sorted;
    }
}

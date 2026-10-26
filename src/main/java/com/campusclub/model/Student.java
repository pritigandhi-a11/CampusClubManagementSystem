package com.campusclub.model;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;

/**
 * [OOP: Inheritance & Composition]
 * Represents a Student in the Campus Club Management System.
 * Inherits identity and authentication attributes from User.
 * Composes student memberships and event registrations.
 */
public class Student extends User {

    // [OOP: Encapsulation - Private specific attributes]
    private int studentTableId;
    private String registerNumber;
    private String department;
    private int year;
    private int semester;
    private String interests;

    private double cgpa;

    // [OOP: Composition - Student maintains a collection of memberships and event registrations]
    private final List<Membership> memberships = new ArrayList<>();
    private final List<EventRegistration> registrations = new ArrayList<>();

    public Student() {
        super();
        setRole(UserRole.STUDENT);
        this.year = 1;
        this.semester = 1;
        this.cgpa = 0.0;
    }

    public Student(int userId, String username, String passwordHash, String fullName, String email,
                   String phoneNumber, String registerNumber, String department, int year, int semester, String interests) {
        super(userId, username, passwordHash, fullName, email, UserRole.STUDENT, phoneNumber);
        this.registerNumber = registerNumber;
        this.department = department;
        this.year = year;
        this.semester = semester;
        this.interests = interests;
        this.cgpa = 0.0;
    }

    public Student(int studentId, int userId, String registerNumber, String department, int year, double cgpa,
                   String interests, int userTableId, String username, String passwordHash, String email,
                   String fullName, String phoneNumber, UserRole role, java.time.LocalDateTime createdAt, boolean active) {
        super(userId, username, passwordHash, fullName, email, role, phoneNumber, createdAt, active);
        this.studentTableId = studentId;
        this.registerNumber = registerNumber;
        this.department = department;
        this.year = year;
        this.semester = year * 2;
        this.cgpa = cgpa;
        this.interests = interests;
    }

    // ==========================================================
    // [OOP: Polymorphism - Overridden Methods]
    // ==========================================================

    @Override
    public String getRoleDisplayName() {
        return "Student (" + (department != null ? department : "General") + " - Year " + year + ")";
    }

    @Override
    public String getDashboardDescription() {
        return "Student Portal: Browse campus clubs, view tailored recommendations, register for events, and manage club memberships.";
    }

    @Override
    public String getPermissionsDescription() {
        return "Permissions: [BROWSE_CLUBS, APPLY_MEMBERSHIP, REGISTER_EVENTS, VIEW_ANNOUNCEMENTS, VIEW_RECOMMENDATIONS]";
    }

    // ==========================================================
    // [OOP: Composition Methods - Managing Child Objects]
    // ==========================================================

    public void addMembership(Membership membership) {
        if (membership != null && !memberships.contains(membership)) {
            memberships.add(membership);
        }
    }

    public void removeMembership(Membership membership) {
        memberships.remove(membership);
    }

    public List<Membership> getMemberships() {
        return Collections.unmodifiableList(memberships);
    }

    public void addRegistration(EventRegistration registration) {
        if (registration != null && !registrations.contains(registration)) {
            registrations.add(registration);
        }
    }

    public void addEventRegistration(EventRegistration registration) {
        addRegistration(registration);
    }

    public List<EventRegistration> getRegistrations() {
        return Collections.unmodifiableList(registrations);
    }

    public List<EventRegistration> getEventRegistrations() {
        return Collections.unmodifiableList(registrations);
    }

    // ==========================================================
    // [OOP: Encapsulation - Accessors and Mutators]
    // ==========================================================

    public int getStudentTableId() {
        return studentTableId > 0 ? studentTableId : getUserId();
    }

    public void setStudentTableId(int studentTableId) {
        this.studentTableId = studentTableId;
    }

    public int getStudentId() {
        return getStudentTableId();
    }

    public void setStudentId(int studentId) {
        setStudentTableId(studentId);
    }

    public String getRegisterNumber() {
        return registerNumber;
    }

    public void setRegisterNumber(String registerNumber) {
        this.registerNumber = registerNumber;
    }

    // USN / Roll number aliases
    public String getUsnRollNo() {
        return registerNumber;
    }

    public void setUsnRollNo(String usnRollNo) {
        this.registerNumber = usnRollNo;
    }

    public String getDepartment() {
        return department;
    }

    public void setDepartment(String department) {
        this.department = department;
    }

    public int getYear() {
        return year;
    }

    public void setYear(int year) {
        if (year > 0 && year <= 6) {
            this.year = year;
        }
    }

    public int getYearOfStudy() {
        return year;
    }

    public void setYearOfStudy(int yearOfStudy) {
        setYear(yearOfStudy);
    }

    public int getAcademicYear() {
        return year;
    }

    public void setAcademicYear(int academicYear) {
        setYear(academicYear);
    }

    public double getCgpa() {
        return cgpa;
    }

    public void setCgpa(double cgpa) {
        this.cgpa = cgpa;
    }

    public int getSemester() {
        return semester;
    }

    public void setSemester(int semester) {
        if (semester > 0 && semester <= 12) {
            this.semester = semester;
        }
    }

    public String getPhone() {
        return getPhoneNumber();
    }

    public void setPhone(String phone) {
        setPhoneNumber(phone);
    }

    public String getInterests() {
        return interests;
    }

    public void setInterests(String interests) {
        this.interests = interests;
    }

    /**
     * [Data Structures: HashSet]
     * Parses the comma-separated interests string into a Set of unique trimmed tokens.
     */
    public java.util.Set<String> getInterestsSet() {
        java.util.Set<String> set = new java.util.LinkedHashSet<>();
        if (interests != null && !interests.trim().isEmpty()) {
            for (String part : interests.split("[,;]+")) {
                String token = part.trim();
                if (!token.isEmpty()) {
                    set.add(token);
                }
            }
        }
        return set;
    }

    /**
     * Updates the internal comma-separated interests string from a Set.
     */
    public void setInterestsFromSet(java.util.Set<String> interestSet) {
        if (interestSet == null || interestSet.isEmpty()) {
            this.interests = "";
        } else {
            this.interests = String.join(", ", interestSet);
        }
    }

    public void addInterest(String interest) {
        if (interest != null && !interest.trim().isEmpty()) {
            java.util.Set<String> set = getInterestsSet();
            set.add(interest.trim());
            setInterestsFromSet(set);
        }
    }

    public void removeInterest(String interest) {
        if (interest != null && !interest.trim().isEmpty()) {
            java.util.Set<String> set = getInterestsSet();
            set.removeIf(s -> s.equalsIgnoreCase(interest.trim()));
            setInterestsFromSet(set);
        }
    }

    public boolean hasInterest(String interest) {
        if (interest == null || interest.trim().isEmpty()) {
            return false;
        }
        for (String s : getInterestsSet()) {
            if (s.equalsIgnoreCase(interest.trim())) {
                return true;
            }
        }
        return false;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        if (!super.equals(o)) return false;
        Student student = (Student) o;
        return Objects.equals(registerNumber, student.registerNumber);
    }

    @Override
    public int hashCode() {
        return Objects.hash(super.hashCode(), registerNumber);
    }

    @Override
    public String toString() {
        return String.format("Student: %s [Reg No: %s, Dept: %s, Year: %d]", getFullName(), registerNumber, department, year);
    }
}

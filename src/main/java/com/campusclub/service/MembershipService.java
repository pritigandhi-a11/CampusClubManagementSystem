package com.campusclub.service;

import com.campusclub.dao.ClubDAO;
import com.campusclub.dao.MembershipDAO;
import com.campusclub.exception.ClubNotFoundException;
import com.campusclub.exception.DatabaseOperationException;
import com.campusclub.exception.DuplicateMembershipException;
import com.campusclub.model.Membership;

import java.time.LocalDateTime;
import java.util.List;

/**
 * Service managing student club membership lifecycle, business rules, and approvals.
 * Implements MembershipOperations interface (Abstraction).
 */
public class MembershipService implements MembershipOperations {
    private final MembershipDAO membershipDAO;
    private final ClubDAO clubDAO;

    public MembershipService() {
        this.membershipDAO = new MembershipDAO();
        this.clubDAO = new ClubDAO();
    }

    public MembershipService(MembershipDAO membershipDAO, ClubDAO clubDAO) {
        this.membershipDAO = membershipDAO;
        this.clubDAO = clubDAO;
    }

    /**
     * Applies for membership in a club.
     * Enforces Business Constraints:
     * 1. Valid student and club IDs.
     * 2. Club must exist.
     * 3. One student cannot join the same club twice (DuplicateMembershipException).
     *
     * @param studentId ID of the student applying
     * @param clubId    ID of the club
     * @return generated membership ID
     * @throws DuplicateMembershipException if already a member or pending
     * @throws ClubNotFoundException        if club does not exist
     * @throws DatabaseOperationException   if database error occurs
     */
    @Override
    public int applyForMembership(int studentId, int clubId)
            throws DuplicateMembershipException, ClubNotFoundException, DatabaseOperationException {
        // 1. Parameter Validation
        if (studentId <= 0) {
            throw new IllegalArgumentException("Invalid student ID: " + studentId);
        }
        if (clubId <= 0) {
            throw new IllegalArgumentException("Invalid club ID: " + clubId);
        }

        // 2. Verify Club Exists
        if (clubDAO.findById(clubId).isEmpty()) {
            throw new ClubNotFoundException("Cannot apply for membership. Club ID " + clubId + " does not exist.");
        }

        // 3. Enforce Duplicate Membership Rule
        if (membershipDAO.isAlreadyMember(studentId, clubId)) {
            throw new DuplicateMembershipException("You already have an active or pending membership in this club.");
        }

        Membership membership = new Membership();
        membership.setStudentId(studentId);
        membership.setClubId(clubId);
        membership.setJoinedAt(LocalDateTime.now());
        membership.setMembershipStatus("PENDING");
        membership.setRoleInClub("MEMBER");

        return membershipDAO.createMembership(membership);
    }

    @Override
    public boolean leaveClub(int studentId, int clubId) throws DatabaseOperationException {
        if (studentId <= 0 || clubId <= 0) {
            throw new IllegalArgumentException("Invalid student ID or club ID for leaving club.");
        }
        return membershipDAO.leaveClub(studentId, clubId);
    }

    @Override
    public List<Membership> getStudentMemberships(int studentId) throws DatabaseOperationException {
        if (studentId <= 0) {
            throw new IllegalArgumentException("Invalid student ID: " + studentId);
        }
        return membershipDAO.findStudentMemberships(studentId);
    }

    @Override
    public List<Membership> getClubMembers(int clubId) throws DatabaseOperationException {
        if (clubId <= 0) {
            throw new IllegalArgumentException("Invalid club ID: " + clubId);
        }
        return membershipDAO.findClubMembers(clubId);
    }

    @Override
    public boolean isAlreadyMember(int studentId, int clubId) throws DatabaseOperationException {
        if (studentId <= 0 || clubId <= 0) {
            return false;
        }
        return membershipDAO.isAlreadyMember(studentId, clubId);
    }

    @Override
    public boolean approveMembership(int membershipId) throws DatabaseOperationException {
        if (membershipId <= 0) {
            throw new IllegalArgumentException("Invalid membership ID: " + membershipId);
        }
        return membershipDAO.updateStatus(membershipId, "APPROVED");
    }

    @Override
    public boolean rejectMembership(int membershipId) throws DatabaseOperationException {
        if (membershipId <= 0) {
            throw new IllegalArgumentException("Invalid membership ID: " + membershipId);
        }
        return membershipDAO.updateStatus(membershipId, "REJECTED");
    }
}

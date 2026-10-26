package com.campusclub.service;

import com.campusclub.exception.ClubNotFoundException;
import com.campusclub.exception.DatabaseOperationException;
import com.campusclub.exception.DuplicateMembershipException;
import com.campusclub.model.Membership;

import java.util.List;

/**
 * Interface defining operations on Club Memberships.
 * Demonstrates Abstraction and Interface Segregation in OOP.
 */
public interface MembershipOperations {
    int applyForMembership(int studentId, int clubId)
            throws DuplicateMembershipException, ClubNotFoundException, DatabaseOperationException;
    boolean leaveClub(int studentId, int clubId) throws DatabaseOperationException;
    List<Membership> getStudentMemberships(int studentId) throws DatabaseOperationException;
    List<Membership> getClubMembers(int clubId) throws DatabaseOperationException;
    boolean isAlreadyMember(int studentId, int clubId) throws DatabaseOperationException;
    boolean approveMembership(int membershipId) throws DatabaseOperationException;
    boolean rejectMembership(int membershipId) throws DatabaseOperationException;
}

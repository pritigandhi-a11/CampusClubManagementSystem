package com.campusclub.service;

import com.campusclub.exception.ClubNotFoundException;
import com.campusclub.exception.DatabaseOperationException;
import com.campusclub.model.Club;

import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * Interface defining operations on Clubs.
 * Demonstrates Abstraction and Interface Segregation in OOP.
 */
public interface ClubOperations {
    List<Club> getActiveClubs() throws DatabaseOperationException;
    List<Club> getAllClubs() throws DatabaseOperationException;
    Club getClubById(int clubId) throws ClubNotFoundException, DatabaseOperationException;
    List<Club> getClubsByCategory(String category) throws DatabaseOperationException;
    Optional<Club> getClubByCoordinatorId(int coordinatorId) throws DatabaseOperationException;
    int createClub(Club club) throws DatabaseOperationException;
    boolean updateClub(Club club) throws ClubNotFoundException, DatabaseOperationException;
    boolean deleteClub(int clubId) throws ClubNotFoundException, DatabaseOperationException;

    // Search, Filter, Sort & Grouping (Data Structures: List, Map, Comparator)
    List<Club> searchClubs(String keyword) throws DatabaseOperationException;
    List<Club> sortClubsByName(List<Club> clubs, boolean ascending);
    List<Club> sortClubsByCategory(List<Club> clubs);
    Map<String, List<Club>> getClubsGroupedByCategory() throws DatabaseOperationException;
    Map<String, Integer> getClubCountByCategory() throws DatabaseOperationException;
}

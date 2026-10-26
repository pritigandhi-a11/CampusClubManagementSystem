package com.campusclub.service;

import com.campusclub.dao.ClubDAO;
import com.campusclub.exception.ClubNotFoundException;
import com.campusclub.exception.DatabaseOperationException;
import com.campusclub.model.Club;
import com.campusclub.util.ValidationUtil;

import java.util.*;

/**
 * Service managing Club domain business rules and lifecycle.
 * Implements ClubOperations interface (Abstraction).
 * Demonstrates Collections usage: ArrayList, HashMap, and Comparator.
 */
public class ClubService implements ClubOperations {
    private final ClubDAO clubDAO;

    public ClubService() {
        this.clubDAO = new ClubDAO();
    }

    public ClubService(ClubDAO clubDAO) {
        this.clubDAO = clubDAO;
    }

    @Override
    public List<Club> getActiveClubs() throws DatabaseOperationException {
        return clubDAO.findActiveClubs();
    }

    @Override
    public List<Club> getAllClubs() throws DatabaseOperationException {
        return clubDAO.findAll();
    }

    @Override
    public Club getClubById(int clubId) throws ClubNotFoundException, DatabaseOperationException {
        if (clubId <= 0) {
            throw new IllegalArgumentException("Invalid club ID: " + clubId + ". Club ID must be a positive integer.");
        }
        return clubDAO.findById(clubId)
                .orElseThrow(() -> new ClubNotFoundException("Club with ID " + clubId + " does not exist."));
    }

    @Override
    public List<Club> getClubsByCategory(String category) throws DatabaseOperationException {
        if (ValidationUtil.isNullOrEmpty(category)) {
            throw new IllegalArgumentException("Category cannot be blank.");
        }
        return clubDAO.findByCategory(category.trim());
    }

    @Override
    public Optional<Club> getClubByCoordinatorId(int coordinatorId) throws DatabaseOperationException {
        if (coordinatorId <= 0) {
            throw new IllegalArgumentException("Invalid coordinator ID: " + coordinatorId);
        }
        return clubDAO.findByCoordinatorId(coordinatorId);
    }

    @Override
    public int createClub(Club club) throws DatabaseOperationException {
        if (club == null) {
            throw new IllegalArgumentException("Club object cannot be null.");
        }
        ValidationUtil.validateRequired(club.getClubName(), "Club name");
        ValidationUtil.validateRequired(club.getCategory(), "Club category");

        return clubDAO.createClub(club);
    }

    @Override
    public boolean updateClub(Club club) throws ClubNotFoundException, DatabaseOperationException {
        if (club == null || club.getClubId() <= 0) {
            throw new IllegalArgumentException("Invalid club entity for update.");
        }
        ValidationUtil.validateRequired(club.getClubName(), "Club name");
        ValidationUtil.validateRequired(club.getCategory(), "Club category");

        // Verify existence before update
        getClubById(club.getClubId());
        return clubDAO.updateClub(club);
    }

    @Override
    public boolean deleteClub(int clubId) throws ClubNotFoundException, DatabaseOperationException {
        if (clubId <= 0) {
            throw new IllegalArgumentException("Invalid club ID for deletion: " + clubId);
        }
        getClubById(clubId);
        return clubDAO.deleteClub(clubId);
    }

    // ==========================================================
    // Collections, Searching, Sorting & Statistics
    // ==========================================================

    /**
     * Searches clubs by matching keywords in name, category, or description.
     */
    @Override
    public List<Club> searchClubs(String keyword) throws DatabaseOperationException {
        List<Club> allClubs = getAllClubs();
        if (keyword == null || keyword.trim().isEmpty()) {
            return allClubs;
        }
        String term = keyword.trim().toLowerCase();
        List<Club> results = new ArrayList<>();
        for (Club c : allClubs) {
            boolean nameMatch = c.getClubName() != null && c.getClubName().toLowerCase().contains(term);
            boolean categoryMatch = c.getCategory() != null && c.getCategory().toLowerCase().contains(term);
            boolean descMatch = c.getDescription() != null && c.getDescription().toLowerCase().contains(term);
            if (nameMatch || categoryMatch || descMatch) {
                results.add(c);
            }
        }
        return results;
    }

    /**
     * Sorts clubs alphabetically by name using a Comparator.
     */
    @Override
    public List<Club> sortClubsByName(List<Club> clubs, boolean ascending) {
        if (clubs == null) return Collections.emptyList();
        List<Club> sorted = new ArrayList<>(clubs);
        Comparator<Club> comparator = Comparator.comparing(c -> c.getClubName() != null ? c.getClubName().toLowerCase() : "");
        if (!ascending) {
            comparator = comparator.reversed();
        }
        sorted.sort(comparator);
        return sorted;
    }

    /**
     * Sorts clubs by category, then by name.
     */
    @Override
    public List<Club> sortClubsByCategory(List<Club> clubs) {
        if (clubs == null) return Collections.emptyList();
        List<Club> sorted = new ArrayList<>(clubs);
        sorted.sort(Comparator.comparing((Club c) -> c.getCategory() != null ? c.getCategory().toLowerCase() : "")
                .thenComparing(c -> c.getClubName() != null ? c.getClubName().toLowerCase() : ""));
        return sorted;
    }

    /**
     * Groups clubs by category using a HashMap.
     */
    @Override
    public Map<String, List<Club>> getClubsGroupedByCategory() throws DatabaseOperationException {
        List<Club> allClubs = getAllClubs();
        Map<String, List<Club>> grouped = new LinkedHashMap<>();
        for (Club c : allClubs) {
            String category = c.getCategory() != null ? c.getCategory() : "Uncategorized";
            grouped.computeIfAbsent(category, k -> new ArrayList<>()).add(c);
        }
        return grouped;
    }

    /**
     * Computes statistics: count of clubs in each category using a HashMap.
     */
    @Override
    public Map<String, Integer> getClubCountByCategory() throws DatabaseOperationException {
        List<Club> allClubs = getAllClubs();
        Map<String, Integer> countMap = new LinkedHashMap<>();
        for (Club c : allClubs) {
            String cat = c.getCategory() != null ? c.getCategory() : "General";
            countMap.put(cat, countMap.getOrDefault(cat, 0) + 1);
        }
        return countMap;
    }
}

package com.campusclub.recommendation;

import com.campusclub.model.Club;
import com.campusclub.model.Membership;
import com.campusclub.model.Student;

import java.util.*;
import java.util.stream.Collectors;

/**
 * Rule-Based Smart Club Recommendation Engine.
 *
 * This module computes transparent, explainable recommendations locally
 * using deterministic rule-based scoring without any external machine-learning API.
 *
 * Data Structures Used:
 * - HashSet: Fast O(1) exclusion of already-joined clubs & deduplicated interest keywords.
 * - HashMap: Domain taxonomy & keyword synonym dictionaries.
 * - PriorityQueue (Max-Heap) / ArrayList: Score-based ranking & top-K selection.
 */
public class ClubRecommendationEngine {

    // Domain Synonym & Related Keyword Dictionary (Data Structure: HashMap of Sets)
    private static final Map<String, Set<String>> RELATED_KEYWORDS_MAP = new HashMap<>();

    static {
        RELATED_KEYWORDS_MAP.put("programming", Set.of("coding", "web development", "software", "algorithms", "hackathon", "python", "java", "tech", "development"));
        RELATED_KEYWORDS_MAP.put("coding", Set.of("programming", "web development", "software", "algorithms", "hackathon", "python", "java", "ai", "tech"));
        RELATED_KEYWORDS_MAP.put("robotics", Set.of("iot", "hardware", "electronics", "automation", "sensors", "embedded", "autonomous", "arduino"));
        RELATED_KEYWORDS_MAP.put("artificial intelligence", Set.of("ai", "machine learning", "data science", "deep learning", "neural", "python", "analytics"));
        RELATED_KEYWORDS_MAP.put("ai", Set.of("artificial intelligence", "machine learning", "data science", "deep learning", "python", "analytics"));
        RELATED_KEYWORDS_MAP.put("web development", Set.of("programming", "coding", "javascript", "fullstack", "frontend", "backend", "web", "html"));
        RELATED_KEYWORDS_MAP.put("sports", Set.of("athletics", "fitness", "badminton", "football", "cricket", "basketball", "table tennis", "gym"));
        RELATED_KEYWORDS_MAP.put("music", Set.of("singing", "band", "acoustic", "instruments", "vocal", "cultural", "audio", "concert"));
        RELATED_KEYWORDS_MAP.put("photography", Set.of("media", "filmmaking", "visuals", "editing", "camera", "design", "creative", "video"));
        RELATED_KEYWORDS_MAP.put("entrepreneurship", Set.of("startup", "innovation", "business", "leadership", "finance", "pitching", "venture", "ecell"));
        RELATED_KEYWORDS_MAP.put("social service", Set.of("volunteering", "ngo", "community", "outreach", "environment", "rotaract", "social", "service"));
        RELATED_KEYWORDS_MAP.put("cultural activities", Set.of("drama", "theatre", "dance", "arts", "music", "cultural", "literary"));
        RELATED_KEYWORDS_MAP.put("cultural", Set.of("drama", "theatre", "dance", "arts", "music", "cultural", "literary", "creative"));
    }

    /**
     * Default recommend method returning top 3 recommendations.
     */
    public List<ClubRecommendation> recommendClubs(Student student, List<Club> availableClubs, List<Membership> currentMemberships) {
        return getTopRecommendations(student, availableClubs, currentMemberships, 3);
    }

    /**
     * Recommends up to maxResults clubs ranked by score.
     *
     * @param student            the student seeking club recommendations
     * @param availableClubs     all active campus clubs
     * @param currentMemberships student's existing club memberships
     * @param maxResults         maximum number of recommendations to return (e.g. 3)
     * @return ranked list of top ClubRecommendation objects
     */
    public List<ClubRecommendation> getTopRecommendations(Student student, List<Club> availableClubs,
                                                          List<Membership> currentMemberships, int maxResults) {
        if (student == null || availableClubs == null || availableClubs.isEmpty()) {
            return Collections.emptyList();
        }

        // [Data Structure: HashSet for O(1) membership lookup]
        Set<Integer> joinedClubIds = new HashSet<>();
        if (currentMemberships != null) {
            for (Membership m : currentMemberships) {
                if (m != null && !"REJECTED".equalsIgnoreCase(m.getMembershipStatus())) {
                    joinedClubIds.add(m.getClubId());
                }
            }
        }

        // [Data Structure: HashSet for unique parsed interest tokens]
        Set<String> studentInterests = extractNormalizedInterests(student);
        String department = student.getDepartment() != null ? student.getDepartment().toLowerCase().trim() : "";

        // [Data Structure: PriorityQueue Max-Heap ranked by score descending]
        PriorityQueue<ClubRecommendation> rankingHeap = new PriorityQueue<>(
                Comparator.comparingInt(ClubRecommendation::getScore).reversed()
        );

        for (Club club : availableClubs) {
            if (club == null) continue;

            // Constraint: Exclude already joined clubs
            if (joinedClubIds.contains(club.getClubId())) {
                continue;
            }

            int score = 0;
            Set<String> matchedInterests = new LinkedHashSet<>();
            List<String> reasons = new ArrayList<>();

            String clubCategory = club.getCategory() != null ? club.getCategory().toLowerCase().trim() : "";
            String clubName = club.getClubName() != null ? club.getClubName().toLowerCase().trim() : "";
            String clubDesc = club.getDescription() != null ? club.getDescription().toLowerCase().trim() : "";

            // Rule 1: Exact Interest / Category Match (+50 Points)
            for (String interest : studentInterests) {
                if (clubCategory.equalsIgnoreCase(interest) ||
                    (interest.contains("tech") && clubCategory.equals("technical")) ||
                    (interest.contains("sport") && clubCategory.equals("sports")) ||
                    (interest.contains("cultur") && clubCategory.equals("cultural"))) {
                    score += 50;
                    matchedInterests.add(capitalize(interest));
                    reasons.add("Matches your interest category in '" + capitalize(interest) + "' (+50 pts)");
                    break;
                }
            }

            // Rule 2: Club Name Contains Interest Keyword (+25 Points)
            for (String interest : studentInterests) {
                if (clubName.contains(interest) || (interest.length() > 3 && clubName.contains(interest.substring(0, 4)))) {
                    score += 25;
                    matchedInterests.add(capitalize(interest));
                    reasons.add("Club name aligns with your interest in '" + capitalize(interest) + "' (+25 pts)");
                    break;
                }
            }

            // Rule 3: Club Description Contains Interest Keyword (+15 Points)
            for (String interest : studentInterests) {
                if (clubDesc.contains(interest)) {
                    score += 15;
                    matchedInterests.add(capitalize(interest));
                    reasons.add("Club activities mention '" + capitalize(interest) + "' (+15 pts)");
                    break;
                }
            }

            // Rule 4: Related Keyword / Domain Synonym Match (+10 Points)
            for (String interest : studentInterests) {
                Set<String> synonyms = RELATED_KEYWORDS_MAP.get(interest);
                if (synonyms != null) {
                    for (String syn : synonyms) {
                        if (clubName.contains(syn) || clubDesc.contains(syn)) {
                            score += 10;
                            matchedInterests.add(capitalize(interest));
                            reasons.add("Related domain keyword match on '" + syn + "' for '" + capitalize(interest) + "' (+10 pts)");
                            break;
                        }
                    }
                }
            }

            // Rule 5: Department Alignment Bonus (+15 Points)
            if (!department.isEmpty()) {
                if (department.contains("computer") || department.contains("information") || department.contains("cse") || department.contains("ise")) {
                    if ("technical".equalsIgnoreCase(clubCategory) || clubName.contains("coding") || clubName.contains("robotics") || clubName.contains("ai")) {
                        score += 15;
                        reasons.add("Strong alignment with your department (" + student.getDepartment() + ") (+15 pts)");
                    }
                } else if (department.contains("electronics") || department.contains("ece") || department.contains("telecom")) {
                    if (clubName.contains("robotics") || clubName.contains("iot") || clubDesc.contains("hardware")) {
                        score += 15;
                        reasons.add("Hardware & embedded focus matches " + student.getDepartment() + " (+15 pts)");
                    }
                } else if (department.contains("management") || department.contains("mba") || department.contains("bba")) {
                    if (clubName.contains("entrepreneur") || clubCategory.contains("leadership") || clubDesc.contains("startup")) {
                        score += 15;
                        reasons.add("Business & leadership focus matches " + student.getDepartment() + " (+15 pts)");
                    }
                }
            }

            // Candidate qualifying threshold
            if (score > 0) {
                rankingHeap.offer(new ClubRecommendation(club, score, matchedInterests, reasons));
            }
        }

        // [Data Structure: ArrayList for top-K results]
        List<ClubRecommendation> topResults = new ArrayList<>();
        int count = 0;
        int limit = maxResults > 0 ? maxResults : 3;
        while (!rankingHeap.isEmpty() && count < limit) {
            topResults.add(rankingHeap.poll());
            count++;
        }

        return topResults;
    }

    /**
     * Extracts normalized, trimmed, lowercase interest tokens from a student.
     */
    public Set<String> extractNormalizedInterests(Student student) {
        if (student == null) {
            return Collections.emptySet();
        }
        Set<String> tokens = new HashSet<>();
        if (student.getInterests() != null && !student.getInterests().trim().isEmpty()) {
            for (String raw : student.getInterests().split("[,;]+")) {
                String trimmed = raw.trim().toLowerCase();
                if (!trimmed.isEmpty()) {
                    tokens.add(trimmed);
                }
            }
        }
        return tokens;
    }

    private static String capitalize(String str) {
        if (str == null || str.isEmpty()) return "";
        return Character.toUpperCase(str.charAt(0)) + str.substring(1);
    }

    // ==========================================================
    // Recommendation Result DTO / Helper Class
    // ==========================================================

    /**
     * Encapsulates a transparent, explainable recommendation result.
     */
    public static class ClubRecommendation {
        private final Club club;
        private final int score;
        private final Set<String> matchedInterests;
        private final List<String> reasons;

        public ClubRecommendation(Club club, int score, Set<String> matchedInterests, List<String> reasons) {
            this.club = club;
            this.score = score;
            this.matchedInterests = matchedInterests != null ? matchedInterests : Collections.emptySet();
            this.reasons = reasons != null ? reasons : Collections.emptyList();
        }

        public Club getClub() {
            return club;
        }

        public int getScore() {
            return score;
        }

        /**
         * Calculates normalized percentage match capped at 100%.
         */
        public int getMatchPercentage() {
            return Math.min(100, Math.max(0, (score * 100) / 100));
        }

        public Set<String> getMatchedInterests() {
            return Collections.unmodifiableSet(matchedInterests);
        }

        public List<String> getReasons() {
            return Collections.unmodifiableList(reasons);
        }

        public String getReasonSummary() {
            if (matchedInterests.isEmpty()) {
                return !reasons.isEmpty() ? reasons.get(0) : "General category match";
            }
            return "Recommended because you selected " + String.join(" and ", matchedInterests) + " (Score: " + score + ")";
        }

        public String getExplanation() {
            return String.format("%s [Score: %d | Match: %d%%] - %s",
                    club.getClubName(), score, getMatchPercentage(), getReasonSummary());
        }

        @Override
        public String toString() {
            return getExplanation();
        }
    }
}

package com.dailycode.bot.domain.problem;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Objects;

@Entity
@Table(name = "problems", uniqueConstraints = {
        @UniqueConstraint(name = "uk_problems_leetcode_question_id", columnNames = "leetcode_question_id"),
        @UniqueConstraint(name = "uk_problems_title_slug", columnNames = "title_slug")
})
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Problem {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @NotBlank
    @Size(max = 50)
    @Column(name = "leetcode_question_id", nullable = false, length = 50, updatable = false)
    private String leetCodeQuestionId;

    @NotBlank
    @Size(max = 50)
    @Column(name = "frontend_id", nullable = false, length = 50)
    private String frontendId;

    @NotBlank
    @Size(max = 300)
    @Column(name = "title", nullable = false, length = 300)
    private String title;

    @NotBlank
    @Size(max = 300)
    @Pattern(regexp = "[a-z0-9]+(?:-[a-z0-9]+)*")
    @Column(name = "title_slug", nullable = false, length = 300)
    private String titleSlug;

    @NotNull
    @Enumerated(EnumType.STRING)
    @Column(name = "difficulty", nullable = false, length = 10)
    private ProblemDifficulty difficulty;

    @Column(name = "paid_only", nullable = false)
    private boolean paidOnly;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    public Problem(String leetCodeQuestionId, String frontendId, String title,
                   String titleSlug, ProblemDifficulty difficulty, boolean paidOnly) {
        this.leetCodeQuestionId = requireText(leetCodeQuestionId, "LeetCode question ID", 50);
        updateMetadata(frontendId, title, titleSlug, difficulty, paidOnly);
    }

    public void updateMetadata(String frontendId, String title, String titleSlug,
                               ProblemDifficulty difficulty, boolean paidOnly) {
        // Validate every value before changing a managed entity.
        String validFrontendId = requireText(frontendId, "Frontend ID", 50);
        String validTitle = requireText(title, "Problem title", 300);
        String validSlug = requireText(titleSlug, "Title slug", 300);
        if (!validSlug.matches("[a-z0-9]+(?:-[a-z0-9]+)*")) {
            throw new IllegalArgumentException("Title slug must contain lowercase letters or digits separated by single hyphens.");
        }
        Objects.requireNonNull(difficulty, "Problem difficulty is required.");
        this.frontendId = validFrontendId;
        this.title = validTitle;
        this.titleSlug = validSlug;
        this.difficulty = difficulty;
        this.paidOnly = paidOnly;
    }

    private static String requireText(String value, String field, int maxLength) {
        if (value == null || value.isBlank() || value.strip().length() > maxLength) {
            throw new IllegalArgumentException(field + " must contain 1 to " + maxLength + " characters.");
        }
        return value.strip();
    }

    @PrePersist
    private void onCreate() {
        Instant now = Instant.now().truncatedTo(ChronoUnit.MICROS);
        createdAt = now;
        updatedAt = now;
    }

    @PreUpdate
    private void onUpdate() {
        updatedAt = Instant.now().truncatedTo(ChronoUnit.MICROS);
    }
}

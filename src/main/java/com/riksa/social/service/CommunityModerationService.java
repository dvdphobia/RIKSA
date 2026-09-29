package com.riksa.social.service;

import com.riksa.social.entity.Category;
import com.riksa.social.entity.CommunityBan;
import com.riksa.social.entity.CommunityRule;
import com.riksa.social.entity.Post;
import com.riksa.social.entity.UserCategoryMembership;
import com.riksa.social.enums.CommunityRole;
import com.riksa.social.enums.PostPermission;
import com.riksa.social.repository.CategoryRepository;
import com.riksa.social.repository.CommunityBanRepository;
import com.riksa.social.repository.CommunityRuleRepository;
import com.riksa.social.repository.PostRepository;
import com.riksa.social.repository.UserCategoryMembershipRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.domain.PageRequest;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.UUID;

/**
 * The tools a founder has to run their community: settings, rules,
 * moderators, removing and banning members, and a pinned post.
 *
 * <p>Official communities have no founder, so nobody inside the app can
 * moderate them; staff use the admin panel for those.
 */
@Service
@RequiredArgsConstructor
public class CommunityModerationService {

    static final int MAX_RULES = 15;
    static final int RULE_TITLE_MAX = 100;
    static final int RULE_BODY_MAX = 500;
    static final int REASON_MAX = 200;
    static final int MEMBER_PAGE_MAX = 100;

    private final CategoryRepository categoryRepository;
    private final UserCategoryMembershipRepository membershipRepository;
    private final CommunityRuleRepository ruleRepository;
    private final CommunityBanRepository banRepository;
    private final PostRepository postRepository;
    private final MediaUrlValidator mediaUrlValidator;

    public record RuleInput(String title, String body) {}

    // ── Reads ─────────────────────────────────────────────

    public Category getCommunity(UUID categoryId) {
        return categoryRepository.findById(categoryId)
                .orElseThrow(() -> new IllegalArgumentException("Community not found"));
    }

    /** The viewer's role, or null when they are not a member. */
    public CommunityRole roleOf(UUID userId, Category category) {
        if (userId == null) return null;
        if (userId.equals(category.getCreatedBy())) return CommunityRole.OWNER;
        return membershipRepository.findByUserIdAndCategoryId(userId, category.getId())
                .map(UserCategoryMembership::getRole)
                .orElse(null);
    }

    public boolean canModerate(UUID userId, UUID categoryId) {
        if (userId == null || categoryId == null) return false;
        return categoryRepository.findById(categoryId)
                .map(category -> {
                    CommunityRole role = roleOf(userId, category);
                    return role != null && role.canModerate();
                })
                .orElse(false);
    }

    public boolean isBanned(UUID userId, UUID categoryId) {
        return userId != null && banRepository.existsByCategoryIdAndUserId(categoryId, userId);
    }

    public List<CommunityRule> rules(UUID categoryId) {
        return ruleRepository.findByCategoryIdOrderByPositionAsc(categoryId);
    }

    public List<UserCategoryMembership> members(UUID categoryId, String query, Integer limit) {
        int size = limit == null || limit <= 0 ? 50 : Math.min(limit, MEMBER_PAGE_MAX);
        String text = query == null ? "" : query.strip().toLowerCase(Locale.ROOT)
                .replace("\\", "\\\\").replace("%", "\\%").replace("_", "\\_");
        return membershipRepository.searchMembers(categoryId, "%" + text + "%", PageRequest.of(0, size));
    }

    public List<CommunityBan> bans(UUID actorId, UUID categoryId) {
        requireModerator(actorId, getCommunity(categoryId));
        return banRepository.findByCategoryIdOrderByCreatedAtDesc(categoryId);
    }

    // ── Posting rules used by PostService and CommentService ──

    public void requireCanPost(UUID userId, UUID categoryId) {
        Category category = getCommunity(categoryId);
        if (isBanned(userId, categoryId)) {
            throw new AccessDeniedException("You are banned from this community");
        }
        if (category.getPostPermission() == PostPermission.MODERATORS) {
            CommunityRole role = roleOf(userId, category);
            if (role == null || !role.canModerate()) {
                throw new AccessDeniedException("Only moderators can post in this community");
            }
        }
    }

    public void requireNotBanned(UUID userId, UUID categoryId) {
        if (isBanned(userId, categoryId)) {
            throw new AccessDeniedException("You are banned from this community");
        }
    }

    // ── Owner settings ────────────────────────────────────

    @Transactional
    public Category updateCommunity(UUID actorId, UUID categoryId, String rawName, String rawDescription,
                                    String iconUrl, boolean clearIcon, PostPermission postPermission,
                                    List<RuleInput> rules) {
        Category category = getCommunity(categoryId);
        requireOwner(actorId, category);

        if (rawName != null) {
            String name = CommunityCreationService.normalizeName(rawName);
            if (!name.equalsIgnoreCase(category.getName()) && categoryRepository.existsByNameIgnoreCase(name)) {
                throw new IllegalArgumentException("A community with this name already exists");
            }
            category.setName(name);
        }
        if (rawDescription != null) {
            category.setDescription(CommunityCreationService.normalizeDescription(rawDescription));
        }
        if (clearIcon) {
            category.setIconUrl(null);
        } else if (iconUrl != null) {
            String url = mediaUrlValidator.normalize(iconUrl);
            if (url != null && !url.regionMatches(true, 0, "https://", 0, 8)) {
                throw new IllegalArgumentException("Community icon must use https");
            }
            category.setIconUrl(url);
        }
        if (postPermission != null) {
            category.setPostPermission(postPermission);
        }
        if (rules != null) {
            replaceRules(categoryId, rules);
        }

        try {
            return categoryRepository.saveAndFlush(category);
        } catch (DataIntegrityViolationException e) {
            throw new IllegalArgumentException("A community with this name already exists");
        }
    }

    private void replaceRules(UUID categoryId, List<RuleInput> inputs) {
        if (inputs.size() > MAX_RULES) {
            throw new IllegalArgumentException("A community can have at most " + MAX_RULES + " rules");
        }
        List<CommunityRule> rows = new ArrayList<>(inputs.size());
        for (RuleInput input : inputs) {
            String title = input.title() == null ? "" : input.title().strip();
            String body = input.body() == null || input.body().isBlank() ? null : input.body().strip();
            if (title.isEmpty()) {
                throw new IllegalArgumentException("Every rule needs a title");
            }
            if (title.codePointCount(0, title.length()) > RULE_TITLE_MAX) {
                throw new IllegalArgumentException("Rule titles must be at most " + RULE_TITLE_MAX + " characters");
            }
            if (body != null && body.codePointCount(0, body.length()) > RULE_BODY_MAX) {
                throw new IllegalArgumentException("Rule details must be at most " + RULE_BODY_MAX + " characters");
            }
            CommunityRule rule = new CommunityRule();
            rule.setCategoryId(categoryId);
            rule.setPosition(rows.size());
            rule.setTitle(title);
            rule.setBody(body);
            rows.add(rule);
        }
        ruleRepository.deleteByCategoryId(categoryId);
        ruleRepository.flush();
        ruleRepository.saveAll(rows);
    }

    @Transactional
    public UserCategoryMembership setRole(UUID actorId, UUID categoryId, UUID userId, CommunityRole role) {
        Category category = getCommunity(categoryId);
        requireOwner(actorId, category);
        if (role == CommunityRole.OWNER) {
            throw new IllegalArgumentException("A community has exactly one owner");
        }
        if (userId.equals(category.getCreatedBy())) {
            throw new IllegalStateException("The owner's role can't be changed");
        }
        UserCategoryMembership membership = membershipRepository.findByUserIdAndCategoryId(userId, categoryId)
                .orElseThrow(() -> new IllegalArgumentException("This person is not a member"));
        membership.setRole(role);
        return membershipRepository.save(membership);
    }

    // ── Moderator actions ─────────────────────────────────

    /** Removes a member, and with {@code ban} keeps them from joining again. */
    @Transactional
    public void removeMember(UUID actorId, UUID categoryId, UUID userId, boolean ban, String reason) {
        Category category = getCommunity(categoryId);
        CommunityRole actorRole = requireModerator(actorId, category);
        if (userId.equals(actorId)) {
            throw new IllegalStateException("You can't remove yourself");
        }
        CommunityRole targetRole = roleOf(userId, category);
        // A moderator can act on members only; the owner on anyone but themself.
        if (targetRole != null && targetRole.compareTo(actorRole) >= 0) {
            throw new AccessDeniedException("You can't remove someone with the same or a higher role");
        }

        membershipRepository.deleteByUserIdAndCategoryId(userId, categoryId);
        if (ban && !banRepository.existsByCategoryIdAndUserId(categoryId, userId)) {
            CommunityBan row = new CommunityBan();
            row.setCategoryId(categoryId);
            row.setUserId(userId);
            row.setBannedBy(actorId);
            row.setReason(cleanReason(reason));
            banRepository.save(row);
        }
    }

    @Transactional
    public void unban(UUID actorId, UUID categoryId, UUID userId) {
        requireModerator(actorId, getCommunity(categoryId));
        banRepository.deleteByCategoryIdAndUserId(categoryId, userId);
    }

    @Transactional
    public Category pinPost(UUID actorId, UUID categoryId, UUID postId) {
        Category category = getCommunity(categoryId);
        requireModerator(actorId, category);
        if (postId != null) {
            Post post = postRepository.findById(postId)
                    .orElseThrow(() -> new IllegalArgumentException("Post not found"));
            if (!categoryId.equals(post.getCategoryId())) {
                throw new IllegalArgumentException("That post is not in this community");
            }
        }
        category.setPinnedPostId(postId);
        return categoryRepository.save(category);
    }

    // ── Guards ────────────────────────────────────────────

    private void requireOwner(UUID actorId, Category category) {
        if (actorId == null || !actorId.equals(category.getCreatedBy())) {
            throw new AccessDeniedException("Only the community owner can do that");
        }
    }

    private CommunityRole requireModerator(UUID actorId, Category category) {
        CommunityRole role = roleOf(actorId, category);
        if (role == null || !role.canModerate()) {
            throw new AccessDeniedException("Only community moderators can do that");
        }
        return role;
    }

    private static String cleanReason(String reason) {
        if (reason == null || reason.isBlank()) return null;
        String value = reason.strip();
        if (value.codePointCount(0, value.length()) > REASON_MAX) {
            throw new IllegalArgumentException("Reason must be at most " + REASON_MAX + " characters");
        }
        return value;
    }
}

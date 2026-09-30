package com.riksa.social.service;

import com.riksa.social.entity.Category;
import com.riksa.social.entity.CommunityBan;
import com.riksa.social.entity.UserCategoryMembership;
import com.riksa.social.enums.CommunityRole;
import com.riksa.social.enums.PostPermission;
import com.riksa.social.repository.CategoryRepository;
import com.riksa.social.repository.CommunityBanRepository;
import com.riksa.social.repository.CommunityRuleRepository;
import com.riksa.social.repository.PostRepository;
import com.riksa.social.repository.UserCategoryMembershipRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.springframework.security.access.AccessDeniedException;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class CommunityModerationServiceTest {

    @Mock private CategoryRepository categoryRepository;
    @Mock private UserCategoryMembershipRepository membershipRepository;
    @Mock private CommunityRuleRepository ruleRepository;
    @Mock private CommunityBanRepository banRepository;
    @Mock private PostRepository postRepository;
    @Mock private MediaUrlValidator mediaUrlValidator;

    @InjectMocks private CommunityModerationService service;

    private final UUID owner = UUID.randomUUID();
    private final UUID mod = UUID.randomUUID();
    private final UUID member = UUID.randomUUID();
    private final UUID otherMod = UUID.randomUUID();
    private Category category;

    @BeforeEach
    void setUp() {
        category = new Category();
        category.setId(UUID.randomUUID());
        category.setName("Chem Club");
        category.setCreatedBy(owner);
        when(categoryRepository.findById(category.getId())).thenReturn(Optional.of(category));
        membership(owner, CommunityRole.OWNER);
        membership(mod, CommunityRole.MODERATOR);
        membership(otherMod, CommunityRole.MODERATOR);
        membership(member, CommunityRole.MEMBER);
    }

    private void membership(UUID userId, CommunityRole role) {
        UserCategoryMembership row = new UserCategoryMembership();
        row.setUserId(userId);
        row.setCategoryId(category.getId());
        row.setRole(role);
        when(membershipRepository.findByUserIdAndCategoryId(userId, category.getId()))
                .thenReturn(Optional.of(row));
    }

    @Test
    void moderatorCanBanAMember() {
        service.removeMember(mod, category.getId(), member, true, " spam ");

        verify(membershipRepository).deleteByUserIdAndCategoryId(member, category.getId());
        verify(banRepository).save(argThat((CommunityBan ban) ->
                ban.getUserId().equals(member) && "spam".equals(ban.getReason())));
    }

    @Test
    void moderatorCannotRemoveAnotherModeratorOrTheOwner() {
        assertThatThrownBy(() -> service.removeMember(mod, category.getId(), otherMod, false, null))
                .isInstanceOf(AccessDeniedException.class);
        assertThatThrownBy(() -> service.removeMember(mod, category.getId(), owner, true, null))
                .isInstanceOf(AccessDeniedException.class);
        verify(membershipRepository, never()).deleteByUserIdAndCategoryId(any(), any());
    }

    @Test
    void memberCannotModerate() {
        assertThatThrownBy(() -> service.removeMember(member, category.getId(), mod, false, null))
                .isInstanceOf(AccessDeniedException.class);
        assertThatThrownBy(() -> service.pinPost(member, category.getId(), null))
                .isInstanceOf(AccessDeniedException.class);
    }

    @Test
    void onlyTheOwnerAppointsModeratorsAndEditsSettings() {
        assertThatThrownBy(() -> service.setRole(mod, category.getId(), member, CommunityRole.MODERATOR))
                .isInstanceOf(AccessDeniedException.class);
        assertThatThrownBy(() -> service.updateCommunity(mod, category.getId(), null, null, null, false,
                PostPermission.MODERATORS, null))
                .isInstanceOf(AccessDeniedException.class);

        when(membershipRepository.save(any())).thenAnswer(invocation -> invocation.getArgument(0));
        UserCategoryMembership row = service.setRole(owner, category.getId(), member, CommunityRole.MODERATOR);
        assertThat(row.getRole()).isEqualTo(CommunityRole.MODERATOR);
    }

    @Test
    void ownerRoleCannotBeGivenOrTaken() {
        assertThatThrownBy(() -> service.setRole(owner, category.getId(), member, CommunityRole.OWNER))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> service.setRole(owner, category.getId(), owner, CommunityRole.MEMBER))
                .isInstanceOf(IllegalStateException.class);
    }

    @Test
    void moderatorsOnlyPostingBlocksMembers() {
        category.setPostPermission(PostPermission.MODERATORS);

        assertThatThrownBy(() -> service.requireCanPost(member, category.getId()))
                .isInstanceOf(AccessDeniedException.class);
        service.requireCanPost(mod, category.getId());
        service.requireCanPost(owner, category.getId());
    }

    @Test
    void bannedUsersCannotPost() {
        when(banRepository.existsByCategoryIdAndUserId(category.getId(), member)).thenReturn(true);

        assertThatThrownBy(() -> service.requireCanPost(member, category.getId()))
                .isInstanceOf(AccessDeniedException.class);
        assertThatThrownBy(() -> service.requireNotBanned(member, category.getId()))
                .isInstanceOf(AccessDeniedException.class);
    }

    @Test
    void ownerReplacesRulesInOrder() {
        when(categoryRepository.saveAndFlush(category)).thenReturn(category);

        service.updateCommunity(owner, category.getId(), null, null, null, false, null, List.of(
                new CommunityModerationService.RuleInput(" Be kind ", ""),
                new CommunityModerationService.RuleInput("No spam", "Links need context")));

        verify(ruleRepository).deleteByCategoryId(category.getId());
        verify(ruleRepository).saveAll(argThat((List<com.riksa.social.entity.CommunityRule> rules) ->
                rules.size() == 2
                        && rules.get(0).getTitle().equals("Be kind")
                        && rules.get(0).getBody() == null
                        && rules.get(1).getPosition() == 1));
    }

    @Test
    void blankRuleTitlesAreRejected() {
        assertThatThrownBy(() -> service.updateCommunity(owner, category.getId(), null, null, null, false, null,
                List.of(new CommunityModerationService.RuleInput("  ", "body"))))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void ownerSetsAndClearsTheBannerColour() {
        when(categoryRepository.saveAndFlush(category)).thenReturn(category);

        service.updateCommunity(owner, category.getId(), null, null, null, false, null, null,
                null, false, "#7c5cff");
        assertThat(category.getBannerColor()).isEqualTo("#7C5CFF");

        service.updateCommunity(owner, category.getId(), null, null, null, false, null, null,
                null, false, "");
        assertThat(category.getBannerColor()).isNull();

        assertThatThrownBy(() -> service.updateCommunity(owner, category.getId(), null, null, null, false,
                null, null, null, false, "red; background:url(x)"))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void bannerPhotosMustUseHttps() {
        when(mediaUrlValidator.normalize("http://cdn.example/b.png")).thenReturn("http://cdn.example/b.png");
        when(mediaUrlValidator.normalize("https://cdn.example/b.png")).thenReturn("https://cdn.example/b.png");
        when(categoryRepository.saveAndFlush(category)).thenReturn(category);

        assertThatThrownBy(() -> service.updateCommunity(owner, category.getId(), null, null, null, false,
                null, null, "http://cdn.example/b.png", false, null))
                .isInstanceOf(IllegalArgumentException.class);
        service.updateCommunity(owner, category.getId(), null, null, null, false, null, null,
                "https://cdn.example/b.png", false, null);
        assertThat(category.getBannerUrl()).isEqualTo("https://cdn.example/b.png");
    }
}

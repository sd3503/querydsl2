package study.querydsl.repository;

import com.querydsl.core.BooleanBuilder;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import study.querydsl.condition.MemberSearchCondition;
import study.querydsl.domain.Member;

import java.util.List;
import java.util.Optional;

import static study.querydsl.domain.QMember.member;
import static study.querydsl.domain.QTeam.team;

public class MemberTestRepository extends Querydsl4RepositorySupport {
    public MemberTestRepository() {
        super(Member.class);
    }

    public List<Member> basicSelect() {
        return select(member)
                .from(member)
                .fetch();
    }

    public List<Member> basicSelectFrom() {
        return selectFrom(member)
                .fetch();
    }

    public Page<Member> applyPagination(MemberSearchCondition condition, Pageable pageable) {
        return applyPagination(pageable, query ->
                query.selectFrom(member)
                        .leftJoin(member.team, team)
                        .where(usernameEq(condition.getUsername()),
                                teamNameEq(condition.getTeamName()),
                                ageGoe(condition.getAgeGoe()),
                                ageLoe(condition.getAgeLoe())
                        )
        );
    }

    private BooleanBuilder ageLoe(Integer ageLoe) {
        return Optional.ofNullable(ageLoe)
                .map(member.age::loe)
                .map(BooleanBuilder::new)
                .orElseGet(BooleanBuilder::new);
    }

    private BooleanBuilder ageGoe(Integer ageGoe) {
        return Optional.ofNullable(ageGoe)
                .map(member.age::goe)
                .map(BooleanBuilder::new)
                .orElseGet(BooleanBuilder::new);
    }

    private BooleanBuilder teamNameEq(String teamName) {
        return Optional.ofNullable(teamName)
                .map(team.name::eq)
                .map(BooleanBuilder::new)
                .orElseGet(BooleanBuilder::new);
    }

    private BooleanBuilder usernameEq(String username) {
        return Optional.ofNullable(username)
                .map(member.username::eq)
                .map(BooleanBuilder::new)
                .orElseGet(BooleanBuilder::new);
    }

    public Page<Member> applyPagination2(MemberSearchCondition condition, Pageable pageable) {
        return applyPagination(pageable, contentQuery ->
                        contentQuery.selectFrom(member)
                                .where(usernameEq(condition.getUsername()),
                                        teamNameEq(condition.getTeamName()),
                                        ageGoe(condition.getAgeGoe()),
                                        ageLoe(condition.getAgeLoe())
                                ),
                countQuery ->
                        countQuery.select(member.id)
                                .from(member)
                                .where(usernameEq(condition.getUsername()),
                                        teamNameEq(condition.getTeamName()),
                                        ageGoe(condition.getAgeGoe()),
                                        ageLoe(condition.getAgeLoe())
                                ));
    }
}

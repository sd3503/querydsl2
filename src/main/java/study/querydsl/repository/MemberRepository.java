package study.querydsl.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.querydsl.QuerydslPredicateExecutor;
import study.querydsl.domain.Member;

import java.util.List;

public interface MemberRepository extends JpaRepository<Member, Long>, MemberRepositoryCustom, QuerydslPredicateExecutor<Member> {
    //spring data jpa의 경우 save, findById, findAll을 제공함 -> 즉 만들 필요가 없음

    //그러나 findByUsername은 제공하지 않음 -> 아래 처럼 만들면 됨, 메소드 이름으로 추측해서 알아서 쿼리를 쏴줌
    List<Member> findByUsername(String username);

}

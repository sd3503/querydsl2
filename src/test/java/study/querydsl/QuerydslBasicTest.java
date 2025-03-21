package study.querydsl;

import com.querydsl.core.BooleanBuilder;
import com.querydsl.core.QueryResults;
import com.querydsl.core.Tuple;
import com.querydsl.core.types.Projections;
import com.querydsl.core.types.dsl.BooleanExpression;
import com.querydsl.core.types.dsl.CaseBuilder;
import com.querydsl.core.types.dsl.Expressions;
import com.querydsl.jpa.impl.JPAQueryFactory;
import jakarta.persistence.EntityManager;
import jakarta.persistence.EntityManagerFactory;
import jakarta.persistence.PersistenceUnit;
import lombok.extern.slf4j.Slf4j;
import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.annotation.Commit;
import org.springframework.transaction.annotation.Transactional;
import study.querydsl.domain.Member;
import study.querydsl.domain.QMember;
import study.querydsl.domain.Team;
import study.querydsl.dto.MemberDto;
import study.querydsl.dto.QMemberDto;
import study.querydsl.dto.UserDto;

import java.util.List;

import static com.querydsl.jpa.JPAExpressions.select;
import static org.assertj.core.api.Assertions.*;
import static org.junit.jupiter.api.Assertions.*;
import static study.querydsl.domain.QMember.member;
import static study.querydsl.domain.QTeam.team;

@SpringBootTest
@Transactional
@Slf4j
public class QuerydslBasicTest {
    @Autowired
    EntityManager em;

    JPAQueryFactory queryFactory;

    @BeforeEach //테스트 실행 전 데이터를 넣는 작업 진행
    public void before() {
        queryFactory = new JPAQueryFactory(em);
        Team teamA = new Team("teamA");
        Team teamB = new Team("teamB");
        em.persist(teamA);
        em.persist(teamB);

        Member member1 = new Member("member1", 10, teamA);
        Member member2 = new Member("member2", 20, teamA);

        Member member3 = new Member("member3", 30, teamB);
        Member member4 = new Member("member4", 40, teamB);

        em.persist(member1);
        em.persist(member2);
        em.persist(member3);
        em.persist(member4);
    }

    @Test
    public void startJPQL() throws Exception {
        // given
        Member findMember = em.createQuery("select m from Member m where m.username = :username", Member.class)
                .setParameter("username", "member1")
                .getSingleResult();
        // when

        // then
        assertThat(findMember.getUsername()).isEqualTo("member1");
    }

    @Test
    public void startQuerydsl() throws Exception {
        // given

        //QMember m = new QMember("m");//QMember의 인자에는 별칭을 넣어줌 -> 크게 중요하지 않음. 앞으로 안 쓸 거임
        // when
        // 파라미터 바인딩 없이 사용 가능
        //m.username.eq("member1") 으로 써도 prepare statement로 파라미터 바인딩이 자동으로 진행
        //db 입장에서도 성능면에서 유리
        Member findMember = queryFactory
                .select(member)
                .from(member)
                .where(member.username.eq("member1"))
                .fetchOne();

        // then
        assertThat(findMember.getUsername()).isEqualTo("member1");

        //QMember의 별칭 설정은 같은 테이블을 조인하는 경우에만 선언해서 사용 → 그렇지 않은 경우에는 그냥 static import 사용
    }

    @Test
    public void search() throws Exception {
        // given
        //selectFrom = select + from을 합친 것
        //이름이 member1이면서(and) 나이가 10인 사람을 조회해
        //조건의 체인은 and, or 다 걸 수 있음
        Member findMember = queryFactory
                .selectFrom(member)
                .where(member.username.eq("member1")
                        .and(member.age.eq(10)))
                .fetchOne();
        // when
        member.username.eq("member1") ;// username = 'member1'
        member.username.ne("member1") ;//username != 'member1'
        member.username.eq("member1").not() ;// username != 'member1'

        member.username.isNotNull() ;//이름이 is not null

        member.age.in(10, 20) ;// age in (10,20)
        member.age.notIn(10, 20) ;// age not in (10, 20)
        member.age.between(10,30) ;//between 10, 30

        member.age.goe(30) ;// age >= 30
        member.age.gt(30) ;// age > 30
        member.age.loe(30) ;// age <= 30
        member.age.lt(30) ;// age < 30

        member.username.like("member%");//like 검색
        member.username.contains("member");// like ‘%member%’ 검색
        member.username.startsWith("member");//like ‘member%’ 검색
        // then
        assertThat(findMember.getUsername()).isEqualTo("member1");
        /*
        where의 파라미터는 여러 개 넘기면 and로 인식됨 → 요렇게 파라미터로 넘길 경우 null이 들어갔을 때 null을 무시함 → 동적 쿼리 만들 때 기가 막히다는 것
        ex) where(member.username.eq("member1", member.age.eq(10), null)
        */

        //and 조건은 쉼표로 끊어 갈 수 있음
        //where의 파라미터를 여러 개 넘기면 걔가 and로 붙음
        Member findMember1 = queryFactory
                .selectFrom(member)
                .where(
                        member.username.eq("member1"),
                        member.age.eq(10)
                )
                .fetchOne();
    }
    @Test
    public void resultFetch() {
        //리스트 조회 → 가장 많이 사용(데이터가 없다면 빈 리스트를 반환)
        List<Member> fetch = queryFactory
                .selectFrom(member)
                .fetch();
        //단 건 조회 → 결과가 없으면 null, 결과가 2개 이상이면 com.querydsl.core.NonUniqueResultException 발생
        assertThrows(Exception.class, () -> {
            Member fetchOne = queryFactory
                    .selectFrom(member)
                    .fetchOne();
        });
        //limit(1).fetchOne()와 같음 → 데이터가 없으면 null 반환
        Member fetchFirst = queryFactory
                .selectFrom(member)
                .fetchFirst();
        //페이징 정보 포함, total count 쿼리 추가 실행 → 실제 나가는 쿼리는 2개, deprecated 상태
        //count 쿼리와 data 쿼리는 달라야 성능 좋음 -> 페이징 처리에 대한 쿼리는 fetch 후 따로 자바 레벨에서 따로 count 처리 하라고 함
        QueryResults<Member> results = queryFactory
                .selectFrom(member)
                .fetchResults();
        long total = results.getTotal();
        List<Member> content = results.getResults();
        //count 수 조회 -> 얘도 deprecated됨(querydsl 5.0 버전부터)
        //fetch().size()로 처리하는 걸 권장함 → 물론 서비스 규모에 따라 적절한 count 수를 받아올 필요는 있음
        long totalCount = queryFactory
                .selectFrom(member)
                .fetchCount();
    }

    /**
     * 회원 정렬 순서
     * 1. 회원 나이 내림차순(desc)
     * 2. 회원 이름 올림차순(asc)
     * 단, 2에서 회원 이름이 없으면 마지막에 출력(nulls last)
     */
    @Test
    public void sort() {
        //예제를 위해 데이터를 더 추가하는 작업
        em.persist(new Member(null, 100));
        em.persist(new Member("member5", 100));
        em.persist(new Member("member6", 100));

        //나이 조건은 다 100살로 맞췄기 때문에 실질적으로는 회원 이름의 올림차순만 적용
        List<Member> results = queryFactory
                .selectFrom(member)
                .where(member.age.eq(100))
                .orderBy(member.age.desc(), member.username.asc().nullsLast())
                .fetch();

        Member member5 = results.get(0);
        Member member6 = results.get(1);
        Member memberNull = results.get(2);

        Assertions.assertThat(member5.getUsername()).isEqualTo("member5");
        Assertions.assertThat(member6.getUsername()).isEqualTo("member6");
        Assertions.assertThat(memberNull.getUsername()).isNull();
    }

    @Test
    public void paging1() {
        List<Member> result = queryFactory
                .selectFrom(member)
                .orderBy(member.username.desc())
                .offset(1)
                .limit(2)
                .fetch();

        Assertions.assertThat(result.size()).isEqualTo(2);
    }
    @Test
    public void aggregation() {
        //아래의 예시처럼 내가 원하는 column으로 선택 시 Tuple이라는 것으로 조회하게 됨
        List<Tuple> result = queryFactory
                .select(
                        member.count(),
                        member.age.sum(),
                        member.age.avg(),
                        member.age.max(),
                        member.age.min()
                )
                .from(member)
                .fetch();

        Tuple tuple = result.get(0);
        Assertions.assertThat(tuple.get(member.count())).isEqualTo(4);
        Assertions.assertThat(tuple.get(member.age.sum())).isEqualTo(100);
        Assertions.assertThat(tuple.get(member.age.avg())).isEqualTo(25);
        Assertions.assertThat(tuple.get(member.age.max())).isEqualTo(40);
        Assertions.assertThat(tuple.get(member.age.min())).isEqualTo(10);
        //실무에서는 Tuple은 잘 안씀 → DTO로 직접 뽑아오는 방법을 주로 선호함
        //인텔리제이에서 오류 있는 부분으로 바로 이동하는 단축키는 F2
    }

    /**
     * 팀의 이름과 각 팀의 평균 연령을 구해라.
     */
    @Test
    public void group() {
        List<Tuple> result = queryFactory
                .select(team.name, member.age.avg())
                .from(member)
                .join(member.team, team)
                .groupBy(team.name)
                .fetch();

        Tuple teamA = result.get(0);
        Tuple teamB = result.get(1);

        assertThat(teamA.get(team.name)).isEqualTo("teamA");
        assertThat(teamA.get(member.age.avg())).isEqualTo(15); //(10 + 20) / 2
        assertThat(teamB.get(team.name)).isEqualTo("teamB");
        assertThat(teamB.get(member.age.avg())).isEqualTo(35); //(30 + 40) / 2
    }

    /**
     * 팀 A에 소속된 모든 회원 찾기
     */
    @Test
    public void join() {
        List<Member> result = queryFactory
                .selectFrom(member)
//              .join(member.team, QTeam.team) 와 같음
//              .leftJoin(member.team, team)으로도 쓸 수 있음
                .join(member.team, team)
                .where(team.name.eq("teamA"))
                .fetch();

        assertThat(result)
                .extracting("username") //column 명 username에
                .containsExactly("member1", "member2"); //member1과 member2가 포함되어 있는가

    }
    @Test
    public void theta_join() {
        em.persist(new Member("teamA"));
        em.persist(new Member("teamB"));
        em.persist(new Member("teamC"));

        List<Member> result = queryFactory
                .select(member)
                .from(member, team)
                .where(member.username.eq(team.name))
                .fetch();

        //모든 member와 모든 team을 조회 후 한꺼번에 조인 -> where절에서 필터링 진행 -> 이걸 theta join이라고 부름
        //물론 db마다 성능 최적화는 진행해줌

        assertThat(result)
                .extracting("username")
                .containsExactly("teamA", "teamB");
        //theta join 시 from절에 여러 엔티티를 선택 → 단, 외부 조인 불가능(left outer, right outer join)
        //cross join을 해버림
    }
    /**
     * 예) 회원과 팀을 조인하면서, 팀  이름이 teamA인 팀만 조인, 회원은 모두 조회
     * JPQL : select m, t from Member m left join m.team t on t.name = 'teamA';
     */
    @Test
    public void join_on_filtering() {
        List<Tuple> result = queryFactory
                .select(member, team)
                .from(member)
                .leftJoin(member.team, team)
                .on(team.name.eq("teamA"))
                .fetch();

        for (Tuple tuple : result) {
            System.out.println("tuple = " + tuple);
        }
        //내부 조인은 on을 사용하지 말고 where로 필터링 하는 것을 권장(똑같음)
    }

    /**
     * 연관 관계가 없는 외부 엔티티 조인
     * 회원의 이름이 팀 이름과 같은 대상 외부 조인
     */
    @Test
    public void join_on_no_relation() {
        em.persist(new Member("teamA"));
        em.persist(new Member("teamB"));
        em.persist(new Member("teamC"));

        List<Tuple> result = queryFactory
                .select(member, team)
                .from(member)
                .leftJoin(team) //기존에는 leftJoin(member.team, team)으로 썼는데 파라미터로 team 하나만 넘겨줌
                .on(member.username.eq("teamA"))
                .fetch();

        for (Tuple tuple : result) {
            System.out.println("tuple : " + tuple);
        }
    }
    @PersistenceUnit
    EntityManagerFactory emf;

    @Test
    public void fetchJoinNo() {
        //페치 조인 시 올바른 결과를 보려면 영속성 컨텍스트를 비워줘야 함
        em.flush();
        em.clear();

        Member findMember = queryFactory
                .selectFrom(member)
                .where(member.username.eq("member1"))
                .fetchOne();

        //EntityManagerFactory emf의 isLoaded는 해당 엔티티가 이미 로딩된 엔티티인지, 초기화가 안된 엔티티인지 가르쳐 주는 녀석
        boolean loaded = emf.getPersistenceUnitUtil().isLoaded(findMember.getTeam());
        assertThat(loaded).as("패치 조인 미적용").isFalse();
    }
    @Test
    public void fetchJoinUse() {
        //페치 조인 시 올바른 결과를 보려면 영속성 컨텍스트를 비워줘야 함
        em.flush();
        em.clear();

        Member findMember = queryFactory
                .selectFrom(member)
                .join(member.team, team).fetchJoin() //join을 해주되 fetchJoin을 추가하면 됨
                .where(member.username.eq("member1"))
                .fetchOne();

        boolean loaded = emf.getPersistenceUnitUtil().isLoaded(findMember.getTeam());
        assertThat(loaded).as("패치 조인 적용").isTrue();
        //페치 조인은 정말 많이 씀 → JPA에서 제공하는 기능이기 때문에 잘 모른다면 꼭 공부해둘 것
    }
    /**
     * 서브쿼리
     * 나이가 가장 많은 회원 조회
     */
    @Test
    public void subQuery() {
        //sub query 안에 들어가는 큐타입은 본 쿼리에서 사용하는 큐타입과 겹치면 안되기 때문에 QMember를 새로 하나 만들어줌
        //sql에서 alias(별칭)가 곂치면 안되는 거랑 같음
        QMember memberSub = new QMember("memberSub");

        List<Member> result = queryFactory
                .selectFrom(member)
                .where(member.age.eq(
                        select(memberSub.age.max())
                                .from(memberSub)
                ))
                .fetch();

        assertThat(result).extracting("age")
                .containsExactly(40);

    }

    /**
     * 나이가 평균 이상인 회원 조회
     */
    @Test
    public void subQueryGoe() {
        QMember memberSub = new QMember("memberSub");

        List<Member> result = queryFactory
                .selectFrom(member)
                .where(member.age.goe(
                        select(memberSub.age.avg())
                                .from(memberSub)
                ))
                .fetch();

        assertThat(result).extracting("age")
                .containsExactly(30, 40);

    }

    /**
     * subquery에 in절 사용해보기
     */
    @Test
    public void subQueryIn() {
        QMember memberSub = new QMember("memberSub");

        List<Member> result = queryFactory
                .selectFrom(member)
                .where(member.age.in(
                        select(memberSub.age)
                                .from(memberSub)
                                .where(memberSub.age.gt(10))
                ))
                .fetch();

        assertThat(result).extracting("age")
                .containsExactly(20, 30, 40);
    }

    @Test
    public void selectSubQuery() {
        //select에 sub query 사용하기 → JPAExpressions는 static import가 가능
        QMember memberSub = new QMember("memberSub");

        List<Tuple> result = queryFactory
                .select(member.username,
                        select(memberSub.age.avg())
                                .from(memberSub))
                .from(member)
                .fetch();

        for (Tuple tuple : result) {
            System.out.println("tuple : " + tuple);
        }

        //JPA의 서브 쿼리는 from절에서 사용할 수 없음(인라인 뷰 사용 불가) → select, where절에서는 사용 가능
        //from절에서 서브 쿼리를 사용하려면 → 서브 쿼리를 join으로 변경(그러나 이렇게 해도 불가능한 상황이 있음), 또는 쿼리를 2번 분리해서 실행, 또는 navtiveSQL 사용
        //실시간 트래픽이 중요하다면 쿼리 한 방 한 방이 중요하다면 한 방 쿼리가 중요하겠지. 그러나 어드민 같은 조금 느려도 괜찮은 서비스라면 복잡한 한 방 쿼리보다 쿼리를 2~3번 나눠서 호출하는 게 더 나을 수 있음 → 애플리케이션 로직에서 시퀀시 하게 여러 번 나눠서 호출하는 것이 몇 천 줄 짜리 한 방 쿼리보다 더 간결하게 처리할 수 있음
    }

    @Test
    public void basicCase() {
        List<String> result = queryFactory
                .select(member.age
                        .when(10).then("10살")
                        .when(20).then("20살")
                        .otherwise("기타")
                )
                .from(member)
                .fetch();

        for (String s : result) {
            System.out.println("s : " + s);
        }
    }

    @Test
    public void complexCase() {
        List<String> result = queryFactory
                .select(new CaseBuilder()
                        .when(member.age.between(0, 20)).then("0~20살")
                        .when(member.age.between(21, 30)).then("21~30살")
                        .otherwise("기타")
                )
                .from(member)
                .fetch();

        for (String s : result) {
            System.out.println(" s : " + s);
        }

        //DB는 row 데이터를 필터링하고 그룹핑 해주는 정도로 데이터를 줄이는 일을 하고, 데이터를 가공해주는 것은 db에서 해주면 안됨
        //애플리케이션, 프레젠테이션 레벨에서 해결하도록 하자
    }
    @Test
    public void constant() {
        List<Tuple> result = queryFactory
                .select(member.username, Expressions.constant("A"))
                .from(member)
                .fetch();

        for (Tuple tuple : result) {
            System.out.println("tuple : " + tuple);
        }
    }

    @Test
    public void concat() {
        //username_age 구조를 만들어보자
        List<String> result = queryFactory
                .select(member.username.concat("_").concat(member.age.stringValue()))
                .from(member)
                .where(member.username.eq("member1"))
                .fetch();

        for (String s : result) {
            System.out.println(" s : " + result);
        }
    }

    /**
     * 프로젝션과 결과 반환 -기본
     */
    @Test
    public void tupleProjection() {
        List<Tuple> result = queryFactory
                .select(member.username, member.age)
                .from(member)
                .fetch();

        for (Tuple tuple : result) {
            String username = tuple.get(member.username);
            Integer age = tuple.get(member.age);

            System.out.println("username : " + username);
            System.out.println("age : " + age);
        }

        //바깥 계층으로 던져줄 때는 tuple 말고 DTO로 던져주자
    }
    /**
     * 프로젝션과 결과 반환 -DTO 조회
     */

    @Test
    public void findDtoByJPQL() {
        //new 해서 패키지명 다 적어줌 -> 마치 생성자를 호출하듯이 사용, JPQL에서 제공하는 new operation 문법
        //이렇게 단순한 쿼리문 하나 짜는데도 좀 별로라는 게 느껴짐
        List<MemberDto> resultList = em.createQuery("select new study.querydsl.dto.MemberDto(m.username, m.age) from Member m", MemberDto.class)
                .getResultList();

        for (MemberDto memberDto : resultList) {
            System.out.println("memberDto : " + memberDto);
        }
    }

    /**
     * 프로젝션과 결과 반환 -DTO 조회 - 프로퍼티 접근 - setter
     */

    @Test
    public void findDtoBySetter() {
        //Projections.bean 안에 반환하고자 하는 타입, 프로젝션할 항목들을 쭉 써주면 됨
        List<MemberDto> result = queryFactory
                .select(Projections.bean(MemberDto.class,
                        member.username, member.age))
                .from(member)
                .fetch();

        for (MemberDto memberDto : result) {
            System.out.println("memberDto : " + memberDto);
        }
        //생성자를 활용해서 값 주입하는것처럼 보이지만 기본 생성자로 생성 후 setter로 값을 주입하는 방식
    }

    /**
     * 프로젝션과 결과 반환 -DTO 조회 - 필드 직접 접근
     */

    @Test
    public void findDtoByField() {
        List<MemberDto> result = queryFactory
                .select(Projections.fields(MemberDto.class,
                        member.username, member.age))
                .from(member)
                .fetch();

        for (MemberDto memberDto : result) {
            System.out.println("memberDto : " + memberDto);
        }
        //필드에 직접 접근하는 방식 → 필드명이 일치해야 함
    }

    @Test
    public void findUserDto() {
        //Projections.bean 안에 반환하고자 하는 타입, 프로젝션할 항목들을 쭉 써주면 됨
        List<UserDto> result = queryFactory
                .select(Projections.fields(UserDto.class,
                        member.username.as("name"), member.age))
                .from(member)
                .fetch();

        for (UserDto userDto : result) {
            System.out.println("userDto : " + userDto);
        }
    }
    /**
     * 프로젝션과 결과 반환 -DTO 조회 - 생성자 사용
     */

    @Test
    public void findDtoByConstructor() {
        List<MemberDto> result = queryFactory
                .select(Projections.constructor(MemberDto.class,
                        member.username, member.age))
                .from(member)
                .fetch();

        for (MemberDto memberDto : result) {
            System.out.println("memberDto : " + memberDto);
        }
    }

    //프로젝션과 결과 반환 - @QueryProjection

    @Test
    public void findDtoByQueryProjection() {
        List<MemberDto> result = queryFactory
                .select(new QMemberDto(member.username, member.age))
                .from(member)
                .fetch();

        for (MemberDto memberDto : result) {
            System.out.println("memberDto : " + memberDto);
        }
        //아키텍처 설계 방향에 따라 dto를 순수하게 가져가고자 한다면 @QueryProjection 을 사용하기 힘듦. 반면 편의성 및 의존성을 감안하고 쓴다면 @QueryProjection 은 꽤 매력적인 선택지가 될 수 있음
    }

    /**
     * 동적 쿼리 - BooleanBuilder 사용
     */

    @Test
    public void dynamicQuery_BooleanBuilder() {
        String usernameParam = "member1";
        Integer ageParam = 10;

        List<Member> result = searchMember1(usernameParam, ageParam);

        assertThat(result.size()).isEqualTo(1);
    }

    private List<Member> searchMember1(String usernameCond, Integer ageCond) {
        BooleanBuilder builder = new BooleanBuilder();
        if (usernameCond != null) {
            //usernameCond이 null이 아니면 builder 조건에 username이 usernameCond과 같은지 판별 조건 추가
            builder.and(member.username.eq(usernameCond));
        }

        if (ageCond != null) {
            //ageCond가 null이 아니면 builder에 조건 추가
            builder.and(member.age.eq(ageCond));
        }

        return queryFactory
                .selectFrom(member)
                .where(builder)
                .fetch();
        //만약 특정 조건이 반드시 들어가야 하는 필수 조건이라면 BooleanBuilder에 초기값을 세팅할 수 있음
        //BooleanBuilder builder = new BooleanBuilder(member.username.eq(usernameCond));
        //BooleanBuilder는 or로도 조립할 수 있음 → builder.or(member.username.eq("member1"));
    }

    /**
     * 동적 쿼리 - Where 다중 파라미터 사용
     */
    public void dynamicQuery_BooleanExpress() {
        String usernameParam = "member1";
        Integer ageParam = 10;

        List<Member> result = searchMember2(usernameParam, ageParam);

        assertThat(result.size()).isEqualTo(1);
    }
    private List<Member> searchMember2(String usernameCond, Integer ageCond) {
        return queryFactory
                .selectFrom(member)
                .where(allEq(usernameCond, ageCond))
                .fetch();
    }

    private BooleanExpression allEq(String usernameCond, Integer ageCond) {
        //이렇게 and로 묶어서 한 방에 조건을 쏠 수 있음
        return usernameEq(usernameCond).and(ageEq(ageCond));
    }

    private BooleanExpression ageEq(Integer ageCond) {
        return ageCond != null ? member.age.eq(ageCond) : null;
    }

    private BooleanExpression  usernameEq(String usernameCond) {
        return usernameCond != null ? member.username.eq(usernameCond) : null;
    }


    /**
     * 수정, 삭제 벌크 연산
     */
    @Test
    @Commit
    public void bulkUpdate() {
        //회원의 나이가 20살 이하면 회원 이름을 다 "비회원"으로 변경하기 -> member1, member2가 변경
        //처리한 결과로 리턴되는 long 타입(count)는 변경이 완료된 row의 수를 의미
        long count = queryFactory
                .update(member)
                .set(member.username, "비회원")
                .where(member.age.loe(20))
                .execute();
    }

    @Test
    public void bulkAdd() {
        // 모든 회원의 나이를 한 살씩 더하기
        long count = queryFactory
                .update(member)
                .set(member.age, member.age.add(1))
//          .set(member.age, member.age.multiply(2)) 이건 곱하기
                .execute();
    }

    @Test
    public void sqlFunction() {
        //member 라는 단어를 M으로 바꿔서 조회할 예정
        List<String> result = queryFactory
                .select(Expressions.stringTemplate(
                        "function('replace', {0}, {1}, {2})",
                        member.username, "member", "M"))
                .from(member)
                .fetch();

        for (String s : result) {
            System.out.println("s : " + s);
        }
    }

    

}

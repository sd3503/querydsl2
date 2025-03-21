package study.querydsl;

import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;

@Entity
public class Hello {
    @Id @GeneratedValue
    private Long id;
}

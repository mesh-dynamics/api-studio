package com.cubeui.backend.domain;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.OnDelete;
import org.hibernate.annotations.OnDeleteAction;

import javax.persistence.*;

@Entity
@Table(name="jira_customer_default_credentials")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class JiraCustomerDefaultCredentials {
    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    Long id;

    @Column(nullable = false)
    String userName;

    @Column(nullable = false)
    String APIKey;

    @Column(nullable = false)
    String jiraBaseURL;

    @OneToOne
    @OnDelete(action = OnDeleteAction.CASCADE)
    @JoinColumn(name = "customer_id", nullable = false)
    Customer customer;
}

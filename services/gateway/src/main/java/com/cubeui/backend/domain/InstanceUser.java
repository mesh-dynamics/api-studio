package com.cubeui.backend.domain;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.OnDelete;
import org.hibernate.annotations.OnDeleteAction;

import javax.persistence.*;

@Entity
@Table(name="instance_users",
        uniqueConstraints=@UniqueConstraint(columnNames={"user_id", "instance_id"}),
        indexes = {
                @Index(columnList = "user_id", name = "instance_user_index")
        })
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class InstanceUser {

    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    Long id;

    @ManyToOne
    @OnDelete(action = OnDeleteAction.CASCADE)
    @JoinColumn(name = "instance_id", nullable = false)
    Instance instance;

    @ManyToOne
    @OnDelete(action = OnDeleteAction.CASCADE)
    @JoinColumn(name = "user_id", nullable = false)
    User user;
}

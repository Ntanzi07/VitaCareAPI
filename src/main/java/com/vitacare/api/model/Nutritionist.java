package com.vitacare.api.model;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.GenericGenerator;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;


@Entity
@Table(name = "nutritionists")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class Nutritionist {

    @Id
    @GeneratedValue(generator = "UUID")
    @GenericGenerator(name = "UUID", strategy = "org.hibernate.id.UUIDGenerator")
    @Column(columnDefinition = "uuid")
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id")
    private User user;

    @Column(name = "name", nullable = false, length = 255)
    private String name;

    @Column(name = "description", columnDefinition = "TEXT")
    private String description;

    @Column(name = "profile_photo", columnDefinition = "TEXT")
    private String profilePhoto;

    @Column(name = "banner_image", columnDefinition = "TEXT")
    private String bannerImage;

    @Column(name = "crn", length = 50)
    private String crn;

    @Column(name = "consultation_price", precision = 10, scale = 2)
    private BigDecimal consultationPrice;

    @Column(name = "created_at", insertable = false, updatable = false)
    private OffsetDateTime createdAt;

    @OneToMany(mappedBy = "nutritionist", fetch = FetchType.LAZY, cascade = CascadeType.ALL)
    private List<Plan> plans;

    @OneToMany(mappedBy = "nutritionist", fetch = FetchType.LAZY, cascade = CascadeType.ALL)
    private List<Availability> availability;
}
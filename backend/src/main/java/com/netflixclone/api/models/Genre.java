package com.netflixclone.api.models;

import jakarta.persistence.*;
import lombok.*;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "genres")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Genre {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @Column(unique = true, nullable = false)
    private String name;

    @ManyToMany(mappedBy = "genres", fetch = FetchType.LAZY)
    @Builder.Default
    private List<Movie> movies = new ArrayList<>();
}
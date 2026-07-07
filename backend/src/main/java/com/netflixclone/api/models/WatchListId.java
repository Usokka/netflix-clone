package com.netflixclone.api.models;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import lombok.*;
import java.io.Serializable;
import java.util.UUID;

@Embeddable
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode
public class WatchListId implements Serializable {

    @Column(name = "profile_id")
    private UUID profileId;

    @Column(name = "movie_id")
    private UUID movieId;
}